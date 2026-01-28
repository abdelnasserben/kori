package com.kori.application.usecase;

import com.kori.application.command.PayByCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.PayByCardResult;
import com.kori.application.security.ActorType;
import com.kori.application.security.PinFormatValidator;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PayByCardService implements PayByCardUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final IdGeneratorPort idGeneratorPort;

    private final TerminalRepositoryPort terminalRepositoryPort;
    private final MerchantRepositoryPort merchantRepositoryPort;

    private final CardRepositoryPort cardRepositoryPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final FeePolicyPort feePolicyPort;

    private final CardSecurityPolicyPort cardSecurityPolicyPort;

    private final LedgerAppendPort ledgerAppendPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final AccountProfilePort accountProfilePort;

    private final AuditPort auditPort;
    private final PinHasherPort pinHasherPort;

    public PayByCardService(TimeProviderPort timeProviderPort,
                            IdempotencyPort idempotencyPort, IdGeneratorPort idGeneratorPort,
                            TerminalRepositoryPort terminalRepositoryPort,
                            MerchantRepositoryPort merchantRepositoryPort,
                            CardRepositoryPort cardRepositoryPort,
                            TransactionRepositoryPort transactionRepositoryPort,
                            FeePolicyPort feePolicyPort,
                            CardSecurityPolicyPort cardSecurityPolicyPort,
                            LedgerAppendPort ledgerAppendPort,
                            LedgerQueryPort ledgerQueryPort,
                            AccountProfilePort accountProfilePort,
                            AuditPort auditPort,
                            PinHasherPort pinHasherPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.idGeneratorPort = idGeneratorPort;
        this.terminalRepositoryPort = terminalRepositoryPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.feePolicyPort = feePolicyPort;
        this.cardSecurityPolicyPort = cardSecurityPolicyPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.accountProfilePort = accountProfilePort;
        this.auditPort = auditPort;
        this.pinHasherPort = pinHasherPort;
    }

    @Override
    public PayByCardResult execute(PayByCardCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), PayByCardResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        if (command.actorContext().actorType() != ActorType.TERMINAL) {
            throw new ForbiddenOperationException("Only TERMINAL can initiate PayByCard");
        }

        // Merchant accountRef must be ACTIVE
        Terminal terminal = terminalRepositoryPort.findById(TerminalId.of(command.terminalId()))
                .orElseThrow(() -> new NotFoundException("Terminal not found"));

        if (terminal.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("Terminal is not active");
        }

        // Merchant accountRef must be ACTIVE
        Merchant merchant = merchantRepositoryPort.findById(terminal.merchantId())
                .orElseThrow(() -> new NotFoundException("Merchant not found"));

        var merchantAcc = LedgerAccountRef.merchant(merchant.id().toString());
        AccountProfile merchantProfile = accountProfilePort.findByAccount(merchantAcc)
                .orElseThrow(() -> new NotFoundException("Merchant account not found"));

        if (merchantProfile.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("Merchant account is not active");
        }

        // CARD
        Card card = cardRepositoryPort.findByCardUid(command.cardUid())
                .orElseThrow(() -> new NotFoundException("Card not found"));

        if (!card.isPayable()) {
            throw new ForbiddenOperationException("Card not payable");
        }

        // Check max pin policy defined
        int maxAttempts = cardSecurityPolicyPort.maxFailedPinAttempts();
        if (maxAttempts <= 0) {
            throw new ForbiddenOperationException("Invalid maxFailedPinAttempts policy value");
        }

        // PIN check -> increment attempts and possibly block
        PinFormatValidator.validate(command.pin());

        if (!pinHasherPort.matches(command.pin(), card.hashedPin())) {
            card.onPinFailure(maxAttempts);
            cardRepositoryPort.save(card);
            throw new ForbiddenOperationException("Invalid PIN");
        }

        // Reset attempts on success
        card.onPinSuccess();
        cardRepositoryPort.save(card);

        // Client accountRef ref comes from card.cardUid
        var clientAcc = LedgerAccountRef.client(card.clientId().value().toString());

        // Client accountRef must be ACTIVE (recommended)
        AccountProfile clientProfile = accountProfilePort.findByAccount(clientAcc)
                .orElseThrow(() -> new NotFoundException("Client account not found"));
        if (clientProfile.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("Client account is not active");
        }

        Instant now = timeProviderPort.now();

        Money amount = Money.positive(command.amount());
        Money fee = feePolicyPort.cardPaymentFee(amount);
        Money totalDebited = amount.plus(fee);

        // Sufficient funds check (ledger-driven)
        Money available = ledgerQueryPort.netBalance(clientAcc);
        if (totalDebited.isGreaterThan(available)) {
            throw new InsufficientFundsException(
                    "Insufficient funds: need " + totalDebited + " but available " + available
            );
        }

        TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
        Transaction tx = Transaction.payByCard(txId, amount, now);
        tx = transactionRepositoryPort.save(tx);

        var feeAcc = LedgerAccountRef.platformFeeRevenue();

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), clientAcc, totalDebited),
                LedgerEntry.credit(tx.id(), merchantAcc, amount),
                LedgerEntry.credit(tx.id(), feeAcc, fee)
        ));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("terminalId", command.terminalId());
        metadata.put("merchantCode", merchant.id().value().toString());
        metadata.put("transactionId", tx.id().value().toString());
        metadata.put("cardUid", command.cardUid());

        auditPort.publish(new AuditEvent(
                "PAY_BY_CARD",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        PayByCardResult result = new PayByCardResult(
                tx.id().toString(),
                merchant.code().value(),
                card.cardUid(),
                amount.asBigDecimal(),
                fee.asBigDecimal(),
                totalDebited.asBigDecimal()
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}

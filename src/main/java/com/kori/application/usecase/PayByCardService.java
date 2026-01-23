package com.kori.application.usecase;

import com.kori.application.command.PayByCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.PayByCardResult;
import com.kori.application.security.ActorType;
import com.kori.application.security.PinFormatValidator;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.account.Account;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.transaction.Transaction;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PayByCardService implements PayByCardUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;

    private final TerminalRepositoryPort terminalRepositoryPort;
    private final MerchantRepositoryPort merchantRepositoryPort;

    private final CardRepositoryPort cardRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final FeePolicyPort feePolicyPort;

    private final CardSecurityPolicyPort cardSecurityPolicyPort;

    private final LedgerAppendPort ledgerAppendPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final AuditPort auditPort;

    private final PinHasherPort pinHasherPort;

    public PayByCardService(TimeProviderPort timeProviderPort,
                            IdempotencyPort idempotencyPort,
                            TerminalRepositoryPort terminalRepositoryPort,
                            MerchantRepositoryPort merchantRepositoryPort,
                            CardRepositoryPort cardRepositoryPort,
                            AccountRepositoryPort accountRepositoryPort,
                            TransactionRepositoryPort transactionRepositoryPort,
                            FeePolicyPort feePolicyPort, CardSecurityPolicyPort cardSecurityPolicyPort,
                            LedgerAppendPort ledgerAppendPort, LedgerQueryPort ledgerQueryPort,
                            AuditPort auditPort, PinHasherPort pinHasherPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.terminalRepositoryPort = terminalRepositoryPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.accountRepositoryPort = accountRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.feePolicyPort = feePolicyPort;
        this.cardSecurityPolicyPort = cardSecurityPolicyPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.ledgerQueryPort = ledgerQueryPort;
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

        Terminal terminal = terminalRepositoryPort.findById(command.terminalId())
                .orElseThrow(() -> new ForbiddenOperationException("Terminal not found"));

        Merchant merchant = merchantRepositoryPort.findById(terminal.merchantId().value())
                .orElseThrow(() -> new ForbiddenOperationException("Merchant not found"));

        Card card = cardRepositoryPort.findByCardUid(command.cardUid())
                .orElseThrow(() -> new ForbiddenOperationException("Card not found"));

        // Carte doit être ACTIVE pour payer
        if (!card.isPayable()) {
            throw new ForbiddenOperationException("Card not payable");
        }

        int maxAttempts = cardSecurityPolicyPort.maxFailedPinAttempts();
        if (maxAttempts <= 0) {
            throw new IllegalStateException("Invalid maxFailedPinAttempts policy value");
        }

        // PIN check -> increment attempts and possibly block
        PinFormatValidator.validate(command.pin());

        if (!pinHasherPort.matches(command.pin(), card.hashedPin())) {
            Card updated = card.onPinFailure(maxAttempts);
            cardRepositoryPort.save(updated);
            throw new ForbiddenOperationException("Invalid PIN");
        }

        // Reset attempts on success (nice-to-have)
        if (card.hasFailedPinAttempts()) {
            cardRepositoryPort.save(card.onPinSuccess());
        }

        Account account = accountRepositoryPort.findById(card.accountId())
                .orElseThrow(() -> new ForbiddenOperationException("Account not found"));

        Instant now = timeProviderPort.now();

        Money amount = Money.positive(command.amount());
        Money fee = feePolicyPort.cardPaymentFee(amount);
        Money totalDebited = amount.plus(fee);

        // --- Sufficient funds check (ledger-driven)
        // If you want to support overdraft/credit line later, model it explicitly.
        Money available = ledgerQueryPort.netBalance(LedgerAccount.CLIENT, account.clientId().value());
        if (totalDebited.isGreaterThan(available)) {
            throw new InsufficientFundsException(
                    "Insufficient funds: need " + totalDebited + " but available " + available
            );
        }

        Transaction tx = Transaction.payByCard(amount, now);
        tx = transactionRepositoryPort.save(tx);

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), LedgerAccount.CLIENT, totalDebited, account.clientId().value()),
                LedgerEntry.credit(tx.id(), LedgerAccount.MERCHANT, amount, merchant.id().value()),
                LedgerEntry.credit(tx.id(), LedgerAccount.PLATFORM, fee, null)
        ));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("terminalId", command.terminalId());
        metadata.put("merchantId", merchant.id().value());
        metadata.put("transactionId", tx.id().value());

        auditPort.publish(new AuditEvent(
                "PAY_BY_CARD",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        PayByCardResult result = new PayByCardResult(
                tx.id().value(),
                merchant.id().value(),
                account.clientId().value(),
                amount.asBigDecimal(),
                fee.asBigDecimal(),
                totalDebited.asBigDecimal()
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}

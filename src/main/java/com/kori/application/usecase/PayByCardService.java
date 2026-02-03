package com.kori.application.usecase;

import com.kori.application.command.PayByCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.exception.ValidationException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.PayByCardResult;
import com.kori.application.security.PinFormatValidator;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.UuidParser;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayByCardService implements PayByCardUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final IdGeneratorPort idGeneratorPort;

    private final TerminalRepositoryPort terminalRepositoryPort;
    private final MerchantRepositoryPort merchantRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;

    private final CardRepositoryPort cardRepositoryPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final FeePolicyPort feePolicyPort;

    private final CardSecurityPolicyPort cardSecurityPolicyPort;

    private final LedgerAppendPort ledgerAppendPort;
    private final LedgerQueryPort ledgerQueryPort;

    private final AuditPort auditPort;
    private final PinHasherPort pinHasherPort;
    private final OperationStatusGuards operationStatusGuards;

    public PayByCardService(TimeProviderPort timeProviderPort,
                            IdempotencyPort idempotencyPort,
                            IdGeneratorPort idGeneratorPort,
                            TerminalRepositoryPort terminalRepositoryPort,
                            MerchantRepositoryPort merchantRepositoryPort,
                            ClientRepositoryPort clientRepositoryPort,
                            CardRepositoryPort cardRepositoryPort,
                            TransactionRepositoryPort transactionRepositoryPort,
                            FeePolicyPort feePolicyPort,
                            CardSecurityPolicyPort cardSecurityPolicyPort,
                            LedgerAppendPort ledgerAppendPort,
                            LedgerQueryPort ledgerQueryPort,
                            AuditPort auditPort,
                            PinHasherPort pinHasherPort,
                            OperationStatusGuards operationStatusGuards) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.idGeneratorPort = idGeneratorPort;
        this.terminalRepositoryPort = terminalRepositoryPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.feePolicyPort = feePolicyPort;
        this.cardSecurityPolicyPort = cardSecurityPolicyPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.auditPort = auditPort;
        this.pinHasherPort = pinHasherPort;
        this.operationStatusGuards = operationStatusGuards;
    }

    @Override
    @Transactional
    public PayByCardResult execute(PayByCardCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), command.idempotencyRequestHash(), PayByCardResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        ActorGuards.requireTerminal(command.actorContext(), "initiate PayByCard");

        TerminalId terminalId = new TerminalId(UuidParser.parse(command.terminalUid(), "terminalId"));
        Terminal terminal = terminalRepositoryPort.findById(terminalId)
                .orElseThrow(() -> new NotFoundException("Terminal not found"));

        if (terminal.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("Terminal is not active");
        }

        Merchant merchant = merchantRepositoryPort.findById(terminal.merchantId())
                .orElseThrow(() -> new NotFoundException("Merchant not found"));

        // Merchant status + merchant account profile status
        operationStatusGuards.requireActiveMerchant(merchant);

        var merchantAcc = LedgerAccountRef.merchant(merchant.id().value().toString());

        Card card = cardRepositoryPort.findByCardUid(command.cardUid())
                .orElseThrow(() -> new NotFoundException("Card not found"));

        Client client = clientRepositoryPort.findById(card.clientId())
                .orElseThrow(() -> new NotFoundException("Client not found"));

        // Client status + client account profile status
        operationStatusGuards.requireActiveClient(client);

        if (!card.isPayable()) {
            throw new ForbiddenOperationException("Card not payable");
        }

        int maxAttempts = cardSecurityPolicyPort.maxFailedPinAttempts();
        if (maxAttempts <= 0) {
            throw new ValidationException(
                    "Invalid maxFailedPinAttempts policy value",
                    Map.of("maxFailedPinAttempts", maxAttempts)
            );
        }

        PinFormatValidator.validate(command.pin());

        if (!pinHasherPort.matches(command.pin(), card.hashedPin())) {
            card.onPinFailure(maxAttempts);
            cardRepositoryPort.save(card);
            throw new ForbiddenOperationException("Invalid PIN");
        }

        card.onPinSuccess();
        cardRepositoryPort.save(card);

        // accountRef client
        var clientAcc = LedgerAccountRef.client(card.clientId().value().toString());

        Instant now = timeProviderPort.now();

        Money amount = Money.positive(command.amount());
        Money fee = feePolicyPort.cardPaymentFee(amount);
        Money totalDebited = amount.plus(fee);

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
        metadata.put("terminalUid", command.terminalUid());
        metadata.put("merchantCode", merchant.code().value());
        metadata.put("transactionId", tx.id().value().toString());
        metadata.put("cardUid", command.cardUid());

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "PAY_BY_CARD",
                command.actorContext(),
                now,
                metadata
        ));

        PayByCardResult result = new PayByCardResult(
                tx.id().value().toString(),
                merchant.code().value(),
                card.cardUid(),
                amount.asBigDecimal(),
                fee.asBigDecimal(),
                totalDebited.asBigDecimal()
        );

        idempotencyPort.save(command.idempotencyKey(), command.idempotencyRequestHash(), result);
        return result;
    }
}

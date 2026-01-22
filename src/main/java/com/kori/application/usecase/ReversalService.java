package com.kori.application.usecase;

import com.kori.application.command.ReversalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.ReversalUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.ReversalResult;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReversalService implements ReversalUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    public ReversalService(TimeProviderPort timeProviderPort,
                           IdempotencyPort idempotencyPort,
                           TransactionRepositoryPort transactionRepositoryPort,
                           LedgerQueryPort ledgerQueryPort,
                           LedgerAppendPort ledgerAppendPort,
                           AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
    }

    @Override
    public ReversalResult execute(ReversalCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), ReversalResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Admin only
        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can initiate reversal");
        }

        TransactionId originalTxId = TransactionId.of(command.originalTransactionId());
        Transaction originalTx = transactionRepositoryPort.findById(originalTxId)
                .orElseThrow(() -> new ForbiddenOperationException("Original transaction not found"));

        if (transactionRepositoryPort.existsReversalFor(command.originalTransactionId())) {
            throw new ForbiddenOperationException("Transaction already reversed");
        }

        List<LedgerEntry> originalEntries = ledgerQueryPort.findByTransactionId(originalTxId.value());
        if (originalEntries.isEmpty()) {
            throw new ForbiddenOperationException("Original transaction has no ledger entries");
        }

        Instant now = timeProviderPort.now();

        // Create reversal transaction linked to original
        Transaction reversalTx = Transaction.reversal(originalTxId, originalTx.amount(), now);
        reversalTx = transactionRepositoryPort.save(reversalTx);

        // Append inverse ledger entries (append-only compensation)
        final TransactionId reversalTxId = reversalTx.id();

        List<LedgerEntry> reversalEntries = originalEntries.stream()
                .map(e -> {
                    LedgerEntryType inverted =
                            (e.type() == LedgerEntryType.CREDIT)
                                    ? LedgerEntryType.DEBIT
                                    : LedgerEntryType.CREDIT;

                    return new LedgerEntry(
                            UUID.randomUUID().toString(),
                            reversalTxId,
                            e.account(),
                            inverted,
                            e.amount(),
                            e.referenceId()
                    );
                })
                .toList();

        ledgerAppendPort.append(reversalEntries);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", reversalTx.id().value());
        metadata.put("originalTransactionId", originalTxId.value());

        auditPort.publish(new AuditEvent(
                "REVERSAL",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        ReversalResult result = new ReversalResult(reversalTx.id().value(), originalTxId.value());
        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}

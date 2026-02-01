package com.kori.application.usecase;

import com.kori.application.command.ReversalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.ReversalUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.ReversalResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReversalService implements ReversalUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;
    private final IdGeneratorPort idGeneratorPort;

    public ReversalService(TimeProviderPort timeProviderPort,
                           IdempotencyPort idempotencyPort,
                           TransactionRepositoryPort transactionRepositoryPort,
                           LedgerQueryPort ledgerQueryPort,
                           LedgerAppendPort ledgerAppendPort,
                           AuditPort auditPort, IdGeneratorPort idGeneratorPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
        this.idGeneratorPort = idGeneratorPort;
    }

    @Override
    public ReversalResult execute(ReversalCommand cmd) {
        var cached = idempotencyPort.find(cmd.idempotencyKey(), ReversalResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Admin only
        ActorGuards.requireAdmin(cmd.actorContext(), "initiate reversal");

        TransactionId originalTxId = TransactionId.of(cmd.originalTransactionId());
        Transaction originalTx = transactionRepositoryPort.findById(originalTxId)
                .orElseThrow(() -> new NotFoundException("Original transaction not found"));

        if (transactionRepositoryPort.existsReversalFor(originalTxId)) {
            throw new ForbiddenOperationException("Transaction already reversed");
        }

        List<LedgerEntry> originalEntries = ledgerQueryPort.findByTransactionId(originalTxId);
        if (originalEntries.isEmpty()) {
            throw new ForbiddenOperationException("Original transaction has no ledger entries");
        }

        originalEntries.forEach(l -> {
            if(l.accountRef() == null)
                throw new ForbiddenOperationException("account reference is required");
        });

        Instant now = timeProviderPort.now();

        // Create reversal transaction linked to original
        TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
        Transaction reversalTx = Transaction.reversal(txId, originalTxId, originalTx.amount(), now);
        transactionRepositoryPort.save(reversalTx);

        // Append inverse ledger entries (append-only compensation)
        final TransactionId reversalTxId = reversalTx.id();

        List<LedgerEntry> reversalEntries = originalEntries.stream()
                .map(e -> e.type() == LedgerEntryType.CREDIT
                        ? LedgerEntry.debit(reversalTxId, e.accountRef(), e.amount())
                        : LedgerEntry.credit(reversalTxId, e.accountRef(), e.amount())
                )
                .toList();

        ledgerAppendPort.append(reversalEntries);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", reversalTx.id().value().toString());
        metadata.put("originalTransactionId", originalTxId.value().toString());

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "REVERSAL",
                cmd.actorContext(),
                now,
                metadata
        ));

        ReversalResult result = new ReversalResult(reversalTx.id().value().toString(), originalTxId.value().toString());
        idempotencyPort.save(cmd.idempotencyKey(), result);
        return result;
    }
}

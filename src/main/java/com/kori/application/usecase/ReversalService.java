package com.kori.application.usecase;

import com.kori.application.command.ReversalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.ReversalUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.ReversalResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.config.FeeConfig;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ReversalService implements ReversalUseCase {
    private static final LedgerAccountRef FEE_ACCOUNT = LedgerAccountRef.platformFeeRevenue();
    private static final LedgerAccountRef CLEARING_ACCOUNT = LedgerAccountRef.platformClearing();

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;
    private final IdGeneratorPort idGeneratorPort;
    private final FeeConfigPort feeConfigPort;

    public ReversalService(TimeProviderPort timeProviderPort,
                           IdempotencyPort idempotencyPort,
                           TransactionRepositoryPort transactionRepositoryPort,
                           LedgerQueryPort ledgerQueryPort,
                           LedgerAppendPort ledgerAppendPort,
                           AuditPort auditPort, IdGeneratorPort idGeneratorPort, FeeConfigPort feeConfigPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
        this.idGeneratorPort = idGeneratorPort;
        this.feeConfigPort = feeConfigPort;
    }

    @Override
    public ReversalResult execute(ReversalCommand cmd) {
        var cached = idempotencyPort.find(cmd.idempotencyKey(), cmd.idempotencyRequestHash(), ReversalResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

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

        TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
        Transaction reversalTx = Transaction.reversal(txId, originalTxId, originalTx.amount(), now);
        transactionRepositoryPort.save(reversalTx);

        List<LedgerEntry> reversalEntries = buildReversalEntries(originalTx, originalEntries, reversalTx.id());

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
        idempotencyPort.save(cmd.idempotencyKey(), cmd.idempotencyRequestHash(), result);
        return result;
    }

    private List<LedgerEntry> buildReversalEntries(Transaction originalTx, List<LedgerEntry> originalEntries, TransactionId reversalTxId) {
        return switch (originalTx.type()) {
            case PAY_BY_CARD -> buildPayByCardReversalEntries(originalEntries, reversalTxId);
            case MERCHANT_WITHDRAW_AT_AGENT -> buildMerchantWithdrawReversalEntries(originalEntries, reversalTxId);
            default -> originalEntries.stream()
                    .map(e -> e.type() == LedgerEntryType.CREDIT
                            ? LedgerEntry.debit(reversalTxId, e.accountRef(), e.amount())
                            : LedgerEntry.credit(reversalTxId, e.accountRef(), e.amount())
                    )
                    .toList();
        };
    }

    private List<LedgerEntry> buildPayByCardReversalEntries(List<LedgerEntry> originalEntries, TransactionId reversalTxId) {
        LedgerEntry merchantCredit = findRequiredEntry(
                originalEntries,
                LedgerEntryType.CREDIT,
                e -> e.accountRef().isForMerchant(),
                "PAY_BY_CARD merchant credit not found"
        );
        LedgerEntry clientDebit = findRequiredEntry(
                originalEntries,
                LedgerEntryType.DEBIT,
                e -> e.accountRef().isForClient(),
                "PAY_BY_CARD client debit not found"
        );
        LedgerEntry feeCredit = findOptionalEntry(
                originalEntries,
                LedgerEntryType.CREDIT,
                e -> e.accountRef().equals(FEE_ACCOUNT)
        );

        boolean refundable = feeConfigPort.get().map(FeeConfig::cardPaymentFeeRefundable).orElse(false);

        if (!refundable || feeCredit == null) {
            return List.of(
                    LedgerEntry.debit(reversalTxId, merchantCredit.accountRef(), merchantCredit.amount()),
                    LedgerEntry.credit(reversalTxId, clientDebit.accountRef(), merchantCredit.amount())
            );
        }

        return List.of(
                LedgerEntry.debit(reversalTxId, merchantCredit.accountRef(), merchantCredit.amount()),
                LedgerEntry.debit(reversalTxId, feeCredit.accountRef(), feeCredit.amount()),
                LedgerEntry.credit(reversalTxId, clientDebit.accountRef(), merchantCredit.amount().plus(feeCredit.amount()))
        );
    }

    private List<LedgerEntry> buildMerchantWithdrawReversalEntries(List<LedgerEntry> originalEntries, TransactionId reversalTxId) {
        LedgerEntry principalPayerDebit = findRequiredEntry(
                originalEntries,
                LedgerEntryType.DEBIT,
                e -> e.accountRef().isForMerchant(),
                "MERCHANT_WITHDRAW merchant debit not found"
        );
        LedgerEntry principalRecipientCredit = findRequiredEntry(
                originalEntries,
                LedgerEntryType.CREDIT,
                e -> e.accountRef().equals(CLEARING_ACCOUNT),
                "MERCHANT_WITHDRAW clearing credit not found"
        );
        LedgerEntry feeCredit = findOptionalEntry(
                originalEntries,
                LedgerEntryType.CREDIT,
                e -> e.accountRef().equals(FEE_ACCOUNT)
        );

        boolean refundable = feeConfigPort.get().map(FeeConfig::merchantWithdrawFeeRefundable).orElse(false);

        if (!refundable || feeCredit == null) {
            return List.of(
                    LedgerEntry.debit(reversalTxId, principalRecipientCredit.accountRef(), principalRecipientCredit.amount()),
                    LedgerEntry.credit(reversalTxId, principalPayerDebit.accountRef(), principalRecipientCredit.amount())
            );
        }

        return List.of(
                LedgerEntry.debit(reversalTxId, principalRecipientCredit.accountRef(), principalRecipientCredit.amount()),
                LedgerEntry.debit(reversalTxId, feeCredit.accountRef(), feeCredit.amount()),
                LedgerEntry.credit(reversalTxId, principalPayerDebit.accountRef(), principalRecipientCredit.amount().plus(feeCredit.amount()))
        );
    }

    private LedgerEntry findRequiredEntry(List<LedgerEntry> entries,
                                          LedgerEntryType type,
                                          Predicate<LedgerEntry> predicate,
                                          String errorMessage) {
        LedgerEntry entry = findOptionalEntry(entries, type, predicate);
        if (entry == null) {
            throw new ForbiddenOperationException(errorMessage);
        }
        return entry;
    }

    private LedgerEntry findOptionalEntry(List<LedgerEntry> entries,
                                          LedgerEntryType type,
                                          Predicate<LedgerEntry> predicate) {
        return entries.stream()
                .filter(e -> e.type() == type)
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }
}

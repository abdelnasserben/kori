package com.kori.application.usecase;

import com.kori.application.command.SearchTransactionHistoryCommand;
import com.kori.application.command.TransactionHistoryView;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.SearchTransactionHistoryUseCase;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.port.out.TransactionRepositoryPort;
import com.kori.application.result.TransactionHistoryItem;
import com.kori.application.result.TransactionHistoryResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.security.LedgerAccessPolicy;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class SearchTransactionHistoryService implements SearchTransactionHistoryUseCase {

    private final LedgerQueryPort ledgerQueryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final LedgerAccessPolicy ledgerAccessPolicy;

    public SearchTransactionHistoryService(LedgerQueryPort ledgerQueryPort,
                                           TransactionRepositoryPort transactionRepositoryPort,
                                           LedgerAccessPolicy ledgerAccessPolicy) {
        this.ledgerQueryPort = Objects.requireNonNull(ledgerQueryPort);
        this.transactionRepositoryPort = Objects.requireNonNull(transactionRepositoryPort);
        this.ledgerAccessPolicy = Objects.requireNonNull(ledgerAccessPolicy);
    }

    @Override
    public TransactionHistoryResult execute(SearchTransactionHistoryCommand command) {
        Objects.requireNonNull(command);

        var scope = resolveScope(command.actorContext(), command.ledgerAccount(), command.referenceId());
        ledgerAccessPolicy.assertCanReadLedger(command.actorContext(), scope.ledgerAccount, scope.referenceId);

        // Entries of the actor's ledger (scope) => defines which tx are "visible" for that actor
        var scopeEntries = ledgerQueryPort.findEntries(scope.ledgerAccount, scope.referenceId);

        // Group scope entries by tx (preserve order)
        var byTx = new LinkedHashMap<String, List<LedgerEntry>>();
        for (var e : scopeEntries) {
            byTx.computeIfAbsent(e.transactionId().value(), __ -> new ArrayList<>()).add(e);
        }

        List<TransactionHistoryItem> items = new ArrayList<>();

        for (var kv : byTx.entrySet()) {
            String txId = kv.getKey();

            var txOpt = transactionRepositoryPort.findById(TransactionId.of(txId));
            if (txOpt.isEmpty()) continue;
            var tx = txOpt.get();

            // Filters on the transaction itself
            if (command.transactionType() != null && tx.type() != command.transactionType()) continue;
            if (!withinRange(tx.createdAt(), command.from(), command.to())) continue;

            // Stable cursor filtering
            if (command.beforeCreatedAt() != null) {
                Instant bc = command.beforeCreatedAt();
                String bt = command.beforeTransactionId();

                boolean strictlyBefore =
                        tx.createdAt().isBefore(bc)
                                || (tx.createdAt().equals(bc) && tx.id().value().compareTo(bt) < 0);

                if (!strictlyBefore) continue;
            }

            // Fetch all ledger entries of this transaction to build counterparties + views
            var allEntries = ledgerQueryPort.findByTransactionId(txId);

            // Counterparties
            String clientId = firstRef(allEntries, LedgerAccount.CLIENT);
            String merchantId = firstRef(allEntries, LedgerAccount.MERCHANT);
            String agentId = firstRef(allEntries, LedgerAccount.AGENT);

            // Self aggregates (only from the entries of the scope ledger)
            Money selfDebits = Money.zero();
            Money selfCredits = Money.zero();
            for (var e : kv.getValue()) {
                if (e.account() != scope.ledgerAccount) continue;
                if (e.type() == LedgerEntryType.DEBIT) selfDebits = selfDebits.plus(e.amount());
                else selfCredits = selfCredits.plus(e.amount());
            }
            Money selfNet = selfCredits.minus(selfDebits);

            // Projection fields for PAY_BY_CARD_VIEW / COMMISSION_VIEW
            Money clientDebit = sum(allEntries, LedgerAccount.CLIENT, LedgerEntryType.DEBIT);
            Money merchantCredit = sum(allEntries, LedgerAccount.MERCHANT, LedgerEntryType.CREDIT);
            Money platformCredit = sum(allEntries, LedgerAccount.PLATFORM, LedgerEntryType.CREDIT);
            Money agentCredit = sum(allEntries, LedgerAccount.AGENT, LedgerEntryType.CREDIT);

            TransactionHistoryView view = command.view();

            // Optional: if someone asks PAY_BY_CARD_VIEW but tx isn't PAY_BY_CARD, we skip (clean UI)
            if (view == TransactionHistoryView.PAY_BY_CARD_VIEW && tx.type() != TransactionType.PAY_BY_CARD) {
                continue;
            }
            // Optional: COMMISSION_VIEW shows only tx where agent actually has credit
            if (view == TransactionHistoryView.COMMISSION_VIEW && agentCredit.asBigDecimal().compareTo(Money.zero().asBigDecimal()) == 0) {
                continue;
            }

            BigDecimal amount = null;
            BigDecimal fee = null;
            BigDecimal totalDebited = null;

            if (view == TransactionHistoryView.PAY_BY_CARD_VIEW) {
                amount = merchantCredit.asBigDecimal();
                fee = platformCredit.asBigDecimal();
                totalDebited = clientDebit.asBigDecimal();
            } else if (view == TransactionHistoryView.COMMISSION_VIEW) {
                // for agent, amount = credited commission
                amount = agentCredit.asBigDecimal();
                fee = BigDecimal.ZERO.setScale(2);
                totalDebited = BigDecimal.ZERO.setScale(2);
            }

            var built = new TransactionHistoryItem(
                    tx.id().value(),
                    tx.type(),
                    tx.createdAt(),
                    clientId,
                    merchantId,
                    agentId,
                    selfDebits.asBigDecimal(),
                    selfCredits.asBigDecimal(),
                    selfNet.asBigDecimal(),
                    amount,
                    fee,
                    totalDebited
            );

            if (passesAmountFilter(command.view(), command.minAmount(), command.maxAmount(), built)) {
                items.add(built);
            }

        }

        // Sort newest first
        items.sort(Comparator
                .comparing(TransactionHistoryItem::createdAt).reversed()
                .thenComparing(TransactionHistoryItem::transactionId, Comparator.reverseOrder())
        );

        // Limit
        int limit = command.limit() <= 0 ? 50 : command.limit();
        if (items.size() > limit) {
            items = items.subList(0, limit);
        }

        // Compute next cursor (stable)
        Instant nextBeforeCreatedAt = null;
        String nextBeforeTransactionId = null;

        if (!items.isEmpty()) {
            var last = items.get(items.size() - 1);
            nextBeforeCreatedAt = last.createdAt();
            nextBeforeTransactionId = last.transactionId();
        }

        return new TransactionHistoryResult(
                scope.ledgerAccount,
                scope.referenceId,
                items,
                nextBeforeCreatedAt,
                nextBeforeTransactionId
        );
    }

    private boolean withinRange(Instant value, Instant from, Instant to) {
        if (from != null && value.isBefore(from)) return false;
        if (to != null && value.isAfter(to)) return false;
        return true;
    }

    private Money sum(List<com.kori.domain.ledger.LedgerEntry> entries, LedgerAccount account, LedgerEntryType type) {
        Money total = Money.zero();
        for (var e : entries) {
            if (e.account() == account && e.type() == type) {
                total = total.plus(e.amount());
            }
        }
        return total;
    }

    private String firstRef(List<com.kori.domain.ledger.LedgerEntry> entries, LedgerAccount account) {
        for (var e : entries) {
            if (e.account() == account && e.referenceId() != null) return e.referenceId();
        }
        return null;
    }

    private Scope resolveScope(ActorContext actorContext, LedgerAccount requestedAccount, String requestedRef) {
        if (requestedAccount != null || requestedRef != null) {
            if (actorContext.actorType() != ActorType.ADMIN) {
                throw new ForbiddenOperationException("Only ADMIN can specify an arbitrary ledger scope");
            }
            if (requestedAccount == null || requestedRef == null) {
                throw new IllegalArgumentException("ledgerAccount and referenceId must both be provided");
            }
            return new Scope(requestedAccount, requestedRef);
        }

        return switch (actorContext.actorType()) {
            case CLIENT -> new Scope(LedgerAccount.CLIENT, actorContext.actorId());
            case MERCHANT -> new Scope(LedgerAccount.MERCHANT, actorContext.actorId());
            case AGENT -> new Scope(LedgerAccount.AGENT, actorContext.actorId());
            default -> throw new ForbiddenOperationException("Actor type cannot consult history");
        };
    }

    private record Scope(LedgerAccount ledgerAccount, String referenceId) {
        private Scope {
            Objects.requireNonNull(ledgerAccount);
            Objects.requireNonNull(referenceId);
        }
    }

    private boolean passesAmountFilter(TransactionHistoryView view,
                                       BigDecimal min,
                                       BigDecimal max,
                                       TransactionHistoryItem item) {
        if (min == null && max == null) return true;

        BigDecimal value = amountValueForView(view, item);
        if (value == null) return false;

        if (min != null && value.compareTo(min) < 0) return false;

        return max == null || value.compareTo(max) <= 0;
    }

    private BigDecimal amountValueForView(TransactionHistoryView view, TransactionHistoryItem item) {
        return switch (view) {
            case SUMMARY -> item.selfNet() == null ? null : item.selfNet().abs();
            case PAY_BY_CARD_VIEW, COMMISSION_VIEW -> item.amount();
        };
    }

}

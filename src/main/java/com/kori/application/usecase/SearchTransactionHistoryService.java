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
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
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

        LedgerAccountRef scope = resolveScope(command.actorContext(), command.ledgerAccountRef());
        ledgerAccessPolicy.assertCanReadLedger(command.actorContext(), scope);

        // Entries of the actor's ledger (scope) => defines which tx are "visible" for that actor
        var scopeEntries = ledgerQueryPort.findEntries(scope);

        // Group scope entries by tx (preserve order)
        var byTx = new LinkedHashMap<String, List<LedgerEntry>>();
        for (var e : scopeEntries) {
            byTx.computeIfAbsent(e.transactionId().value().toString(), __ -> new ArrayList<>()).add(e);
        }

        List<TransactionHistoryItem> items = new ArrayList<>();

        for (var kv : byTx.entrySet()) {
            String txId = kv.getKey();

            var txOpt = transactionRepositoryPort.findById(TransactionId.of(txId));
            if (txOpt.isEmpty()) continue;
            var tx = txOpt.get();

            // Filters on the transaction itself
            if (command.transactionType() != null && !tx.type().name().equals(command.transactionType())) continue;
            if (!withinRange(tx.createdAt(), command.from(), command.to())) continue;

            // Stable cursor filtering
            if (command.beforeCreatedAt() != null) {
                Instant bc = command.beforeCreatedAt();
                String bt = command.beforeTransactionId();

                boolean strictlyBefore =
                        tx.createdAt().isBefore(bc)
                                || (tx.createdAt().equals(bc) && tx.id().value().compareTo(UUID.fromString(bt)) < 0);

                if (!strictlyBefore) continue;
            }

            // Fetch all ledger entries of this transaction to build counterparties + views
            var allEntries = ledgerQueryPort.findByTransactionId(TransactionId.of(txId));

            // Counterparties
            String clientId = firstOwnerRef(allEntries, LedgerAccountType.CLIENT);
            String merchantId = firstOwnerRef(allEntries, LedgerAccountType.MERCHANT);
            String agentId = firstOwnerRef(allEntries, LedgerAccountType.AGENT);

            // Self aggregates (only from the entries of the scope ledger)
            Money selfDebits = Money.zero();
            Money selfCredits = Money.zero();
            for (var e : kv.getValue()) {
                if (!e.accountRef().equals(scope)) continue;
                if (e.type() == LedgerEntryType.DEBIT) selfDebits = selfDebits.plus(e.amount());
                else selfCredits = selfCredits.plus(e.amount());
            }
            Money selfNet = selfCredits.minus(selfDebits);

            // Projection fields
            Money clientDebit = sum(allEntries, LedgerAccountType.CLIENT, LedgerEntryType.DEBIT);
            Money merchantCredit = sum(allEntries, LedgerAccountType.MERCHANT, LedgerEntryType.CREDIT);
            Money platformCredit = sum(allEntries, LedgerAccountType.PLATFORM_FEE_REVENUE, LedgerEntryType.CREDIT);
            Money agentCredit = sum(allEntries, LedgerAccountType.AGENT, LedgerEntryType.CREDIT);

            TransactionHistoryView view = command.view();

            if (view == TransactionHistoryView.PAY_BY_CARD_VIEW && tx.type() != TransactionType.PAY_BY_CARD) {
                continue;
            }
            if (view == TransactionHistoryView.COMMISSION_VIEW && agentCredit.isZero()) {
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
                amount = agentCredit.asBigDecimal();
                fee = BigDecimal.ZERO.setScale(2);
                totalDebited = BigDecimal.ZERO.setScale(2);
            }

            var built = new TransactionHistoryItem(
                    tx.id().value().toString(),
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

        // Next cursor
        Instant nextBeforeCreatedAt = null;
        String nextBeforeTransactionId = null;
        if (!items.isEmpty()) {
            var last = items.get(items.size() - 1);
            nextBeforeCreatedAt = last.createdAt();
            nextBeforeTransactionId = last.transactionId();
        }

        return new TransactionHistoryResult(scope, items, nextBeforeCreatedAt, nextBeforeTransactionId);
    }

    private boolean withinRange(Instant value, Instant from, Instant to) {
        if (from != null && value.isBefore(from)) return false;
        if (to != null && value.isAfter(to)) return false;
        return true;
    }

    private Money sum(List<LedgerEntry> entries, LedgerAccountType type, LedgerEntryType entryType) {
        Money total = Money.zero();
        for (var e : entries) {
            if (e.accountRef().type() == type && e.type() == entryType) {
                total = total.plus(e.amount());
            }
        }
        return total;
    }

    private String firstOwnerRef(List<LedgerEntry> entries, LedgerAccountType type) {
        for (var e : entries) {
            if (e.accountRef().type() == type) {
                return e.accountRef().ownerRef();
            }
        }
        return null;
    }

    private LedgerAccountRef resolveScope(ActorContext actorContext, LedgerAccountRef requestedScope) {
        if (requestedScope != null) {
            if (actorContext.actorType() != ActorType.ADMIN) {
                throw new ForbiddenOperationException("Only ADMIN can specify an arbitrary ledger scope");
            }
            return requestedScope;
        }

        return switch (actorContext.actorType()) {
            case CLIENT -> LedgerAccountRef.client(actorContext.actorId());
            case MERCHANT -> LedgerAccountRef.merchant(actorContext.actorId());
            case AGENT -> LedgerAccountRef.agent(actorContext.actorId());
            default -> throw new ForbiddenOperationException("Actor type cannot consult history");
        };
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

package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.adapters.out.jpa.repo.LedgerEntryJpaRepository;
import com.kori.application.port.out.LedgerAppendPort;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import com.kori.domain.model.transaction.TransactionType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class JpaLedgerAdapter implements LedgerAppendPort, LedgerQueryPort {

    private final LedgerEntryJpaRepository repo;

    public JpaLedgerAdapter(LedgerEntryJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo, "repo");
    }

    /**
     * Append-only: insère des écritures immuables.
     * L'entité est updatable=false sur tous les champs métier (voir LedgerEntryEntity).
     */
    @Override
    @Transactional
    public void append(List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }

        List<LedgerEntryEntity> entities = entries.stream()
                .map(this::toEntity)
                .toList();

        repo.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public Money netBalance(LedgerAccountRef account) {
        BigDecimal balance = repo.netBalance(account.type().name(), account.ownerRef());
        return Money.of(balance);
    }

    @Override
    @Transactional(readOnly = true)
    public Money getBalance(LedgerAccountRef account) {
        return netBalance(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> findByTransactionId(TransactionId transactionId) {

        return repo.findByTransactionIdOrderByCreatedAtAscIdAsc(transactionId.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> findEntries(LedgerAccountRef account) {
        return repo.findByAccountTypeAndOwnerRefOrderByCreatedAtAscIdAsc(
                        account.type().name(),
                        account.ownerRef()
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionId> findInconsistentTransactionIds() {
        return repo.findInconsistentTransactionIds()
                .stream()
                .map(TransactionId::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Money sumDebitsByTransactionTypeAndPeriod(
            LedgerAccountRef account,
            TransactionType transactionType,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        BigDecimal debits = repo.sumDebitsByTypeAndPeriod(
                account.type().name(),
                account.ownerRef(),
                transactionType.name(),
                fromInclusive.atOffset(ZoneOffset.UTC),
                toExclusive.atOffset(ZoneOffset.UTC)
        );
        return Money.of(debits);
    }


    // Mapping
    private LedgerEntryEntity toEntity(LedgerEntry e) {
        return new LedgerEntryEntity(
                UUID.fromString(e.id()),
                e.transactionId().value(),
                e.accountRef().type().name(),
                e.accountRef().ownerRef(),
                e.type().name(),
                e.amount().asBigDecimal()
        );
    }

    private LedgerEntry toDomain(LedgerEntryEntity e) {
        return new LedgerEntry(
                e.getId().toString(),
                new TransactionId(e.getTransactionId()),
                new LedgerAccountRef(
                        LedgerAccountType.valueOf(e.getAccountType()),
                        e.getOwnerRef()
                ),
                LedgerEntryType.valueOf(e.getEntryType()),
                Money.of(e.getAmount())
        );
    }
}

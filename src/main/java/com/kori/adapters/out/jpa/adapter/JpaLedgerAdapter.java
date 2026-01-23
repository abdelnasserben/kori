package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.adapters.out.jpa.repo.LedgerEntryJpaRepository;
import com.kori.application.port.out.LedgerAppendPort;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class JpaLedgerAdapter implements LedgerAppendPort, LedgerQueryPort {

    @PersistenceContext
    private EntityManager em;

    private final LedgerEntryJpaRepository repo;

    public JpaLedgerAdapter(LedgerEntryJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional
    public void append(List<LedgerEntry> entries) {
        if (entries == null || entries.isEmpty()) return;

        int i = 0;
        for (LedgerEntry e : entries) {
            LedgerEntryEntity entity = new LedgerEntryEntity(
                    UUID.fromString(e.id()),
                    UUID.fromString(e.transactionId().value()),
                    e.account().name(),
                    e.type().name(),
                    e.amount().asBigDecimal(),
                    e.referenceId()
            );
            em.persist(entity);

            // batching friendly
            if (++i % 50 == 0) {
                em.flush();
                em.clear();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Money agentAvailableBalance(String agentId) {
        return netBalance(LedgerAccount.AGENT, agentId);
    }

    @Override
    @Transactional(readOnly = true)
    public Money netBalance(LedgerAccount ledgerAccount, String referenceId) {
        BigDecimal v = repo.netBalance(ledgerAccount.name(), referenceId);
        return Money.of(v == null ? BigDecimal.ZERO : v);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> findByTransactionId(String transactionId) {
        List<LedgerEntryEntity> rows = repo.findByTransactionIdOrderByCreatedAtAscIdAsc(UUID.fromString(transactionId));
        return rows.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerEntry> findEntries(LedgerAccount ledgerAccount, String referenceId) {
        List<LedgerEntryEntity> rows = repo.findByAccountAndReferenceIdOrderByCreatedAtAscIdAsc(ledgerAccount.name(), referenceId);
        return rows.stream().map(this::toDomain).toList();
    }

    private LedgerEntry toDomain(LedgerEntryEntity e) {
        return new LedgerEntry(
                e.getId().toString(),
                TransactionId.of(e.getTransactionId().toString()),
                LedgerAccount.valueOf(e.getAccount()),
                LedgerEntryType.valueOf(e.getEntryType()),
                Money.of(e.getAmount()),
                e.getReferenceId()
        );
    }
}

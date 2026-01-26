package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import com.kori.domain.ledger.LedgerAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    List<LedgerEntryEntity> findByTransactionIdOrderByCreatedAtAscIdAsc(UUID transactionId);

    List<LedgerEntryEntity> findByAccountTypeAndOwnerRefOrderByCreatedAtAscIdAsc(
            LedgerAccountType accountType,
            String ownerRef
    );

    @Query("""
        select
          coalesce(sum(case when e.entryType = com.kori.domain.ledger.LedgerEntryType.CREDIT then e.amount else 0 end), 0)
          - coalesce(sum(case when e.entryType = com.kori.domain.ledger.LedgerEntryType.DEBIT then e.amount else 0 end), 0)
        from LedgerEntryEntity e
        where e.accountType = :accountType and e.ownerRef = :ownerRef
    """)
    BigDecimal netBalance(LedgerAccountType accountType, String ownerRef);
}


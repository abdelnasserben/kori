package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    List<LedgerEntryEntity> findByTransactionIdOrderByCreatedAtAscIdAsc(UUID transactionId);

    List<LedgerEntryEntity> findByAccountTypeAndOwnerRefOrderByCreatedAtAscIdAsc(
            String accountType,
            String ownerRef
    );

    @Query("""
        select
          coalesce(sum(case when e.entryType = 'CREDIT' then e.amount else 0 end), 0)
          - coalesce(sum(case when e.entryType = 'DEBIT' then e.amount else 0 end), 0)
        from LedgerEntryEntity e
        where e.ledgerAccountType = :ledgerAccountType and e.ownerRef = :ownerRef
    """)
    BigDecimal netBalance(@Param("ledgerAccountType") String accountType,
                          @Param("ownerRef") String ownerRef);
}


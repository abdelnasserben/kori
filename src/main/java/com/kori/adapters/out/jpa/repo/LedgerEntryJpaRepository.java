package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        where e.accountType = :ledgerAccountType and e.ownerRef = :ownerRef
    """)
    BigDecimal netBalance(@Param("ledgerAccountType") String accountType,
                          @Param("ownerRef") String ownerRef);

    @Query("""
        select e.transactionId
        from LedgerEntryEntity e
        group by e.transactionId
        having
          coalesce(sum(case when e.entryType = 'CREDIT' then e.amount else 0 end), 0)
            <> coalesce(sum(case when e.entryType = 'DEBIT' then e.amount else 0 end), 0)
          or coalesce(sum(case when e.entryType = 'CREDIT' then 1 else 0 end), 0) = 0
          or coalesce(sum(case when e.entryType = 'DEBIT' then 1 else 0 end), 0) = 0
    """)
    List<UUID> findInconsistentTransactionIds();

    @Query("""
        select coalesce(sum(e.amount), 0)
        from LedgerEntryEntity e
        join TransactionEntity t on t.id = e.transactionId
        where e.accountType = :accountType
          and e.ownerRef = :ownerRef
          and e.entryType = 'DEBIT'
          and t.type = :transactionType
          and t.createdAt >= :fromInclusive
          and t.createdAt < :toExclusive
    """)
    BigDecimal sumDebitsByTypeAndPeriod(@Param("accountType") String accountType,
                                        @Param("ownerRef") String ownerRef,
                                        @Param("transactionType") String transactionType,
                                        @Param("fromInclusive") OffsetDateTime fromInclusive,
                                        @Param("toExclusive") OffsetDateTime toExclusive);
}


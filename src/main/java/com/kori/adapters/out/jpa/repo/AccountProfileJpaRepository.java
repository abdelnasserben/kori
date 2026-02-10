package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.AccountProfileEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountProfileJpaRepository
        extends JpaRepository<AccountProfileEntity, AccountProfileEntity.AccountProfileId> {

    Optional<AccountProfileEntity> findByIdAccountTypeAndIdOwnerRef(String accountType, String ownerRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountProfileEntity a where a.id.accountType = :accountType and a.id.ownerRef = :ownerRef")
    Optional<AccountProfileEntity> findByIdAccountTypeAndIdOwnerRefForUpdate(String accountType, String ownerRef);
}

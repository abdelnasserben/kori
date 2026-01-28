package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.AccountProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountProfileJpaRepository
        extends JpaRepository<AccountProfileEntity, AccountProfileEntity.AccountProfileId> {

    Optional<AccountProfileEntity> findByIdAccountTypeAndIdOwnerRef(String accountType, String ownerRef);
}

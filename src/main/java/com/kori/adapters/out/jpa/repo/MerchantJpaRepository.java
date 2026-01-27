package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, UUID> {

    boolean existsByCode(String code);

    Optional<MerchantEntity> findByCode(String code);
}

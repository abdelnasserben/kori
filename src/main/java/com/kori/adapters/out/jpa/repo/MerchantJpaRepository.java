package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, String> {

    boolean existsByCode(String code);

    Optional<MerchantEntity> findByCode(String code);
}

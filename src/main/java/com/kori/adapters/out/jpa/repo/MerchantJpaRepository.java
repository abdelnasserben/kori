package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, String> {
}

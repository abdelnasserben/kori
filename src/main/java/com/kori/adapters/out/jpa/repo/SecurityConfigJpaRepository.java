package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.SecurityConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityConfigJpaRepository extends JpaRepository<SecurityConfigEntity, Integer> {
}

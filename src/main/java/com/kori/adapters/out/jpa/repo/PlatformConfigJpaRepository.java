package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.PlatformConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformConfigJpaRepository extends JpaRepository<PlatformConfigEntity, Integer> {
}

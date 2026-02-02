package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminJpaRepository extends JpaRepository<AdminEntity, UUID> {}

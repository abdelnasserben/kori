package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.AdminEntity;
import com.kori.domain.model.admin.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminJpaRepository extends JpaRepository<AdminEntity, UUID> {
    Optional<Admin> findByUsername(String username);
}

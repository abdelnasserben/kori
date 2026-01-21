package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, UUID> {
    Optional<ClientEntity> findByPhoneNumber(String phoneNumber);
}

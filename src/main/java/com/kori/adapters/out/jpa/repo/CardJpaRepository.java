package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CardJpaRepository extends JpaRepository<CardEntity, UUID> {
    Optional<CardEntity> findByCardUid(UUID cardUid);
}

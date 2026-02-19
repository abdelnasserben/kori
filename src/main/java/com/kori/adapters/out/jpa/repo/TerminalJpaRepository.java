package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.TerminalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TerminalJpaRepository extends JpaRepository<TerminalEntity, UUID> {
    List<TerminalEntity> findAllByMerchantId(UUID merchantId);

    Optional<TerminalEntity> findByTerminalUid(String terminalUId);

    boolean existsByTerminalUid(String terminalUid);
}

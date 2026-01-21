package com.kori.adapters.out.jpa.repo;

import com.kori.adapters.out.jpa.entity.TerminalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalJpaRepository extends JpaRepository<TerminalEntity, String> {
}

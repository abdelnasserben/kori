package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.repo.AgentJpaRepository;
import com.kori.application.port.out.AgentRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class JpaAgentRepositoryAdapter implements AgentRepositoryPort {

    private final AgentJpaRepository repo;

    public JpaAgentRepositoryAdapter(AgentJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String agentId) {
        Objects.requireNonNull(agentId, "agentId must not be null");
        return repo.existsById(agentId);
    }
}

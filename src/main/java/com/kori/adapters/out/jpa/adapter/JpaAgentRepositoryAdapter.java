package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.AgentEntity;
import com.kori.adapters.out.jpa.repo.AgentJpaRepository;
import com.kori.application.port.out.AgentRepositoryPort;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.common.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaAgentRepositoryAdapter implements AgentRepositoryPort {

    private final AgentJpaRepository repo;

    public JpaAgentRepositoryAdapter(AgentJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(AgentCode code) {
        return repo.existsByCode(code.value());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agent> findByCode(AgentCode code) {
        return repo.findByCode(code.value())
                .map(entity -> new Agent(
                        new AgentId(entity.getId()),
                        AgentCode.of(entity.getCode()),
                        entity.getCreatedAt(),
                        Status.valueOf(entity.getStatus())
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agent> findById(AgentId agentId) {
        return repo.findById(agentId.value())
                .map(entity -> new Agent(
                        new AgentId(entity.getId()),
                        AgentCode.of(entity.getCode()),
                        entity.getCreatedAt(),
                        Status.valueOf(entity.getStatus())
                ));
    }

    @Override
    @Transactional
    public Optional<Agent> findByIdForUpdate(AgentId agentId) {
        return repo.findByIdForUpdate(agentId.value())
                .map(entity -> new Agent(
                        new AgentId(entity.getId()),
                        AgentCode.of(entity.getCode()),
                        entity.getCreatedAt(),
                        Status.valueOf(entity.getStatus())
                ));
    }


    @Override
    public void save(Agent agent) {
        repo.save(new AgentEntity(
                agent.id().value(),
                agent.code().value(),
                agent.status().name(),
                agent.createdAt())
        );
    }
}

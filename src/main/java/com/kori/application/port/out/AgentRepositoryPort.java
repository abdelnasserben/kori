package com.kori.application.port.out;

import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;

import java.util.Optional;

public interface AgentRepositoryPort {

    boolean existsByCode(AgentCode code);

    Optional<Agent> findByCode(AgentCode code);

    Optional<Agent> findById(AgentId agentId);

    Optional<Agent> findByIdForUpdate(AgentId agentId);

    void save(Agent agent);
}

package com.kori.application.port.out;

import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;

import java.util.Optional;

public interface AgentRepositoryPort {

    boolean existsByCode(AgentCode code);

    Optional<Agent> findByCode(AgentCode code);

    void save(Agent agent);
}

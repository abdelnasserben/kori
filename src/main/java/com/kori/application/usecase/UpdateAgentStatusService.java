package com.kori.application.usecase;

import com.kori.application.command.UpdateAgentStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateAgentStatusUseCase;
import com.kori.application.port.out.AgentRepositoryPort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateAgentStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UpdateAgentStatusService implements UpdateAgentStatusUseCase {

    private final AgentRepositoryPort agentRepositoryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdateAgentStatusService(
            AgentRepositoryPort agentRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort) {
        this.agentRepositoryPort = agentRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdateAgentStatusResult execute(UpdateAgentStatusCommand cmd) {
        requireAdmin(cmd.actorContext());

        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(cmd.agentCode()))
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        // For audit
        String before = agent.status().name();

        // Apply updating
        switch (Status.valueOf(cmd.targetStatus())) {
            case ACTIVE -> agent.activate();
            case SUSPENDED -> agent.suspend();
            case CLOSED -> agent.close();
        }
        agentRepositoryPort.save(agent);

        // Audit
        String auditAction = "ADMIN_UPDATE_AGENT_STATUS_" + cmd.targetStatus();
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("agentCode", cmd.agentCode());
        metadata.put("before", before);
        metadata.put("after", cmd.targetStatus());
        metadata.put("reason", cmd.reason());

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorId(),
                now,
                metadata
        ));
        return new UpdateAgentStatusResult(cmd.agentCode(), before, cmd.targetStatus());
    }

    private void requireAdmin(ActorContext actor) {
        if (actor == null || actor.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update agent status");
        }
    }
}

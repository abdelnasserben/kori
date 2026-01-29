package com.kori.application.usecase;

import com.kori.application.command.UpdateAgentStatusCommand;
import com.kori.application.events.AgentStatusChangedEvent;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateAgentStatusUseCase;
import com.kori.application.port.out.AgentRepositoryPort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.DomainEventPublisherPort;
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
import java.util.UUID;

public class UpdateAgentStatusService implements UpdateAgentStatusUseCase {

    private final AgentRepositoryPort agentRepositoryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;
    private final DomainEventPublisherPort domainEventPublisherPort;

    public UpdateAgentStatusService(
            AgentRepositoryPort agentRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort
    ) {
        this.agentRepositoryPort = agentRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
        this.domainEventPublisherPort = domainEventPublisherPort;
    }

    @Override
    public UpdateAgentStatusResult execute(UpdateAgentStatusCommand cmd) {
        requireAdmin(cmd.actorContext());

        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(cmd.agentCode()))
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        Status beforeStatus = agent.status();
        String before = beforeStatus.name();
        Status afterStatus = Status.valueOf(cmd.targetStatus());

        switch (afterStatus) {
            case ACTIVE -> agent.activate();
            case SUSPENDED -> agent.suspend();
            case CLOSED -> agent.close();
        }

        agentRepositoryPort.save(agent);

        String reason = normalizeReason(cmd.reason());

        String auditAction = "ADMIN_UPDATE_AGENT_STATUS_" + cmd.targetStatus();
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("agentCode", cmd.agentCode());
        metadata.put("before", before);
        metadata.put("after", cmd.targetStatus());
        metadata.put("reason", reason);

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorId(),
                now,
                metadata
        ));

        if (beforeStatus != afterStatus) {
            domainEventPublisherPort.publish(new AgentStatusChangedEvent(
                    UUID.randomUUID().toString(),
                    now,
                    agent.id(),
                    beforeStatus,
                    afterStatus,
                    reason
            ));
        }

        return new UpdateAgentStatusResult(cmd.agentCode(), before, cmd.targetStatus());
    }

    private void requireAdmin(ActorContext actor) {
        if (actor == null || actor.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update agent status");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null) return "N/A";
        String trimmed = reason.trim();
        return trimmed.isBlank() ? "N/A" : trimmed;
    }
}

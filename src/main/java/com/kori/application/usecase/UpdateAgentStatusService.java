package com.kori.application.usecase;

import com.kori.application.command.UpdateAgentStatusCommand;
import com.kori.application.events.AgentStatusChangedEvent;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.UpdateAgentStatusUseCase;
import com.kori.application.port.out.AgentRepositoryPort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.DomainEventPublisherPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateAgentStatusResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.common.Status;

import java.time.Instant;
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
        ActorGuards.requireAdmin(cmd.actorContext(), "update agent status");

        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(cmd.agentCode()))
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        Status beforeStatus = agent.status();
        String before = beforeStatus.name();
        Status afterStatus = Status.parseStatus(cmd.targetStatus());

        switch (afterStatus) {
            case ACTIVE -> agent.activate();
            case SUSPENDED -> agent.suspend();
            case CLOSED -> agent.close();
        }

        agentRepositoryPort.save(agent);

        String reason = ReasonNormalizer.normalize(cmd.reason());

        Instant now = timeProviderPort.now();

        // Audit
        auditPort.publish(AuditBuilder.buildStatusChangeAudit(
                "ADMIN_UPDATE_AGENT_STATUS",
                cmd.actorContext(),
                now,
                "agentCode",
                cmd.agentCode(),
                before,
                cmd.targetStatus(),
                reason
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
}

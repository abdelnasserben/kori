package com.kori.application.usecase;

import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AgentUpdateCardStatusUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AgentUpdateCardStatusResult;
import com.kori.application.security.ActorType;
import com.kori.domain.model.card.AgentCardAction;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class AgentUpdateCardStatusService implements AgentUpdateCardStatusUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final AgentRepositoryPort agentRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AuditPort auditPort;

    public AgentUpdateCardStatusService(TimeProviderPort timeProviderPort,
                                        IdempotencyPort idempotencyPort,
                                        AgentRepositoryPort agentRepositoryPort,
                                        CardRepositoryPort cardRepositoryPort,
                                        AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public AgentUpdateCardStatusResult execute(AgentUpdateCardStatusCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), AgentUpdateCardStatusResult.class);
        if (cached.isPresent()) return cached.get();

        if (command.actorContext().actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Only AGENT can update card status");
        }

        if (!agentRepositoryPort.existsById(command.agentId())) {
            throw new ForbiddenOperationException("Agent not found");
        }

        Card card = cardRepositoryPort.findByCardUid(command.cardUid())
                .orElseThrow(() -> new ForbiddenOperationException("Card not found"));

        CardStatus targetStatus = switch (command.action()) {
            case BLOCKED -> CardStatus.BLOCKED;
            case LOST -> CardStatus.LOST;
        };

        Card updated = new Card(
                card.id(),
                card.accountId(),
                card.cardUid(),
                card.pin(),
                targetStatus,
                card.failedPinAttempts()
        );

        updated = cardRepositoryPort.save(updated);

        Instant now = timeProviderPort.now();

        String auditAction = (command.action() == AgentCardAction.BLOCKED)
                ? "AGENT_BLOCK_CARD"
                : "AGENT_MARK_CARD_LOST";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("agentId", command.agentId());
        metadata.put("cardUid", command.cardUid());
        metadata.put("reason", command.reason());

        auditPort.publish(new AuditEvent(
                auditAction,
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        AgentUpdateCardStatusResult result = new AgentUpdateCardStatusResult(
                updated.id().value(),
                updated.cardUid(),
                updated.status().name()
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}

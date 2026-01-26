package com.kori.application.usecase;

import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.AgentUpdateCardStatusUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.UpdateCardStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AgentUpdateCardStatusService implements AgentUpdateCardStatusUseCase {

    private final TimeProviderPort timeProviderPort;
    private final AgentRepositoryPort agentRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AuditPort auditPort;

    public AgentUpdateCardStatusService(
            TimeProviderPort timeProviderPort,
            AgentRepositoryPort agentRepositoryPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public UpdateCardStatusResult execute(AgentUpdateCardStatusCommand cmd) {

        requireAgentActor(cmd.actorContext());

        if (cmd.targetStatus() != CardStatus.BLOCKED && cmd.targetStatus() != CardStatus.LOST) {
            throw new ForbiddenOperationException("Agent can only can set it");
        }

        // Agent must be active
        Agent agent = agentRepositoryPort.findByCode(cmd.agentCode())
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        if (agent.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("Agent is not active");
        }

        Card card = getCard(cmd.cardUid());
        CardStatus before = card.status(); // for audit

        switch (cmd.targetStatus()) {
            case BLOCKED -> card.block();
            case LOST -> card.markLost();
            default -> throw new ForbiddenOperationException("Unexpected target status: " + cmd.targetStatus());
        }

        cardRepositoryPort.save(card);

        // Audit
        Instant now = timeProviderPort.now();
        String auditAction = cmd.targetStatus() == CardStatus.BLOCKED
                ? "AGENT_BLOCK_CARD"
                : "AGENT_MARK_CARD_LOST";

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorId(),
                now,
                Map.of(
                        "cardId", card.id().toString(),
                        "before", before.name(),
                        "after", card.status().name(),
                        "reason", cmd.reason()
                )
        ));

        return new UpdateCardStatusResult(cmd.cardUid(), before, card.status());
    }

    private Card getCard(UUID cardUid) {
        return cardRepositoryPort.findByCardUid(cardUid.toString())
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    private static void requireAgentActor(ActorContext ctx) {
        if (ctx.actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Actor must be an AGENT");
        }
    }
}

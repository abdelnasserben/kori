package com.kori.application.usecase;

import com.kori.application.command.AgentUpdateCardStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorStatusGuards;
import com.kori.application.guard.ActorTypeGuards;
import com.kori.application.port.in.AgentUpdateCardStatusUseCase;
import com.kori.application.port.out.AgentRepositoryPort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateCardStatusResult;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class AgentUpdateCardStatusService implements AgentUpdateCardStatusUseCase {

    private final AgentRepositoryPort agentRepositoryPort;
    private final TimeProviderPort timeProviderPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AuditPort auditPort;

    public AgentUpdateCardStatusService(
            AgentRepositoryPort agentRepositoryPort,
            TimeProviderPort timeProviderPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort) {
        this.agentRepositoryPort = agentRepositoryPort;
        this.timeProviderPort = timeProviderPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public UpdateCardStatusResult execute(AgentUpdateCardStatusCommand cmd) {

        ActorTypeGuards.onlyAgentCan(cmd.actorContext(), "update card status");

        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(cmd.actorContext().actorRef()))
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        ActorStatusGuards.requireActiveAgent(agent);

        if (!Objects.equals(cmd.targetStatus(), CardStatus.BLOCKED.name())
                && !Objects.equals(cmd.targetStatus(), CardStatus.LOST.name())) {
            throw new ForbiddenOperationException("Agent can only can set it");
        }

        Card card = getCard(cmd.cardUid());
        String before = card.status().name(); // for audit

        switch (CardStatus.valueOf(cmd.targetStatus())) {
            case BLOCKED -> card.block();
            case LOST -> card.markLost();
            default -> throw new ForbiddenOperationException("Unexpected target status: " + cmd.targetStatus());
        }

        cardRepositoryPort.save(card);

        // Audit
        Instant now = timeProviderPort.now();
        String auditAction = cmd.targetStatus().equals(CardStatus.BLOCKED.name())
                ? "AGENT_BLOCK_CARD"
                : "AGENT_MARK_CARD_LOST";

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorRef(),
                now,
                Map.of(
                        "cardId", card.id().value().toString(),
                        "before", before,
                        "after", card.status().name(),
                        "reason", cmd.reason()
                )
        ));

        return new UpdateCardStatusResult(cmd.cardUid(), before, card.status().name());
    }

    private Card getCard(String cardUid) {
        return cardRepositoryPort.findByCardUid(cardUid)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }
}

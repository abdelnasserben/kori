package com.kori.application.usecase;

import com.kori.application.command.AdminUpdateCardStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUpdateCardStatusUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AdminUpdateCardStatusResult;
import com.kori.application.security.ActorType;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class AdminUpdateCardStatusService implements AdminUpdateCardStatusUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AuditPort auditPort;

    public AdminUpdateCardStatusService(TimeProviderPort timeProviderPort,
                                        IdempotencyPort idempotencyPort,
                                        CardRepositoryPort cardRepositoryPort,
                                        AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public AdminUpdateCardStatusResult execute(AdminUpdateCardStatusCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), AdminUpdateCardStatusResult.class);
        if (cached.isPresent()) return cached.get();

        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update card status");
        }

        Card card = cardRepositoryPort.findByCardUid(command.cardUid())
                .orElseThrow(() -> new ForbiddenOperationException("Card not found"));

        CardStatus target = getCardStatus(command, card);

        Card updated = card.transitionTo(target);
        cardRepositoryPort.save(updated);

        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("cardUid", command.cardUid());
        metadata.put("reason", command.reason());

        String auditAction = "ADMIN_SET_CARD_STATUS_" + command.action().name();

        auditPort.publish(new AuditEvent(
                auditAction,
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        AdminUpdateCardStatusResult result = new AdminUpdateCardStatusResult(
                updated.id().value(),
                updated.cardUid(),
                updated.status().name()
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }

    private static CardStatus getCardStatus(AdminUpdateCardStatusCommand command, Card card) {
        CardStatus target = switch (command.action()) {
            case ACTIVE -> CardStatus.ACTIVE;
            case SUSPENDED -> CardStatus.SUSPENDED;
            case INACTIVE -> CardStatus.INACTIVE;
        };

        // Governance rule: BLOCKED -> ACTIVE must go through AdminUnblockCard
        if (card.status() == CardStatus.BLOCKED && target == CardStatus.ACTIVE) {
            throw new ForbiddenOperationException("Use AdminUnblockCard to reactivate a BLOCKED card");
        }

        // LOST terminal: only allow LOST -> INACTIVE
        if (card.status() == CardStatus.LOST && target != CardStatus.INACTIVE) {
            throw new ForbiddenOperationException("LOST card can only be set to INACTIVE by admin");
        }
        return target;
    }
}

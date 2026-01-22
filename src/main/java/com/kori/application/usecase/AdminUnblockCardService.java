package com.kori.application.usecase;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AdminUnblockCardResult;
import com.kori.application.security.ActorType;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class AdminUnblockCardService implements AdminUnblockCardUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AuditPort auditPort;

    public AdminUnblockCardService(TimeProviderPort timeProviderPort,
                                   IdempotencyPort idempotencyPort,
                                   CardRepositoryPort cardRepositoryPort,
                                   AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public AdminUnblockCardResult execute(AdminUnblockCardCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), AdminUnblockCardResult.class);
        if (cached.isPresent()) return cached.get();

        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can unblock cards");
        }

        Card card = cardRepositoryPort.findByCardUid(command.cardUid())
                .orElseThrow(() -> new ForbiddenOperationException("Card not found"));

        if ("LOST".equals(card.status().name())) {
            throw new ForbiddenOperationException("Cannot unblock a LOST card");
        }

        if (!"BLOCKED".equals(card.status().name())) {
            throw new ForbiddenOperationException("Card is not blocked");
        }

        Card updated = new Card(
                card.id(),
                card.accountId(),
                card.cardUid(),
                card.hashedPin(),
                CardStatus.ACTIVE,
                0
        );
        updated = cardRepositoryPort.save(updated);

        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("cardUid", command.cardUid());
        metadata.put("reason", command.reason());

        auditPort.publish(new AuditEvent(
                "ADMIN_UNBLOCK_CARD",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        AdminUnblockCardResult result = new AdminUnblockCardResult(
                updated.id().value(),
                updated.cardUid(),
                updated.status().name(),
                updated.failedPinAttempts()
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}

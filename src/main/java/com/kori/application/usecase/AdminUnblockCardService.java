package com.kori.application.usecase;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.UpdateCardStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
    public UpdateCardStatusResult execute(AdminUnblockCardCommand cmd) {

        requireAdminActor(cmd.actorContext());

        Card card = getCard(cmd.cardUid());
        CardStatus before = card.status(); // for audit

        // Domaine impose: only if BLOCKED, reset failedPinAttempts, set ACTIVE
        card.unblock();

        cardRepositoryPort.save(card);

        Instant now = timeProviderPort.now();

        auditPort.publish(new AuditEvent(
                "ADMIN_UNBLOCK_CARD",
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

    private void requireAdminActor(ActorContext ctx) {
        if (ctx.actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Actor must be an ADMIN");
        }
    }
}

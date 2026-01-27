package com.kori.application.usecase;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateCardStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.card.Card;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class AdminUnblockCardService implements AdminUnblockCardUseCase {

    private final TimeProviderPort timeProviderPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AuditPort auditPort;

    public AdminUnblockCardService(TimeProviderPort timeProviderPort,
                                   CardRepositoryPort cardRepositoryPort,
                                   AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public UpdateCardStatusResult execute(AdminUnblockCardCommand cmd) {

        requireAdminActor(cmd.actorContext());

        Card card = getCard(cmd.cardUid());
        String before = card.status().name(); // for audit

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
                        "before", before,
                        "after", card.status().name(),
                        "reason", cmd.reason()
                )
        ));

        return new UpdateCardStatusResult(cmd.cardUid(), before, card.status().name());
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

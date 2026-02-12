package com.kori.application.usecase;

import com.kori.application.command.AdminUnblockCardCommand;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.AdminUnblockCardUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateCardStatusResult;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.card.Card;

import java.time.Instant;
import java.util.Map;

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

        ActorGuards.requireAdmin(cmd.actorContext(), "unblock card");

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

package com.kori.application.usecase;

import com.kori.application.command.AdminUpdateCardStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.AdminUpdateCardStatusUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CardRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateCardStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.CardStatus;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class AdminUpdateCardStatusService implements AdminUpdateCardStatusUseCase {

    private final AdminAccessService adminAccessService;
    private final TimeProviderPort timeProviderPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AuditPort auditPort;

    public AdminUpdateCardStatusService(
            AdminAccessService adminAccessService, TimeProviderPort timeProviderPort,
            CardRepositoryPort cardRepositoryPort,
            AuditPort auditPort) {
        this.adminAccessService = adminAccessService;
        this.timeProviderPort = timeProviderPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public UpdateCardStatusResult execute(AdminUpdateCardStatusCommand cmd) {

        ActorContext actorContext = cmd.actorContext();

        adminAccessService.requireActiveAdmin(actorContext, "update card status");

        if (!Objects.equals(cmd.targetStatus(), CardStatus.ACTIVE.name())
                && !Objects.equals(cmd.targetStatus(), CardStatus.INACTIVE.name())
                && !Objects.equals(cmd.targetStatus(), CardStatus.SUSPENDED.name())) {
            throw new ForbiddenOperationException("Admin can only set it");
        }

        Card card = getCard(cmd.cardUid());
        String before = card.status().name(); // for audit

        // Domaine fait respecter LOST terminal + refus BLOCKED->ACTIVE via activate()
        switch (CardStatus.valueOf(cmd.targetStatus())) {
            case ACTIVE -> card.activate();
            case INACTIVE -> card.deactivate();
            case SUSPENDED -> card.suspend();
            default -> throw new ForbiddenOperationException("Unexpected target status: " + cmd.targetStatus());
        }

        cardRepositoryPort.save(card);

        // Audit
        String auditAction = "ADMIN_CARD_STATUS_UPDATED_" + cmd.targetStatus();
        Instant now = timeProviderPort.now();

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorRef(),
                now,
                Map.of(
                        "cardId", card.id().value().toString(),
                        "before", before,
                        "after", card.status().name(),
                        "target", cmd.targetStatus(),
                        "reason", cmd.reason()
                )
        ));

        return new UpdateCardStatusResult(cmd.cardUid(), before, cmd.targetStatus());
    }

    private Card getCard(String cardUid) {
        return cardRepositoryPort.findByCardUid(cardUid)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }
}

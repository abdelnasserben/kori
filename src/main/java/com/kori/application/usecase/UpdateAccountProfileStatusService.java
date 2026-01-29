package com.kori.application.usecase;

import com.kori.application.command.UpdateAccountProfileStatusCommand;
import com.kori.application.events.AccountProfileStatusChangedEvent;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateAccountProfileStatusUseCase;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.DomainEventPublisherPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateAccountProfileStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpdateAccountProfileStatusService implements UpdateAccountProfileStatusUseCase {

    private final AccountProfilePort accountProfilePort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;
    private final DomainEventPublisherPort domainEventPublisherPort;

    public UpdateAccountProfileStatusService(
            AccountProfilePort accountProfilePort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort
    ) {
        this.accountProfilePort = accountProfilePort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
        this.domainEventPublisherPort = domainEventPublisherPort;
    }

    @Override
    public UpdateAccountProfileStatusResult execute(UpdateAccountProfileStatusCommand cmd) {
        requireAdmin(cmd.actorContext());

        LedgerAccountRef accountRef = resolveLedgerAccountRef(cmd.accountType(), cmd.ownerRef());

        AccountProfile accountProfile = accountProfilePort.findByAccount(accountRef)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        Status beforeStatus = accountProfile.status();
        String before = beforeStatus.name();
        Status afterStatus = Status.valueOf(cmd.targetStatus());

        switch (afterStatus) {
            case ACTIVE -> accountProfile.activate();
            case SUSPENDED -> accountProfile.suspend();
            case CLOSED -> accountProfile.close();
        }
        accountProfilePort.save(accountProfile);

        // Normalize reason (blank => N/A)
        String reason = normalizeReason(cmd.reason());

        String auditAction = "ADMIN_UPDATE_ACCOUNT_PROFILE_STATUS_" + cmd.targetStatus();
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("ledgerAccountType", cmd.accountType());
        metadata.put("ownerRef", cmd.ownerRef());
        metadata.put("before", before);
        metadata.put("after", cmd.targetStatus());
        metadata.put("reason", reason);

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorId(),
                now,
                metadata
        ));

        if (beforeStatus != afterStatus) {
            domainEventPublisherPort.publish(new AccountProfileStatusChangedEvent(
                    UUID.randomUUID().toString(),
                    now,
                    accountProfile.account(),
                    beforeStatus,
                    afterStatus,
                    reason
            ));
        }

        return new UpdateAccountProfileStatusResult(cmd.accountType(), cmd.ownerRef(), before, cmd.targetStatus());
    }

    private LedgerAccountRef resolveLedgerAccountRef(String accountType, String ownerRef) {
        LedgerAccountType type = LedgerAccountType.valueOf(accountType);
        return new LedgerAccountRef(type, ownerRef);
    }

    private void requireAdmin(ActorContext actor) {
        if (actor == null || actor.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update account status");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null) return "N/A";
        String trimmed = reason.trim();
        return trimmed.isBlank() ? "N/A" : trimmed;
    }
}

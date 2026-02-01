package com.kori.application.usecase;

import com.kori.application.command.UpdateAccountProfileStatusCommand;
import com.kori.application.events.AccountProfileStatusChangedEvent;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.UpdateAccountProfileStatusUseCase;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.DomainEventPublisherPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateAccountProfileStatusResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.Status;

import java.time.Instant;
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
        ActorGuards.requireAdmin(cmd.actorContext(), "update account status");

        LedgerAccountRef accountRef = resolveLedgerAccountRef(cmd.accountType(), cmd.ownerRef());

        AccountProfile accountProfile = accountProfilePort.findByAccount(accountRef)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        Status beforeStatus = accountProfile.status();
        String before = beforeStatus.name();
        Status afterStatus = Status.parseStatus(cmd.targetStatus());

        switch (afterStatus) {
            case ACTIVE -> accountProfile.activate();
            case SUSPENDED -> accountProfile.suspend();
            case CLOSED -> accountProfile.close();
        }
        accountProfilePort.save(accountProfile);

        // Normalize reason (blank => N/A)
        String reason = ReasonNormalizer.normalize(cmd.reason());

        Instant now = timeProviderPort.now();

        // Audit
        auditPort.publish(AuditBuilder.buildStatusChangeAudit(
                "ADMIN_UPDATE_ACCOUNT_PROFILE_STATUS",
                cmd.actorContext(),
                now,
                "ownerRef",
                cmd.ownerRef(),
                before,
                cmd.targetStatus(),
                reason,
                Map.of("ledgerAccountType", cmd.accountType())
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
}

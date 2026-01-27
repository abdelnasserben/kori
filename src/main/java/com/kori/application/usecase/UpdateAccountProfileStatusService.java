package com.kori.application.usecase;

import com.kori.application.command.UpdateAccountProfileStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateAccountProfileStatusUseCase;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.port.out.AuditPort;
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

public class UpdateAccountProfileStatusService implements UpdateAccountProfileStatusUseCase {

    private final AccountProfilePort accountProfilePort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdateAccountProfileStatusService(
            AccountProfilePort accountProfilePort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort) {
        this.accountProfilePort = accountProfilePort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdateAccountProfileStatusResult execute(UpdateAccountProfileStatusCommand cmd) {
        requireAdmin(cmd.actorContext());

        LedgerAccountRef accountRef = resolveLedgerAccountRef(cmd.accountType(), cmd.ownerRef());

        AccountProfile accountProfile = accountProfilePort.findByAccount(accountRef)
                .orElseThrow(() -> new NotFoundException("Account not found"));

        // For audit
        String before = accountProfile.status().name();

        // Apply updating
        switch (Status.valueOf(cmd.targetStatus())) {
            case ACTIVE -> accountProfile.activate();
            case SUSPENDED -> accountProfile.suspend();
            case CLOSED -> accountProfile.close();
        }
        accountProfilePort.save(accountProfile);

        // Audit
        String auditAction = "ADMIN_UPDATE_ACCOUNT_PROFILE_STATUS_" + cmd.targetStatus();
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("ledgerAccountType", cmd.accountType());
        metadata.put("ownerRef", cmd.ownerRef());
        metadata.put("before", before);
        metadata.put("after", cmd.targetStatus());
        metadata.put("reason", cmd.reason());

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorId(),
                now,
                metadata
        ));

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
}

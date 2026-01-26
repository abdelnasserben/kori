package com.kori.application.usecase;

import com.kori.application.command.UpdateAccountProfileStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateAccountProfileStatusUseCase;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.port.out.AuditEvent;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateAccountProfileStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.account.AccountProfile;
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

        AccountProfile accountProfile = accountProfilePort.findByAccount(cmd.accountRef())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        // For audit
        Status before = accountProfile.status();

        // Apply updating
        switch (cmd.targetStatus()) {
            case ACTIVE -> accountProfile.activate();
            case SUSPENDED -> accountProfile.suspend();
            case CLOSED -> accountProfile.close();
        }
        accountProfilePort.save(accountProfile);

        // Audit
        String auditAction = "ADMIN_UPDATE_ACCOUNT_PROFILE_STATUS_" + cmd.targetStatus();
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("ledgerAccountType", cmd.accountRef().type().name());
        metadata.put("ownerRef", cmd.accountRef().ownerRef());
        metadata.put("before", before.name());
        metadata.put("after", cmd.targetStatus().name());
        metadata.put("reason", cmd.reason());

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorId(),
                now,
                metadata
        ));

        return new UpdateAccountProfileStatusResult(cmd.accountRef(), before, cmd.targetStatus());
    }

    private void requireAdmin(ActorContext actor) {
        if (actor == null || actor.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update account status");
        }
    }
}

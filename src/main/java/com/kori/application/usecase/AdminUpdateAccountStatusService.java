package com.kori.application.usecase;

import com.kori.application.command.AdminUpdateAccountStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.AdminUpdateAccountStatusUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.AdminUpdateAccountStatusResult;
import com.kori.application.security.ActorType;
import com.kori.domain.model.account.Account;
import com.kori.domain.model.account.AccountId;
import com.kori.domain.model.common.Status;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class AdminUpdateAccountStatusService implements AdminUpdateAccountStatusUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final AuditPort auditPort;

    public AdminUpdateAccountStatusService(TimeProviderPort timeProviderPort,
                                           IdempotencyPort idempotencyPort,
                                           AccountRepositoryPort accountRepositoryPort,
                                           AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.accountRepositoryPort = accountRepositoryPort;
        this.auditPort = auditPort;
    }

    @Override
    public AdminUpdateAccountStatusResult execute(AdminUpdateAccountStatusCommand command) {
        var cached = idempotencyPort.find(command.idempotencyKey(), AdminUpdateAccountStatusResult.class);
        if (cached.isPresent()) return cached.get();

        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update account status");
        }

        AccountId accountId = AccountId.of(command.accountId());
        Account account = accountRepositoryPort.findById(accountId)
                .orElseThrow(() -> new ForbiddenOperationException("Account not found"));

        Status target = switch (command.action()) {
            case ACTIVE -> Status.ACTIVE;
            case SUSPENDED -> Status.SUSPENDED;
            case INACTIVE -> Status.INACTIVE;
        };

        Account updated = new Account(account.id(), account.clientId(), target);
        updated = accountRepositoryPort.save(updated);

        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("accountId", updated.id().value());
        metadata.put("reason", command.reason());

        String auditAction = "ADMIN_SET_ACCOUNT_STATUS_" + command.action().name();

        auditPort.publish(new AuditEvent(
                auditAction,
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));

        AdminUpdateAccountStatusResult result =
                new AdminUpdateAccountStatusResult(updated.id().value(), updated.status().name());

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}

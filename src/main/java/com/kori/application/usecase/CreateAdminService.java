package com.kori.application.usecase;

import com.kori.application.command.CreateAdminCommand;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.CreateAdminUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateAdminResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.admin.AdminId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CreateAdminService implements CreateAdminUseCase {

    private final AdminRepositoryPort adminRepositoryPort;
    private final IdempotencyPort idempotencyPort;
    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final AuditPort auditPort;

    public CreateAdminService(AdminRepositoryPort adminRepositoryPort, IdempotencyPort idempotencyPort, TimeProviderPort timeProviderPort, IdGeneratorPort idGeneratorPort, AuditPort auditPort) {
        this.adminRepositoryPort = Objects.requireNonNull(adminRepositoryPort, "adminRepositoryPort");
        this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort");
        this.timeProviderPort = Objects.requireNonNull(timeProviderPort, "timeProviderPort");
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort, "idGeneratorPort");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
    }

    @Override
    public CreateAdminResult execute(CreateAdminCommand command) {
        Objects.requireNonNull(command, "command");
        var actorContext = command.actorContext();

        ActorGuards.requireAdmin(actorContext, "create admin");

        var cached = idempotencyPort.find(command.idempotencyKey(), command.idempotencyRequestHash(), CreateAdminResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        Instant now = timeProviderPort.now();

        var inProgress = IdempotencyReservations.reserveOrLoad(
                idempotencyPort,
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                CreateAdminResult.class
        );
        if (inProgress.isPresent()) {
            return inProgress.get();
        }

        AdminId adminId = new AdminId(idGeneratorPort.newUuid());
        Admin admin = Admin.activeNew(adminId, now);
        adminRepositoryPort.save(admin);

        CreateAdminResult result = new CreateAdminResult(adminId.value().toString());
        idempotencyPort.save(command.idempotencyKey(), command.idempotencyRequestHash(), result);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("adminId", actorContext.actorId());
        metadata.put("createdAdminId", adminId.value().toString());

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "ADMIN_CREATED",
                actorContext,
                now,
                metadata
        ));

        return result;
    }
}

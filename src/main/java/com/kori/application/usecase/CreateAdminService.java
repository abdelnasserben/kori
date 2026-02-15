package com.kori.application.usecase;

import com.kori.application.command.CreateAdminCommand;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.CreateAdminUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateAdminResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.admin.AdminId;
import com.kori.domain.model.admin.AdminUsername;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CreateAdminService implements CreateAdminUseCase {

    private final AdminAccessService adminAccessService;
    private final AdminRepositoryPort adminRepositoryPort;
    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final AuditPort auditPort;
    private final IdempotencyExecutor idempotencyExecutor;

    public CreateAdminService(AdminAccessService adminAccessService, AdminRepositoryPort adminRepositoryPort, IdempotencyPort idempotencyPort, TimeProviderPort timeProviderPort, IdGeneratorPort idGeneratorPort, AuditPort auditPort) {
        this.adminAccessService = adminAccessService;
        this.adminRepositoryPort = Objects.requireNonNull(adminRepositoryPort, "adminRepositoryPort");
        this.timeProviderPort = Objects.requireNonNull(timeProviderPort, "timeProviderPort");
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort, "idGeneratorPort");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public CreateAdminResult execute(CreateAdminCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                CreateAdminResult.class,
                () -> {

                    var actorContext = command.actorContext();
                    adminAccessService.requireActiveAdmin(actorContext, "create admin");

                    Instant now = timeProviderPort.now();

                    AdminId adminId = new AdminId(idGeneratorPort.newUuid());
                    AdminUsername username = AdminUsername.of(command.username());
                    Admin newAdmin = Admin.activeNew(adminId, username, now);
                    adminRepositoryPort.save(newAdmin);

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("createdAdminUsername", newAdmin.username().value());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "ADMIN_CREATED",
                            actorContext,
                            now,
                            metadata
                    ));

                    return new CreateAdminResult(adminId.value().toString());
                }
        );
    }
}

package com.kori.application.usecase;

import com.kori.application.command.UpdateAdminStatusCommand;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateAdminStatusUseCase;
import com.kori.application.port.out.AdminRepositoryPort;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateAdminStatusResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.common.Status;

import java.time.Instant;

public class UpdateAdminStatusService implements UpdateAdminStatusUseCase {

    private final AdminAccessService adminAccessService;
    private final AdminRepositoryPort adminRepositoryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdateAdminStatusService(
            AdminAccessService adminAccessService, AdminRepositoryPort adminRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        this.adminAccessService = adminAccessService;
        this.adminRepositoryPort = adminRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdateAdminStatusResult execute(UpdateAdminStatusCommand cmd) {
        adminAccessService.requireActiveAdmin(cmd.actorContext(), "update admin status");

        Admin admin = adminRepositoryPort.findByUsername(cmd.adminUsername())
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        Status beforeStatus = admin.status();
        String before = beforeStatus.name();
        Status afterStatus = Status.parseStatus(cmd.targetStatus());

        switch (afterStatus) {
            case ACTIVE -> admin.activate();
            case SUSPENDED -> admin.suspend();
            case CLOSED -> admin.close();
        }

        adminRepositoryPort.save(admin);

        String reason = ReasonNormalizer.normalize(cmd.reason());
        Instant now = timeProviderPort.now();

        auditPort.publish(AuditBuilder.buildStatusChangeAudit(
                "ADMIN_UPDATE_ADMIN_STATUS",
                cmd.actorContext(),
                now,
                "adminUsername",
                cmd.adminUsername(),
                before,
                cmd.targetStatus(),
                reason
        ));

        return new UpdateAdminStatusResult(cmd.adminUsername(), before, cmd.targetStatus());
    }
}

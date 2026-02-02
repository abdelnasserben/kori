package com.kori.it;

import com.kori.application.command.UpdateAdminStatusCommand;
import com.kori.application.port.in.UpdateAdminStatusUseCase;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.admin.AdminId;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateAdminStatusServiceIT extends IntegrationTestBase {

    @Autowired
    UpdateAdminStatusUseCase updateAdminStatusUseCase;

    @Test
    void updateAdminStatus_suspendsAdmin() {
        Admin admin = createActiveAdmin();

        updateAdminStatusUseCase.execute(new UpdateAdminStatusCommand(
                adminActor(),
                admin.id().value().toString(),
                Status.SUSPENDED.name(),
                "test"
        ));

        Admin updated = adminRepositoryPort.findById(new AdminId(admin.id().value())).orElseThrow();
        assertEquals(Status.SUSPENDED, updated.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_UPDATE_ADMIN_STATUS"))
        );
    }
}

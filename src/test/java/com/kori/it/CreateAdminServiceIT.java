package com.kori.it;

import com.kori.application.command.CreateAdminCommand;
import com.kori.application.port.in.CreateAdminUseCase;
import com.kori.application.result.CreateAdminResult;
import com.kori.domain.model.admin.Admin;
import com.kori.domain.model.admin.AdminId;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CreateAdminServiceIT extends IntegrationTestBase {

    @Autowired
    CreateAdminUseCase createAdminUseCase;

    @Test
    void createAdmin_happyPath_persistsAdminAndAudit() {
        CreateAdminResult result = createAdminUseCase.execute(new CreateAdminCommand(
                "idem-create-admin-1",
                adminActor()
        ));

        assertNotNull(result.adminId());

        Admin admin = adminRepositoryPort.findById(new AdminId(UUID.fromString(result.adminId()))).orElseThrow();
        assertEquals(Status.ACTIVE, admin.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_CREATED"))
        );
    }
}

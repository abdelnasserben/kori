package com.kori.application.usecase;

import com.kori.application.command.UpdatePlatformConfigCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.PlatformConfigPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdatePlatformConfigResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.config.PlatformConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class UpdatePlatformConfigServiceTest {

    @Mock PlatformConfigPort platformConfigPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;

    @InjectMocks UpdatePlatformConfigService service;

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, "admin-1", Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, "agent-1", Map.of());
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        var command = new UpdatePlatformConfigCommand(nonAdminActor(), new BigDecimal("1000.00"), "test");
        assertThrows(ForbiddenOperationException.class, () -> service.execute(command));
        verifyNoInteractions(platformConfigPort, auditPort, timeProviderPort);
    }

    @Test
    void throwsValidationException_whenCashLimitIsNegative() {
        var command = new UpdatePlatformConfigCommand(adminActor(), new BigDecimal("-1.00"), "test");
        assertThrows(ValidationException.class, () -> service.execute(command));
        verifyNoInteractions(platformConfigPort, auditPort, timeProviderPort);
    }

    @Test
    void happyPath_updatesConfig_andWritesAudit() {
        when(platformConfigPort.get()).thenReturn(Optional.of(new PlatformConfig(new BigDecimal("500.00"))));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdatePlatformConfigResult result = service.execute(
                new UpdatePlatformConfigCommand(adminActor(), new BigDecimal("1000.00"), "ops update")
        );

        assertEquals(new BigDecimal("1000.00"), result.agentCashLimitGlobal());
        verify(platformConfigPort).upsert(any(PlatformConfig.class));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        AuditEvent event = auditCaptor.getValue();
        assertEquals("ADMIN_UPDATE_PLATFORM_CONFIG", event.action());
        assertEquals("500.00", event.metadata().get("previousAgentCashLimitGlobal"));
        assertEquals("1000.00", event.metadata().get("agentCashLimitGlobal"));
    }
}

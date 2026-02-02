package com.kori.application.usecase;

import com.kori.application.command.UpdateFeeConfigCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.FeeConfigPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateFeeConfigResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.config.FeeConfig;
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
final class UpdateFeeConfigServiceTest {

    @Mock FeeConfigPort feeConfigPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;

    @InjectMocks UpdateFeeConfigService service;

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, "admin-1", Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, "agent-1", Map.of());
    }

    private UpdateFeeConfigCommand command(ActorContext actor, String reason) {
        return new UpdateFeeConfigCommand(
                actor,
                new BigDecimal("10.00"),
                new BigDecimal("0.020000"),
                new BigDecimal("1.00"),
                new BigDecimal("5.00"),
                new BigDecimal("0.030000"),
                new BigDecimal("1.50"),
                new BigDecimal("6.00"),
                reason
        );
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(command(nonAdminActor(), "test"))
        );

        verifyNoInteractions(feeConfigPort, auditPort, timeProviderPort);
    }

    @Test
    void throwsValidationException_whenRateIsOutOfRange() {
        UpdateFeeConfigCommand cmd = new UpdateFeeConfigCommand(
                adminActor(),
                new BigDecimal("10.00"),
                new BigDecimal("1.200000"),
                new BigDecimal("1.00"),
                new BigDecimal("5.00"),
                new BigDecimal("0.030000"),
                new BigDecimal("1.50"),
                new BigDecimal("6.00"),
                "test"
        );

        assertThrows(ValidationException.class, () -> service.execute(cmd));
        verifyNoInteractions(feeConfigPort, auditPort, timeProviderPort);
    }

    @Test
    void happyPath_updatesConfig_andWritesAudit() {
        FeeConfig previous = new FeeConfig(
                new BigDecimal("9.00"),
                new BigDecimal("0.010000"),
                new BigDecimal("0.50"),
                new BigDecimal("4.00"),
                new BigDecimal("0.020000"),
                new BigDecimal("1.00"),
                new BigDecimal("5.00")
        );

        when(feeConfigPort.get()).thenReturn(Optional.of(previous));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateFeeConfigResult result = service.execute(command(adminActor(), "ops update"));

        assertEquals(new BigDecimal("10.00"), result.cardEnrollmentPrice());
        verify(feeConfigPort).upsert(any(FeeConfig.class));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        AuditEvent event = auditCaptor.getValue();
        assertEquals("ADMIN_UPDATE_FEE_CONFIG", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals("admin-1", event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals("ops update", event.metadata().get("reason"));
        assertEquals("9.00", event.metadata().get("previousCardEnrollmentPrice"));
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        when(feeConfigPort.get()).thenReturn(Optional.empty());
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(command(adminActor(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));
    }
}

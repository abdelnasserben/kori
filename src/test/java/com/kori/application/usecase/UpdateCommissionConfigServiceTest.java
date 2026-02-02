package com.kori.application.usecase;

import com.kori.application.command.UpdateCommissionConfigCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CommissionConfigPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateCommissionConfigResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.config.CommissionConfig;
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
final class UpdateCommissionConfigServiceTest {

    @Mock CommissionConfigPort commissionConfigPort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;

    @InjectMocks UpdateCommissionConfigService service;

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, "admin-1", Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, "agent-1", Map.of());
    }

    private UpdateCommissionConfigCommand command(ActorContext actor, String reason) {
        return new UpdateCommissionConfigCommand(
                actor,
                new BigDecimal("3.00"),
                new BigDecimal("0.500000"),
                new BigDecimal("0.50"),
                new BigDecimal("2.00"),
                reason
        );
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () ->
                service.execute(command(nonAdminActor(), "test"))
        );

        verifyNoInteractions(commissionConfigPort, auditPort, timeProviderPort);
    }

    @Test
    void throwsValidationException_whenRateIsOutOfRange() {
        UpdateCommissionConfigCommand cmd = new UpdateCommissionConfigCommand(
                adminActor(),
                new BigDecimal("3.00"),
                new BigDecimal("1.200000"),
                new BigDecimal("0.50"),
                new BigDecimal("2.00"),
                "test"
        );

        assertThrows(ValidationException.class, () -> service.execute(cmd));
        verifyNoInteractions(commissionConfigPort, auditPort, timeProviderPort);
    }

    @Test
    void happyPath_updatesConfig_andWritesAudit() {
        CommissionConfig previous = new CommissionConfig(
                new BigDecimal("2.00"),
                new BigDecimal("0.400000"),
                new BigDecimal("0.20"),
                new BigDecimal("1.50")
        );

        when(commissionConfigPort.get()).thenReturn(Optional.of(previous));
        when(timeProviderPort.now()).thenReturn(NOW);

        UpdateCommissionConfigResult result = service.execute(command(adminActor(), "ops update"));

        assertEquals(new BigDecimal("3.00"), result.cardEnrollmentAgentCommission());
        verify(commissionConfigPort).upsert(any(CommissionConfig.class));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        AuditEvent event = auditCaptor.getValue();
        assertEquals("ADMIN_UPDATE_COMMISSION_CONFIG", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals("admin-1", event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals("ops update", event.metadata().get("reason"));
        assertEquals("2.00", event.metadata().get("previousCardEnrollmentAgentCommission"));
    }

    @Test
    void reasonDefaultsToNA_whenBlank() {
        when(commissionConfigPort.get()).thenReturn(Optional.empty());
        when(timeProviderPort.now()).thenReturn(NOW);

        service.execute(command(adminActor(), "   "));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        assertEquals("N/A", auditCaptor.getValue().metadata().get("reason"));
    }
}

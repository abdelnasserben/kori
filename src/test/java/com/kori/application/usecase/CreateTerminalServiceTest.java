package com.kori.application.usecase;

import com.kori.application.command.CreateTerminalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateTerminalResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class CreateTerminalServiceTest {

    // ======= mocks =======
    @Mock TerminalRepositoryPort terminalRepositoryPort;
    @Mock MerchantRepositoryPort merchantRepositoryPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock TimeProviderPort timeProviderPort;
    @Mock IdGeneratorPort idGeneratorPort;
    @Mock AuditPort auditPort;

    @InjectMocks CreateTerminalService service;

    // ======= constants =======
    private static final String IDEM_KEY = "idem-1";
    private static final String REQUEST_HASH = "request-hash";
    private static final String ADMIN_ID = "admin-actor";
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final UUID MERCHANT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final MerchantId MERCHANT_ID = new MerchantId(MERCHANT_UUID);

    private static final String MERCHANT_CODE_RAW = "M-123456";
    private static final MerchantCode MERCHANT_CODE = MerchantCode.of(MERCHANT_CODE_RAW);

    private static final UUID TERMINAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final TerminalId TERMINAL_ID = new TerminalId(TERMINAL_UUID);

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, "agent-actor", Map.of());
    }

    private static CreateTerminalCommand cmd(ActorContext actor) {
        return new CreateTerminalCommand(IDEM_KEY, REQUEST_HASH, actor, MERCHANT_CODE_RAW);
    }

    private static Merchant activeMerchant() {
        return new Merchant(MERCHANT_ID, MERCHANT_CODE, Status.ACTIVE, NOW.minusSeconds(120));
    }

    @BeforeEach
    void setUp() {
        lenient().when(idempotencyPort.reserve(anyString(), anyString(), any())).thenReturn(true);
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(nonAdminActor())));

        // actor check happens before idempotency
        verifyNoInteractions(idempotencyPort, merchantRepositoryPort, terminalRepositoryPort, timeProviderPort, idGeneratorPort, auditPort);
    }

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        CreateTerminalResult cached = new CreateTerminalResult("t-1", MERCHANT_CODE_RAW);
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CreateTerminalResult.class)).thenReturn(Optional.of(cached));

        CreateTerminalResult out = service.execute(cmd(adminActor()));

        assertSame(cached, out);
        verify(idempotencyPort).find(IDEM_KEY, REQUEST_HASH, CreateTerminalResult.class);

        verifyNoMoreInteractions(
                merchantRepositoryPort,
                terminalRepositoryPort,
                timeProviderPort,
                idGeneratorPort,
                auditPort,
                idempotencyPort
        );
    }

    @Test
    void throwsNotFound_whenMerchantDoesNotExist() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CreateTerminalResult.class)).thenReturn(Optional.empty());
        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.execute(cmd(adminActor())));

        verify(merchantRepositoryPort).findByCode(MERCHANT_CODE);
        verifyNoInteractions(terminalRepositoryPort, timeProviderPort, idGeneratorPort, auditPort);
        verify(idempotencyPort, never()).save(anyString(), anyString(), any());
    }

    @Test
    void forbidden_whenMerchantIsNotActive() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CreateTerminalResult.class)).thenReturn(Optional.empty());

        Merchant inactive = new Merchant(MERCHANT_ID, MERCHANT_CODE, Status.SUSPENDED, NOW.minusSeconds(120));
        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(inactive));

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(adminActor())));

        verifyNoInteractions(terminalRepositoryPort, timeProviderPort, idGeneratorPort, auditPort);
        verify(idempotencyPort, never()).save(anyString(), anyString(), any());
    }

    @Test
    void happyPath_createsTerminal_audits_andSavesIdempotency() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, CreateTerminalResult.class)).thenReturn(Optional.empty());
        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(activeMerchant()));
        when(timeProviderPort.now()).thenReturn(NOW);
        when(idGeneratorPort.newUuid()).thenReturn(TERMINAL_UUID);

        CreateTerminalResult out = service.execute(cmd(adminActor()));

        assertEquals(TERMINAL_UUID.toString(), out.terminalId());
        // result uses command.merchantCode (string)
        assertEquals(MERCHANT_CODE_RAW, out.merchantCode());

        // Terminal saved
        ArgumentCaptor<Terminal> terminalCaptor = ArgumentCaptor.forClass(Terminal.class);
        verify(terminalRepositoryPort).save(terminalCaptor.capture());
        Terminal saved = terminalCaptor.getValue();

        assertEquals(TERMINAL_ID, saved.id());
        assertEquals(MERCHANT_ID, saved.merchantId());
        assertEquals(Status.ACTIVE, saved.status());
        assertEquals(NOW, saved.createdAt());

        // Audit
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("TERMINAL_CREATED", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        assertEquals(ADMIN_ID, event.metadata().get("adminId"));
        assertEquals(TERMINAL_UUID.toString(), event.metadata().get("terminalId"));
        assertEquals(MERCHANT_CODE_RAW, event.metadata().get("merchantCode"));

        verify(idempotencyPort).save(eq(IDEM_KEY), eq(REQUEST_HASH), any(CreateTerminalResult.class));
    }
}

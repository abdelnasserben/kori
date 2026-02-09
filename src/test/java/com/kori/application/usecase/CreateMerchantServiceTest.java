package com.kori.application.usecase;

import com.kori.application.command.CreateMerchantCommand;
import com.kori.application.exception.ApplicationException;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.idempotency.IdempotencyClaim;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateMerchantResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;
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
final class CreateMerchantServiceTest {

    // ======= mocks =======
    @Mock MerchantRepositoryPort merchantRepositoryPort;
    @Mock AccountProfilePort accountProfilePort;
    @Mock AuditPort auditPort;
    @Mock TimeProviderPort timeProviderPort;
    @Mock
    IdempotencyPort idempotencyPort;
    @Mock CodeGeneratorPort codeGeneratorPort;
    @Mock IdGeneratorPort idGeneratorPort;

    @InjectMocks CreateMerchantService service;

    // ======= constants =======
    private static final String IDEM_KEY = "idem-1";
    private static final String REQUEST_HASH = "request-hash";
    private static final String ADMIN_ID = "admin-actor";
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final UUID MERCHANT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final MerchantId MERCHANT_ID = new MerchantId(MERCHANT_UUID);

    private static final String DIGITS_1 = "123456";
    private static final String DIGITS_2 = "654321";
    private static final String MERCHANT_CODE_1_RAW = "M-123456";
    private static final String MERCHANT_CODE_2_RAW = "M-654321";
    private static final MerchantCode MERCHANT_CODE_1 = MerchantCode.of(MERCHANT_CODE_1_RAW);
    private static final MerchantCode MERCHANT_CODE_2 = MerchantCode.of(MERCHANT_CODE_2_RAW);

    // ======= helpers =======
    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ID, Map.of());
    }

    private static ActorContext nonAdminActor() {
        return new ActorContext(ActorType.AGENT, "agent-actor", Map.of());
    }

    private static CreateMerchantCommand cmd(ActorContext actor) {
        return new CreateMerchantCommand(IDEM_KEY, REQUEST_HASH, actor);
    }

    @Test
    void forbidden_whenActorIsNotAdmin() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, CreateMerchantResult.class))
                .thenReturn(IdempotencyClaim.claimed());

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(nonAdminActor())));

        // actor check happens before idempotency
        verify(idempotencyPort).fail(IDEM_KEY, REQUEST_HASH);
        verifyNoInteractions(
                merchantRepositoryPort,
                accountProfilePort,
                auditPort,
                timeProviderPort,
                codeGeneratorPort,
                idGeneratorPort
        );
    }

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        CreateMerchantResult cached = new CreateMerchantResult("m-1", "M-000001");
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, CreateMerchantResult.class)).thenReturn(IdempotencyClaim.completed(cached));

        CreateMerchantResult out = service.execute(cmd(adminActor()));

        assertSame(cached, out);
        verify(idempotencyPort).claimOrLoad(IDEM_KEY, REQUEST_HASH, CreateMerchantResult.class);

        verifyNoMoreInteractions(
                merchantRepositoryPort,
                accountProfilePort,
                auditPort,
                timeProviderPort,
                codeGeneratorPort,
                idGeneratorPort,
                idempotencyPort
        );
    }

    @Test
    void happyPath_createsMerchantAndAccountProfile_audits_andSavesIdempotency() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, CreateMerchantResult.class)).thenReturn(IdempotencyClaim.claimed());

        when(codeGeneratorPort.next6Digits()).thenReturn(DIGITS_1);
        when(merchantRepositoryPort.existsByCode(MERCHANT_CODE_1)).thenReturn(false);

        when(idGeneratorPort.newUuid()).thenReturn(MERCHANT_UUID);
        when(timeProviderPort.now()).thenReturn(NOW);

        LedgerAccountRef merchantAcc = LedgerAccountRef.merchant(MERCHANT_UUID.toString());
        when(accountProfilePort.findByAccount(merchantAcc)).thenReturn(Optional.empty());

        CreateMerchantResult out = service.execute(cmd(adminActor()));

        assertEquals(MERCHANT_UUID.toString(), out.merchantId());
        assertEquals(MERCHANT_CODE_1_RAW, out.code());

        // merchant saved
        ArgumentCaptor<Merchant> merchantCaptor = ArgumentCaptor.forClass(Merchant.class);
        verify(merchantRepositoryPort).save(merchantCaptor.capture());
        Merchant savedMerchant = merchantCaptor.getValue();

        assertEquals(MERCHANT_ID, savedMerchant.id());
        assertEquals(MERCHANT_CODE_1, savedMerchant.code());
        assertEquals(Status.ACTIVE, savedMerchant.status());
        assertEquals(NOW, savedMerchant.createdAt());

        // profile saved
        ArgumentCaptor<AccountProfile> profileCaptor = ArgumentCaptor.forClass(AccountProfile.class);
        verify(accountProfilePort).save(profileCaptor.capture());
        AccountProfile savedProfile = profileCaptor.getValue();

        assertEquals(merchantAcc, savedProfile.account());
        assertEquals(Status.ACTIVE, savedProfile.status());
        assertEquals(NOW, savedProfile.createdAt());

        // audit
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());
        AuditEvent event = auditCaptor.getValue();

        assertEquals("MERCHANT_CREATED", event.action());
        assertEquals("ADMIN", event.actorType());
        assertEquals(ADMIN_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals(ADMIN_ID, event.metadata().get("adminId"));
        assertEquals(MERCHANT_CODE_1_RAW, event.metadata().get("merchantCode"));

        // idempotency saved
        verify(idempotencyPort).complete(eq(IDEM_KEY), eq(REQUEST_HASH), any(CreateMerchantResult.class));
    }

    @Test
    void codeGeneration_retriesOnCollision_thenSucceeds() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, CreateMerchantResult.class)).thenReturn(IdempotencyClaim.claimed());

        when(codeGeneratorPort.next6Digits()).thenReturn(DIGITS_1, DIGITS_2);
        when(merchantRepositoryPort.existsByCode(MERCHANT_CODE_1)).thenReturn(true);
        when(merchantRepositoryPort.existsByCode(MERCHANT_CODE_2)).thenReturn(false);

        when(idGeneratorPort.newUuid()).thenReturn(MERCHANT_UUID);
        when(timeProviderPort.now()).thenReturn(NOW);

        LedgerAccountRef merchantAcc = LedgerAccountRef.merchant(MERCHANT_UUID.toString());
        when(accountProfilePort.findByAccount(merchantAcc)).thenReturn(Optional.empty());

        CreateMerchantResult out = service.execute(cmd(adminActor()));

        assertEquals(MERCHANT_CODE_2_RAW, out.code());
        verify(codeGeneratorPort, times(2)).next6Digits();
        verify(merchantRepositoryPort).existsByCode(MERCHANT_CODE_1);
        verify(merchantRepositoryPort).existsByCode(MERCHANT_CODE_2);
    }

    @Test
    void throwsApplicationException_whenCannotGenerateUniqueCode_afterMaxAttempts() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, CreateMerchantResult.class)).thenReturn(IdempotencyClaim.claimed());

        when(codeGeneratorPort.next6Digits()).thenReturn("000001");
        when(merchantRepositoryPort.existsByCode(any(MerchantCode.class))).thenReturn(true);

        assertThrows(ApplicationException.class, () -> service.execute(cmd(adminActor())));

        verify(codeGeneratorPort, times(20)).next6Digits();
        verify(merchantRepositoryPort, times(20)).existsByCode(any(MerchantCode.class));

        verifyNoInteractions(idGeneratorPort, timeProviderPort, accountProfilePort, auditPort);
        verify(idempotencyPort, never()).complete(anyString(), anyString(), any());
    }

    @Test
    void forbidden_whenAccountProfileAlreadyExists() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, CreateMerchantResult.class)).thenReturn(IdempotencyClaim.claimed());

        when(codeGeneratorPort.next6Digits()).thenReturn(DIGITS_1);
        when(merchantRepositoryPort.existsByCode(MERCHANT_CODE_1)).thenReturn(false);

        when(idGeneratorPort.newUuid()).thenReturn(MERCHANT_UUID);
        when(timeProviderPort.now()).thenReturn(NOW);

        LedgerAccountRef merchantAcc = LedgerAccountRef.merchant(MERCHANT_UUID.toString());
        when(accountProfilePort.findByAccount(merchantAcc)).thenReturn(Optional.of(mock(AccountProfile.class)));

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(adminActor())));

        // per service: merchant is saved BEFORE checking existing profile
        verify(merchantRepositoryPort).save(any(Merchant.class));

        verify(accountProfilePort, never()).save(any(AccountProfile.class));
        verify(idempotencyPort, never()).complete(anyString(), anyString(), any());
        verifyNoInteractions(auditPort);
    }
}

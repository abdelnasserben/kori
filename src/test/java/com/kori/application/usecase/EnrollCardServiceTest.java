package com.kori.application.usecase;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.out.*;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.card.HashedPin;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class EnrollCardServiceTest {

    // ======= mocks =======
    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock IdGeneratorPort idGeneratorPort;

    @Mock ClientRepositoryPort clientRepositoryPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock AccountProfilePort accountProfilePort;

    @Mock FeePolicyPort feePolicyPort;
    @Mock CommissionPolicyPort commissionPolicyPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;
    @Mock PinHasherPort pinHasherPort;

    @InjectMocks EnrollCardService enrollCardService;

    // ======= constants (single source of truth) =======
    private static final String IDEM_KEY = "idem-1";
    private static final String ACTOR_ID = "agent-actor";
    private static final String ADMIN_ACTOR_ID = "admin-actor";

    private static final String CLIENT_PHONE = "+26912345678";
    private static final String CARD_UID = "CARD-001";
    private static final String RAW_PIN = "1234";

    // IMPORTANT: must match A-XXXXXX
    private static final String AGENT_CODE_RAW = "A-123456";
    private static final AgentCode AGENT_CODE = AgentCode.of(AGENT_CODE_RAW);

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");
    private static final UUID CLIENT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TX_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID AGENT_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private static final Money CARD_PRICE = Money.of(new BigDecimal("100.00"));
    private static final Money AGENT_COMMISSION = Money.of(new BigDecimal("20.00"));
    private static final Money PLATFORM_REVENUE = Money.of(new BigDecimal("80.00"));

    // ======= helpers =======
    private static ActorContext agentActor() {
        return new ActorContext(ActorType.AGENT, ACTOR_ID, Map.of());
    }

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, ADMIN_ACTOR_ID, Map.of());
    }

    private static EnrollCardCommand cmd(ActorContext actor) {
        return new EnrollCardCommand(
                IDEM_KEY,
                actor,
                CLIENT_PHONE,
                CARD_UID,
                RAW_PIN,
                AGENT_CODE_RAW
        );
    }

    private static Agent activeAgent() {
        return new Agent(new AgentId(AGENT_UUID), AGENT_CODE, NOW.minusSeconds(60), Status.ACTIVE);
    }

    private static AccountProfile activeAccount(LedgerAccountRef account) {
        return new AccountProfile(account, NOW.minusSeconds(10), Status.ACTIVE);
    }

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        EnrollCardResult cached = new EnrollCardResult(
                "tx-1",
                CLIENT_PHONE,
                CARD_UID,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                false,
                false
        );

        when(idempotencyPort.find(IDEM_KEY, EnrollCardResult.class)).thenReturn(Optional.of(cached));

        EnrollCardResult out = enrollCardService.execute(cmd(agentActor()));

        assertSame(cached, out);
        verify(idempotencyPort).find(IDEM_KEY, EnrollCardResult.class);

        verifyNoMoreInteractions(
                timeProviderPort,
                idGeneratorPort,
                clientRepositoryPort,
                cardRepositoryPort,
                agentRepositoryPort,
                transactionRepositoryPort,
                accountProfilePort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerAppendPort,
                auditPort,
                pinHasherPort,
                idempotencyPort
        );
    }

    @Test
    void forbidden_whenActorIsNotAgent() {
        when(idempotencyPort.find(IDEM_KEY, EnrollCardResult.class)).thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> enrollCardService.execute(cmd(adminActor())));

        verify(idempotencyPort).find(IDEM_KEY, EnrollCardResult.class);
        verifyNoMoreInteractions(idempotencyPort);

        verifyNoInteractions(
                timeProviderPort,
                idGeneratorPort,
                clientRepositoryPort,
                cardRepositoryPort,
                agentRepositoryPort,
                transactionRepositoryPort,
                accountProfilePort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerAppendPort,
                auditPort,
                pinHasherPort
        );
    }

    @Test
    void throwsNotFound_whenAgentDoesNotExist() {
        when(idempotencyPort.find(IDEM_KEY, EnrollCardResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> enrollCardService.execute(cmd(agentActor())));

        verify(idempotencyPort).find(IDEM_KEY, EnrollCardResult.class);
        verify(agentRepositoryPort).findByCode(AGENT_CODE);
        verifyNoMoreInteractions(idempotencyPort, agentRepositoryPort);

        verifyNoInteractions(
                timeProviderPort,
                idGeneratorPort,
                clientRepositoryPort,
                cardRepositoryPort,
                transactionRepositoryPort,
                accountProfilePort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerAppendPort,
                auditPort,
                pinHasherPort
        );
    }

    @Test
    void happyPath_createsClientAndAccountProfile_postsLedger_andAudits() {
        when(idempotencyPort.find(IDEM_KEY, EnrollCardResult.class)).thenReturn(Optional.empty());
        when(timeProviderPort.now()).thenReturn(NOW);
        when(idGeneratorPort.newUuid()).thenReturn(CLIENT_UUID, TX_UUID);

        Agent agent = activeAgent();
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));

        LedgerAccountRef agentAccount = LedgerAccountRef.agent(agent.id().value().toString());
        when(accountProfilePort.findByAccount(agentAccount)).thenReturn(Optional.of(activeAccount(agentAccount)));

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.empty());

        when(clientRepositoryPort.findByPhoneNumber(CLIENT_PHONE)).thenReturn(Optional.empty());
        when(clientRepositoryPort.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        LedgerAccountRef clientAccount = LedgerAccountRef.client(CLIENT_UUID.toString());
        when(accountProfilePort.findByAccount(clientAccount)).thenReturn(Optional.empty());
        doNothing().when(accountProfilePort).save(any(AccountProfile.class));

        when(pinHasherPort.hash(RAW_PIN)).thenReturn(new HashedPin("HASHED"));
        when(cardRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(feePolicyPort.cardEnrollmentPrice()).thenReturn(CARD_PRICE);
        when(commissionPolicyPort.cardEnrollmentAgentCommission()).thenReturn(AGENT_COMMISSION);

        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        EnrollCardResult out = enrollCardService.execute(cmd(agentActor()));

        assertEquals(TX_UUID.toString(), out.transactionId());
        assertEquals(CLIENT_PHONE, out.clientPhoneNumber());
        assertEquals(CARD_UID, out.cardUid());
        assertEquals(CARD_PRICE.asBigDecimal(), out.cardPrice());
        assertEquals(AGENT_COMMISSION.asBigDecimal(), out.agentCommission());
        assertTrue(out.clientCreated());
        assertTrue(out.accountCreated());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(ledgerCaptor.capture());

        List<LedgerEntry> entries = ledgerCaptor.getValue();
        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(e -> e.type() == LedgerEntryType.CREDIT));

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(new TransactionId(TX_UUID))
                        && e.accountRef().equals(agentAccount)
                        && e.amount().equals(AGENT_COMMISSION)
        ));

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(new TransactionId(TX_UUID))
                        && e.accountRef().equals(LedgerAccountRef.platformFeeRevenue())
                        && e.amount().equals(PLATFORM_REVENUE)
        ));

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        AuditEvent event = auditCaptor.getValue();
        assertEquals("ENROLL_CARD", event.action());
        assertEquals("AGENT", event.actorType());
        assertEquals(ACTOR_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());
        assertEquals(TX_UUID.toString(), event.metadata().get("transactionId"));
        assertEquals(AGENT_CODE_RAW, event.metadata().get("agentCode"));
        assertEquals(CLIENT_PHONE, event.metadata().get("clientPhoneNumber"));
        assertEquals(CARD_UID, event.metadata().get("cardUid"));

        verify(idempotencyPort).save(eq(IDEM_KEY), any(EnrollCardResult.class));
    }

    @Test
    void forbidden_whenCardUidAlreadyEnrolled() {
        when(idempotencyPort.find(IDEM_KEY, EnrollCardResult.class)).thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));

        LedgerAccountRef agentAccount = LedgerAccountRef.agent(agent.id().value().toString());
        when(accountProfilePort.findByAccount(agentAccount)).thenReturn(Optional.of(activeAccount(agentAccount)));

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.of(mock(Card.class)));

        assertThrows(ForbiddenOperationException.class, () -> enrollCardService.execute(cmd(agentActor())));

        verify(idempotencyPort).find(IDEM_KEY, EnrollCardResult.class);
        verify(agentRepositoryPort).findByCode(AGENT_CODE);
        verify(accountProfilePort).findByAccount(agentAccount);
        verify(cardRepositoryPort).findByCardUid(CARD_UID);
        verifyNoMoreInteractions(idempotencyPort, agentRepositoryPort, accountProfilePort, cardRepositoryPort);

        verifyNoInteractions(
                timeProviderPort,
                idGeneratorPort,
                clientRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerAppendPort,
                auditPort,
                pinHasherPort
        );
    }

    @Test
    void forbidden_whenClientAccountProfileIsNotActive() {
        when(idempotencyPort.find(IDEM_KEY, EnrollCardResult.class)).thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));

        LedgerAccountRef agentAccount = LedgerAccountRef.agent(agent.id().value().toString());
        when(accountProfilePort.findByAccount(agentAccount)).thenReturn(Optional.of(activeAccount(agentAccount)));

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.empty());

        Client client = new Client(new ClientId(CLIENT_UUID), CLIENT_PHONE, Status.ACTIVE);
        when(clientRepositoryPort.findByPhoneNumber(CLIENT_PHONE)).thenReturn(Optional.of(client));

        LedgerAccountRef clientAccount = LedgerAccountRef.client(CLIENT_UUID.toString());
        when(accountProfilePort.findByAccount(clientAccount))
                .thenReturn(Optional.of(new AccountProfile(clientAccount, NOW.minusSeconds(10), Status.SUSPENDED)));

        assertThrows(ForbiddenOperationException.class, () -> enrollCardService.execute(cmd(agentActor())));
    }
}

package com.kori.application.usecase;

import com.kori.application.command.AddCardToExistingClientCommand;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.idempotency.IdempotencyClaim;
import com.kori.application.port.out.*;
import com.kori.application.result.AddCardToExistingClientResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.card.HashedPin;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.config.PlatformConfig;
import com.kori.domain.model.transaction.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class AddCardToExistingClientServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock
    IdempotencyPort idempotencyPort;
    @Mock IdGeneratorPort idGeneratorPort;
    @Mock ClientRepositoryPort clientRepositoryPort;
    @Mock CardRepositoryPort cardRepositoryPort;
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock FeePolicyPort feePolicyPort;
    @Mock CommissionPolicyPort commissionPolicyPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock PlatformConfigPort platformConfigPort;
    @Mock AuditPort auditPort;
    @Mock PinHasherPort pinHasherPort;
    @Mock OperationStatusGuards operationStatusGuards;

    @InjectMocks AddCardToExistingClientService service;

    private static final String IDEM_KEY = "idem-1";
    private static final String REQUEST_HASH = "request-hash";
    private static final String ACTOR_ID = "agent-actor";
    private static final String PHONE = "+2691234567";
    private static final String CARD_UID = "CARD-001";
    private static final String RAW_PIN = "1234";
    private static final String AGENT_CODE_RAW = "A-123456";
    private static final AgentCode AGENT_CODE = AgentCode.of(AGENT_CODE_RAW);
    private static final UUID CLIENT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AGENT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TX_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");
    private static final Money CARD_PRICE = Money.of(new BigDecimal("100.00"));
    private static final Money AGENT_COMMISSION = Money.of(new BigDecimal("20.00"));

    private static ActorContext agentActor() {
        return new ActorContext(ActorType.AGENT, ACTOR_ID, Map.of());
    }

    private static AddCardToExistingClientCommand cmd() {
        return new AddCardToExistingClientCommand(
                IDEM_KEY,
                REQUEST_HASH,
                agentActor(),
                PHONE,
                CARD_UID,
                RAW_PIN,
                AGENT_CODE_RAW
        );
    }

    @Test
    void returns_cached_result_when_idempotency_key_exists() {
        var cached = new AddCardToExistingClientResult("tx-1", CLIENT_UUID.toString(), CARD_UID, BigDecimal.ONE, BigDecimal.ZERO);
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, AddCardToExistingClientResult.class))
                .thenReturn(IdempotencyClaim.completed(cached));

        var result = service.execute(cmd());

        assertSame(cached, result);
        verify(idempotencyPort).claimOrLoad(IDEM_KEY, REQUEST_HASH, AddCardToExistingClientResult.class);
        verifyNoMoreInteractions(idempotencyPort);
        verifyNoInteractions(
                timeProviderPort,
                idGeneratorPort,
                clientRepositoryPort,
                cardRepositoryPort,
                agentRepositoryPort,
                transactionRepositoryPort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerAppendPort,
                ledgerQueryPort,
                platformConfigPort,
                auditPort,
                pinHasherPort,
                operationStatusGuards
        );
    }

    @Test
    void happy_path_adds_card_for_existing_client() {
        when(idempotencyPort.claimOrLoad(IDEM_KEY, REQUEST_HASH, AddCardToExistingClientResult.class)).thenReturn(IdempotencyClaim.claimed());
        when(timeProviderPort.now()).thenReturn(NOW);

        Agent agent = new Agent(new AgentId(AGENT_UUID), AGENT_CODE, NOW.minusSeconds(60), Status.ACTIVE);
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        doNothing().when(operationStatusGuards).requireActiveAgent(agent);

        Client client = new Client(new ClientId(CLIENT_UUID), PHONE, Status.ACTIVE, NOW.minusSeconds(120));
        when(clientRepositoryPort.findByPhoneNumber(PHONE)).thenReturn(Optional.of(client));
        doNothing().when(operationStatusGuards).requireActiveClient(client);

        when(cardRepositoryPort.findByCardUid(CARD_UID)).thenReturn(Optional.empty());
        when(cardRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pinHasherPort.hash(RAW_PIN)).thenReturn(new HashedPin("hashed"));

        when(feePolicyPort.cardEnrollmentPrice()).thenReturn(CARD_PRICE);
        when(commissionPolicyPort.cardEnrollmentAgentCommission()).thenReturn(AGENT_COMMISSION);

        when(idGeneratorPort.newUuid()).thenReturn(TX_UUID);
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(agentRepositoryPort.findByIdForUpdate(agent.id())).thenReturn(Optional.of(agent));
        when(ledgerQueryPort.getBalance(any())).thenReturn(Money.zero());
        when(platformConfigPort.get()).thenReturn(Optional.of(new PlatformConfig(new BigDecimal("500.00"))));

        var result = service.execute(cmd());

        assertEquals(TX_UUID.toString(), result.transactionId());
        assertEquals(CLIENT_UUID.toString(), result.clientId());
        assertEquals(CARD_UID, result.cardUid());
        assertEquals(CARD_PRICE.asBigDecimal(), result.cardPrice());
        assertEquals(AGENT_COMMISSION.asBigDecimal(), result.agentCommission());

        verify(ledgerAppendPort).append(any(List.class));
        verify(auditPort).publish(any());
        verify(idempotencyPort).complete(eq(IDEM_KEY), eq(REQUEST_HASH), any(AddCardToExistingClientResult.class));
    }
}
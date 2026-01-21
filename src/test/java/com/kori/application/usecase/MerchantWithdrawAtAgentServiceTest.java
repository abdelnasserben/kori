package com.kori.application.usecase;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.*;
import com.kori.application.result.MerchantWithdrawAtAgentResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantId;
import com.kori.domain.model.transaction.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantWithdrawAtAgentServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock MerchantRepositoryPort merchantRepositoryPort;
    @Mock AgentRepositoryPort agentRepositoryPort;
    @Mock FeePolicyPort feePolicyPort;
    @Mock CommissionPolicyPort commissionPolicyPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;

    private MerchantWithdrawAtAgentService service;

    @BeforeEach
    void setUp() {
        service = new MerchantWithdrawAtAgentService(
                timeProviderPort, idempotencyPort,
                merchantRepositoryPort, agentRepositoryPort,
                feePolicyPort, commissionPolicyPort,
                transactionRepositoryPort, ledgerAppendPort, auditPort
        );
    }

    @Test
    void returnsCachedResultWhenIdempotencyHit() {
        MerchantWithdrawAtAgentResult cached = new MerchantWithdrawAtAgentResult(
                "tx1", "m1", "a1",
                BigDecimal.valueOf(100).setScale(2),
                BigDecimal.valueOf(5).setScale(2),
                BigDecimal.valueOf(2).setScale(2),
                BigDecimal.valueOf(105).setScale(2)
        );

        when(idempotencyPort.find("idem-1", MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.of(cached));

        MerchantWithdrawAtAgentCommand cmd = new MerchantWithdrawAtAgentCommand(
                "idem-1",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "m-1",
                "agent-1",
                BigDecimal.valueOf(100)
        );

        MerchantWithdrawAtAgentResult result = service.execute(cmd);

        assertSame(cached, result);
        verifyNoInteractions(merchantRepositoryPort, agentRepositoryPort, feePolicyPort,
                commissionPolicyPort, transactionRepositoryPort, ledgerAppendPort, auditPort);
    }

    @Test
    void withdraw_happyPath_appendsLedger_audits_andSavesIdempotency() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);

        when(idempotencyPort.find("idem-2", MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());

        when(agentRepositoryPort.existsById("agent-1")).thenReturn(true);
        when(merchantRepositoryPort.findById("m-1"))
                .thenReturn(Optional.of(new Merchant(MerchantId.of("m-1"), Status.ACTIVE)));

        Money fee = Money.of(BigDecimal.valueOf(5));
        Money commission = Money.of(BigDecimal.valueOf(2));
        when(feePolicyPort.merchantWithdrawFee(any(Money.class))).thenReturn(fee);
        when(commissionPolicyPort.merchantWithdrawAgentCommission(fee)).thenReturn(commission);

        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        MerchantWithdrawAtAgentCommand cmd = new MerchantWithdrawAtAgentCommand(
                "idem-2",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "m-1",
                "agent-1",
                BigDecimal.valueOf(100)
        );

        MerchantWithdrawAtAgentResult result = service.execute(cmd);

        assertNotNull(result.transactionId());
        assertEquals("m-1", result.merchantId());
        assertEquals("agent-1", result.agentId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), result.amount());
        assertEquals(BigDecimal.valueOf(5).setScale(2), result.fee());
        assertEquals(BigDecimal.valueOf(2).setScale(2), result.commission());
        assertEquals(BigDecimal.valueOf(105).setScale(2), result.totalDebitedMerchant());

        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(ledgerCaptor.capture());
        assertEquals(4, ledgerCaptor.getValue().size());

        verify(auditPort).publish(argThat(ev ->
                ev.action().equals("MERCHANT_WITHDRAW_AT_AGENT")
                        && ev.actorType().equals("AGENT")
                        && ev.actorId().equals("agent-actor")
                        && ev.occurredAt().equals(now)
                        && "m-1".equals(ev.metadata().get("merchantId"))
                        && "agent-1".equals(ev.metadata().get("agentId"))
        ));

        verify(idempotencyPort).save("idem-2", result);
    }

    @Test
    void withdraw_forbidden_whenCommissionExceedsFee() {
        Instant now = Instant.parse("2026-01-21T10:15:30Z");
        when(timeProviderPort.now()).thenReturn(now);

        when(idempotencyPort.find("idem-3", MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());
        when(agentRepositoryPort.existsById("agent-1")).thenReturn(true);
        when(merchantRepositoryPort.findById("m-1"))
                .thenReturn(Optional.of(new Merchant(MerchantId.of("m-1"), Status.ACTIVE)));

        Money fee = Money.of(BigDecimal.valueOf(5));
        Money commission = Money.of(BigDecimal.valueOf(6)); // > fee
        when(feePolicyPort.merchantWithdrawFee(any(Money.class))).thenReturn(fee);
        when(commissionPolicyPort.merchantWithdrawAgentCommission(fee)).thenReturn(commission);

        MerchantWithdrawAtAgentCommand cmd = new MerchantWithdrawAtAgentCommand(
                "idem-3",
                new ActorContext(ActorType.AGENT, "agent-actor", Map.of()),
                "m-1",
                "agent-1",
                BigDecimal.valueOf(100)
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }

    @Test
    void withdraw_forbidden_whenActorNotAgent() {
        when(idempotencyPort.find("idem-4", MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());

        MerchantWithdrawAtAgentCommand cmd = new MerchantWithdrawAtAgentCommand(
                "idem-4",
                new ActorContext(ActorType.ADMIN, "admin-1", Map.of()),
                "m-1",
                "agent-1",
                BigDecimal.valueOf(100)
        );

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }
}

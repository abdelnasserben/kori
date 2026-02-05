package com.kori.application.usecase;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.port.out.*;
import com.kori.application.result.MerchantWithdrawAtAgentResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;
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
final class MerchantWithdrawAtAgentServiceTest {

    // ======= mocks =======
    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock IdGeneratorPort idGeneratorPort;

    @Mock MerchantRepositoryPort merchantRepositoryPort;
    @Mock AgentRepositoryPort agentRepositoryPort;

    @Mock FeePolicyPort feePolicyPort;
    @Mock CommissionPolicyPort commissionPolicyPort;

    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock PlatformConfigPort platformConfigPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;

    @Mock OperationStatusGuards operationStatusGuards;

    @InjectMocks MerchantWithdrawAtAgentService service;

    // ======= constants (single source of truth) =======
    private static final String IDEM_KEY = "idem-1";
    private static final String REQUEST_HASH = "request-hash";
    private static final String ACTOR_ID = "agent-actor";

    // IMPORTANT formats
    private static final String AGENT_CODE_RAW = "A-123456";
    private static final AgentCode AGENT_CODE = AgentCode.of(AGENT_CODE_RAW);

    private static final String MERCHANT_CODE_RAW = "M-123456";
    private static final MerchantCode MERCHANT_CODE = MerchantCode.of(MERCHANT_CODE_RAW);

    private static final BigDecimal AMOUNT_BD = new BigDecimal("100.00");
    private static final Money AMOUNT = Money.positive(AMOUNT_BD);

    private static final Money FEE = Money.of(new BigDecimal("10.00"));
    private static final Money COMMISSION = Money.of(new BigDecimal("3.00"));
    private static final Money PLATFORM_REVENUE = Money.of(new BigDecimal("7.00"));
    private static final Money TOTAL_DEBITED_MERCHANT = Money.of(new BigDecimal("110.00"));

    private static final Instant NOW = Instant.parse("2026-01-28T10:15:30Z");

    private static final UUID AGENT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MERCHANT_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TX_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    // ======= helpers =======
    private static ActorContext agentActor() {
        return new ActorContext(ActorType.AGENT, ACTOR_ID, Map.of());
    }

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, "admin-actor", Map.of());
    }

    private static MerchantWithdrawAtAgentCommand cmd(ActorContext actor) {
        return new MerchantWithdrawAtAgentCommand(
                IDEM_KEY,
                REQUEST_HASH,
                actor,
                MERCHANT_CODE_RAW,
                AGENT_CODE_RAW,
                AMOUNT_BD
        );
    }

    private static Agent activeAgent() {
        return new Agent(new AgentId(AGENT_UUID), AGENT_CODE, NOW.minusSeconds(60), Status.ACTIVE);
    }

    private static Merchant activeMerchant() {
        return new Merchant(new MerchantId(MERCHANT_UUID), MERCHANT_CODE, Status.ACTIVE, NOW.minusSeconds(120));
    }

    // ======= tests =======

    @Test
    void returnsCachedResult_whenIdempotencyKeyAlreadyProcessed() {
        MerchantWithdrawAtAgentResult cached = new MerchantWithdrawAtAgentResult(
                "tx-1",
                MERCHANT_CODE_RAW,
                AGENT_CODE_RAW,
                AMOUNT_BD,
                new BigDecimal("10.00"),
                new BigDecimal("3.00"),
                new BigDecimal("110.00")
        );

        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.of(cached));

        MerchantWithdrawAtAgentResult out = service.execute(cmd(agentActor()));

        assertSame(cached, out);
        verify(idempotencyPort).find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class);

        verifyNoMoreInteractions(
                timeProviderPort,
                idGeneratorPort,
                merchantRepositoryPort,
                agentRepositoryPort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerQueryPort,
                transactionRepositoryPort,
                ledgerAppendPort,
                auditPort,
                operationStatusGuards,
                idempotencyPort
        );
    }

    @Test
    void forbidden_whenActorIsNotAgent() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(adminActor())));

        verify(idempotencyPort).find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class);
        verifyNoMoreInteractions(idempotencyPort);

        verifyNoInteractions(
                timeProviderPort,
                idGeneratorPort,
                merchantRepositoryPort,
                agentRepositoryPort,
                feePolicyPort,
                commissionPolicyPort,
                ledgerQueryPort,
                transactionRepositoryPort,
                ledgerAppendPort,
                auditPort,
                operationStatusGuards
        );
    }

    @Test
    void forbidden_whenAgentNotFound() {
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(agentActor())));

        verify(agentRepositoryPort).findByCode(AGENT_CODE);
        verifyNoInteractions(merchantRepositoryPort, feePolicyPort, ledgerAppendPort, auditPort, operationStatusGuards);
    }

    @Test
    void forbidden_whenMerchantNotFound() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        doNothing().when(operationStatusGuards).requireActiveAgent(agent);

        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.empty());

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(agentActor())));

        verify(merchantRepositoryPort).findByCode(MERCHANT_CODE);
        verifyNoInteractions(feePolicyPort, ledgerAppendPort, auditPort);
    }

    @Test
    void forbidden_whenAgentNotActive_orAgentAccountNotActive_isHandledByGuard() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));

        doThrow(new ForbiddenOperationException("AGENT_NOT_ACTIVE"))
                .when(operationStatusGuards).requireActiveAgent(agent);

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(agentActor())));

        verifyNoInteractions(merchantRepositoryPort, feePolicyPort, commissionPolicyPort, ledgerAppendPort, auditPort);
    }

    @Test
    void forbidden_whenMerchantNotActive_orMerchantAccountNotActive_isHandledByGuard() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        doNothing().when(operationStatusGuards).requireActiveAgent(agent);

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(merchant));

        doThrow(new ForbiddenOperationException("MERCHANT_ACCOUNT_NOT_ACTIVE"))
                .when(operationStatusGuards).requireActiveMerchant(merchant);

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(agentActor())));

        verifyNoInteractions(feePolicyPort, commissionPolicyPort, ledgerAppendPort, auditPort);
    }

    @Test
    void forbidden_whenCommissionExceedsFee() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        doNothing().when(operationStatusGuards).requireActiveAgent(agent);

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        when(timeProviderPort.now()).thenReturn(NOW);
        when(feePolicyPort.merchantWithdrawFee(AMOUNT)).thenReturn(Money.of(new BigDecimal("10.00")));
        when(commissionPolicyPort.merchantWithdrawAgentCommission(Money.of(new BigDecimal("10.00"))))
                .thenReturn(Money.of(new BigDecimal("11.00"))); // > fee -> forbidden via PricingGuards

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd(agentActor())));

        verifyNoInteractions(ledgerAppendPort, transactionRepositoryPort, auditPort);
    }

    @Test
    void insufficientFunds_whenMerchantBalanceTooLow() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        doNothing().when(operationStatusGuards).requireActiveAgent(agent);

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        LedgerAccountRef merchantAcc = LedgerAccountRef.merchant(merchant.id().value().toString());

        when(timeProviderPort.now()).thenReturn(NOW);
        when(feePolicyPort.merchantWithdrawFee(AMOUNT)).thenReturn(FEE);
        when(commissionPolicyPort.merchantWithdrawAgentCommission(FEE)).thenReturn(COMMISSION);

        when(ledgerQueryPort.netBalance(merchantAcc)).thenReturn(Money.of(new BigDecimal("1.00"))); // too low

        assertThrows(InsufficientFundsException.class, () -> service.execute(cmd(agentActor())));

        verifyNoInteractions(transactionRepositoryPort, ledgerAppendPort, auditPort);
    }

    @Test
    void happyPath_createsTx_postsLedger_audits_andSavesIdempotency() {
        when(idempotencyPort.find(IDEM_KEY, REQUEST_HASH, MerchantWithdrawAtAgentResult.class)).thenReturn(Optional.empty());

        Agent agent = activeAgent();
        when(agentRepositoryPort.findByCode(AGENT_CODE)).thenReturn(Optional.of(agent));
        doNothing().when(operationStatusGuards).requireActiveAgent(agent);

        Merchant merchant = activeMerchant();
        when(merchantRepositoryPort.findByCode(MERCHANT_CODE)).thenReturn(Optional.of(merchant));
        doNothing().when(operationStatusGuards).requireActiveMerchant(merchant);

        LedgerAccountRef agentAcc = LedgerAccountRef.agentWallet(agent.id().value().toString());
        LedgerAccountRef merchantAcc = LedgerAccountRef.merchant(merchant.id().value().toString());

        when(timeProviderPort.now()).thenReturn(NOW);

        when(feePolicyPort.merchantWithdrawFee(AMOUNT)).thenReturn(FEE);
        when(commissionPolicyPort.merchantWithdrawAgentCommission(FEE)).thenReturn(COMMISSION);

        when(ledgerQueryPort.netBalance(merchantAcc)).thenReturn(Money.of(new BigDecimal("999999.00")));
        when(agentRepositoryPort.findByIdForUpdate(agent.id())).thenReturn(Optional.of(agent));
        when(ledgerQueryPort.getBalance(LedgerAccountRef.agentCashClearing(agent.id().value().toString()))).thenReturn(Money.zero());
        when(platformConfigPort.get()).thenReturn(Optional.of(new com.kori.domain.model.config.PlatformConfig(new BigDecimal("1000.00"))));

        when(idGeneratorPort.newUuid()).thenReturn(TX_UUID);
        when(transactionRepositoryPort.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        MerchantWithdrawAtAgentResult out = service.execute(cmd(agentActor()));

        assertEquals(TX_UUID.toString(), out.transactionId());
        assertEquals(MERCHANT_CODE_RAW, out.merchantCode());
        assertEquals(AGENT_CODE_RAW, out.agentCode());
        assertEquals(AMOUNT.asBigDecimal(), out.amount());
        assertEquals(FEE.asBigDecimal(), out.fee());
        assertEquals(COMMISSION.asBigDecimal(), out.commission());
        assertEquals(TOTAL_DEBITED_MERCHANT.asBigDecimal(), out.totalDebitedMerchant());

        // tx saved
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(txCaptor.capture());
        Transaction saved = txCaptor.getValue();

        assertEquals(new TransactionId(TX_UUID), saved.id());
        assertEquals(AMOUNT, saved.amount());

        // ledger appended
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LedgerEntry>> ledgerCaptor = ArgumentCaptor.forClass(List.class);
        verify(ledgerAppendPort).append(ledgerCaptor.capture());

        List<LedgerEntry> entries = ledgerCaptor.getValue();
        assertEquals(4, entries.size());

        TransactionId txId = new TransactionId(TX_UUID);
        LedgerAccountRef clearingAcc = LedgerAccountRef.agentCashClearing(agent.id().value().toString());
        LedgerAccountRef feeAcc = LedgerAccountRef.platformFeeRevenue();

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(txId)
                        && e.type() == LedgerEntryType.DEBIT
                        && e.accountRef().equals(merchantAcc)
                        && e.amount().equals(TOTAL_DEBITED_MERCHANT)
        ));

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(txId)
                        && e.type() == LedgerEntryType.CREDIT
                        && e.accountRef().equals(clearingAcc)
                        && e.amount().equals(AMOUNT)
        ));

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(txId)
                        && e.type() == LedgerEntryType.CREDIT
                        && e.accountRef().equals(agentAcc)
                        && e.amount().equals(COMMISSION)
        ));

        assertTrue(entries.stream().anyMatch(e ->
                e.transactionId().equals(txId)
                        && e.type() == LedgerEntryType.CREDIT
                        && e.accountRef().equals(feeAcc)
                        && e.amount().equals(PLATFORM_REVENUE) // fee - commission
        ));

        // audit
        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditPort).publish(auditCaptor.capture());

        AuditEvent event = auditCaptor.getValue();
        assertEquals("MERCHANT_WITHDRAW_AT_AGENT", event.action());
        assertEquals("AGENT", event.actorType());
        assertEquals(ACTOR_ID, event.actorId());
        assertEquals(NOW, event.occurredAt());

        Map<String, String> md = event.metadata();
        assertEquals(TX_UUID.toString(), md.get("transactionId"));
        assertEquals(MERCHANT_CODE_RAW, md.get("merchantCode"));
        assertEquals(AGENT_CODE_RAW, md.get("agentCode"));

        verify(idempotencyPort).save(eq(IDEM_KEY), eq(REQUEST_HASH), any(MerchantWithdrawAtAgentResult.class));
    }
}

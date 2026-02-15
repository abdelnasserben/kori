package com.kori.it;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.result.MerchantWithdrawAtAgentResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MerchantWithdrawAtAgentServiceIT extends IntegrationTestBase {

    private static final String AGENT_CODE = "A-000111";
    private static final String MERCHANT_CODE = "M-000222";

    @Autowired
    MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;

    @Test
    void merchantWithdrawAtAgent_happyPath_persistsLedgerAndAudit() {
        Agent agent = createActiveAgent(AGENT_CODE);
        Merchant merchant = createActiveMerchant(MERCHANT_CODE);

        LedgerAccountRef merchantAccount = LedgerAccountRef.merchant(merchant.id().value().toString());
        seedLedgerCredit(merchantAccount, new BigDecimal("200.00"));

        MerchantWithdrawAtAgentResult result = merchantWithdrawAtAgentUseCase.execute(new MerchantWithdrawAtAgentCommand(
                "idem-withdraw-1",
                "request-hash",
                agentActor("A-000001"),
                MERCHANT_CODE,
                AGENT_CODE,
                new BigDecimal("100.00")
        ));

        assertNotNull(result.transactionId());

        List<LedgerEntry> entries = ledgerQueryPort.findByTransactionId(TransactionId.of(result.transactionId()));
        assertEquals(4, entries.size());

        LedgerAccountRef agentAccount = LedgerAccountRef.agentWallet(agent.id().value().toString());

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.DEBIT
                        && entry.accountRef().equals(merchantAccount)
                        && entry.amount().equals(Money.of(new BigDecimal("103.00")))
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(LedgerAccountRef.agentCashClearing(agent.id().value().toString()))
                        && entry.amount().equals(Money.of(new BigDecimal("100.00")))
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(agentAccount)
                        && entry.amount().equals(Money.of(new BigDecimal("1.50")))
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(LedgerAccountRef.platformFeeRevenue())
                        && entry.amount().equals(Money.of(new BigDecimal("1.50")))
        ));

        assertTrue(entries.stream().noneMatch(entry ->
                entry.type() == LedgerEntryType.DEBIT
                        && entry.accountRef().equals(LedgerAccountRef.agentWallet(agent.id().value().toString()))
        ));

        assertTrue(entries.stream().noneMatch(entry ->
                entry.accountRef().equals(LedgerAccountRef.platformClearing())
        ));

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("MERCHANT_WITHDRAW_AT_AGENT"))
        );
    }
}

package com.kori.it;

import com.kori.application.command.MerchantWithdrawAtAgentCommand;
import com.kori.application.command.PayByCardCommand;
import com.kori.application.port.in.MerchantWithdrawAtAgentUseCase;
import com.kori.application.port.in.PayByCardUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.merchant.Merchant;
import com.kori.query.model.BackofficeAuditEventQuery;
import com.kori.query.model.BackofficeTransactionQuery;
import com.kori.query.port.out.BackofficeAuditEventReadPort;
import com.kori.query.port.out.BackofficeTransactionReadPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BackofficeReadQueryIT extends IntegrationTestBase {

    @Autowired
    private PayByCardUseCase payByCardUseCase;
    @Autowired
    private MerchantWithdrawAtAgentUseCase merchantWithdrawAtAgentUseCase;
    @Autowired
    private BackofficeTransactionReadPort backofficeTransactionReadPort;
    @Autowired
    private BackofficeAuditEventReadPort backofficeAuditEventReadPort;

    @Test
    void transaction_detail_returns_real_ledger_lines_and_context_from_writes() {
        Merchant merchant = createActiveMerchant("M-123456");
        var terminal = createActiveTerminal(merchant);
        Client client = createActiveClient("+2693000000");
        createActiveCard(client, "CARD-001", "1234");
        seedLedgerCredit(LedgerAccountRef.client(client.id().value().toString()), new BigDecimal("500.00"));

        var payment = payByCardUseCase.execute(new PayByCardCommand(
                "idem-pay-1",
                "hash-pay-1",
                terminalActor(terminal.id().value().toString()),
                terminal.id().value().toString(),
                "CARD-001",
                "1234",
                new BigDecimal("120.00")
        ));

        var detail = backofficeTransactionReadPort.findById(payment.transactionId()).orElseThrow();

        assertEquals("PAY_BY_CARD", detail.type());
        assertEquals(3, detail.ledgerLines().size());
        assertEquals("CARD-001", detail.cardUid());
        assertEquals(terminal.id().value().toString(), detail.terminalUid());
        assertEquals(client.phoneNumber(), detail.clientPhone());
        assertTrue(detail.ledgerLines().stream().anyMatch(l -> l.accountType().equals("CLIENT") && l.entryType().equals("DEBIT")));
        assertTrue(detail.ledgerLines().stream().anyMatch(l -> l.accountType().equals("MERCHANT") && l.entryType().equals("CREDIT")));
    }

    @Test
    void transaction_list_filters_and_audit_resource_filters_use_persisted_data() {
        Merchant merchant = createActiveMerchant("M-654321");
        Agent agent = createActiveAgent("A-654321");
        seedLedgerCredit(LedgerAccountRef.merchant(merchant.id().value().toString()), new BigDecimal("200.00"));

        var withdraw = merchantWithdrawAtAgentUseCase.execute(new MerchantWithdrawAtAgentCommand(
                "idem-wd-1",
                "hash-wd-1",
                agentActor(agent.id().value().toString()),
                merchant.code().value(),
                agent.code().value(),
                new BigDecimal("100.00")
        ));

        var byMerchant = backofficeTransactionReadPort.list(new BackofficeTransactionQuery(
                null, null, null, null, null,
                null, null, merchant.code().value(), null, null,
                null, null, null, null,
                20, null, null
        ));
        assertTrue(byMerchant.items().stream().anyMatch(i -> i.transactionId().equals(withdraw.transactionId())));

        var byAgent = backofficeTransactionReadPort.list(new BackofficeTransactionQuery(
                null, null, null, null, null,
                null, null, null, agent.code().value(), null,
                null, null, null, null,
                20, null, null
        ));
        assertTrue(byAgent.items().stream().anyMatch(i -> i.transactionId().equals(withdraw.transactionId())));

        var auditByResource = backofficeAuditEventReadPort.list(new BackofficeAuditEventQuery(
                null,
                null,
                null,
                "AGENT",
                agent.id().value().toString(),
                null,
                null,
                20,
                null,
                null
        ));

        assertFalse(auditByResource.items().isEmpty());
        assertTrue(auditByResource.items().stream().allMatch(i -> "AGENT".equals(i.resourceType()) && agent.id().value().toString().equals(i.resourceId())));
    }
}

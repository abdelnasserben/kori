package com.kori.it;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.result.EnrollCardResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.PhoneNumber;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EnrollCardServiceIT extends IntegrationTestBase {

    private static final String AGENT_CODE = "A-123456";
    private static final String CLIENT_PHONE = "+2693001122";
    private static final String CARD_UID = "CARD-ENROLL-001";
    private static final String PIN = "1234";

    @Autowired EnrollCardUseCase enrollCardUseCase;

    @Test
    void enrollCard_happyPath_persistsClientCardLedgerAndAudit() {
        Agent agent = createActiveAgent(AGENT_CODE);

        EnrollCardResult result = enrollCardUseCase.execute(new EnrollCardCommand(
                "idem-enroll-1",
                "request-hash",
                agentActor("A-000001"),
                CLIENT_PHONE,
                CARD_UID,
                PIN,
                AGENT_CODE
        ));

        assertTrue(result.clientCreated());
        assertTrue(result.accountCreated());
        assertNotNull(result.transactionId());

        Client client = clientRepositoryPort.findByPhoneNumber(PhoneNumber.of(CLIENT_PHONE)).orElseThrow();
        assertEquals(CLIENT_PHONE, client.phoneNumber().value());

        assertTrue(cardRepositoryPort.findByCardUid(CARD_UID).isPresent());

        List<LedgerEntry> entries = ledgerQueryPort.findByTransactionId(TransactionId.of(result.transactionId()));
        assertEquals(3, entries.size());

        LedgerAccountRef agentAccount = LedgerAccountRef.agentWallet(agent.id().value().toString());

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.DEBIT
                        && entry.accountRef().equals(LedgerAccountRef.agentCashClearing(agent.id().value().toString()))
                        && entry.amount().equals(Money.of(new BigDecimal("10.00")))
        ));
        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(LedgerAccountRef.platformFeeRevenue())
                        && entry.amount().equals(Money.of(new BigDecimal("7.00")))
        ));

        assertTrue(entries.stream().anyMatch(entry ->
                entry.type() == LedgerEntryType.CREDIT
                        && entry.accountRef().equals(agentAccount)
                        && entry.amount().equals(Money.of(new BigDecimal("3.00")))
        ));

        assertTrue(entries.stream().noneMatch(entry ->
                entry.type() == LedgerEntryType.DEBIT
                        && entry.accountRef().equals(agentAccount)
        ));

        assertTrue(entries.stream().noneMatch(entry ->
                entry.accountRef().equals(LedgerAccountRef.platformClearing())
        ));

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("ENROLL_CARD"))
        );
    }
}

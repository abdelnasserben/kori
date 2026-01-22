package com.kori.integration.usecase;

import com.kori.application.command.GetBalanceCommand;
import com.kori.application.port.in.GetBalanceUseCase;
import com.kori.application.result.BalanceResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.integration.AbstractIntegrationTest;
import com.kori.integration.fixture.LedgerSqlFixture;
import com.kori.integration.fixture.TransactionFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GetBalanceIT extends AbstractIntegrationTest {

    @Autowired GetBalanceUseCase getBalanceUseCase;

    @Test
    void getBalance_returnsSelfClientBalance_fromLedgerEntries() {
        // Given
        String clientId = uuid().toString();
        String otherClientId = uuid().toString();

        UUID tx1 = uuid();
        UUID tx2 = uuid();
        UUID txOther = uuid();

        OffsetDateTime now = OffsetDateTime.now();

        TransactionFixture txFixture = new TransactionFixture(transactionJpaRepository);
        LedgerSqlFixture ledgerFixture = new LedgerSqlFixture(jdbcTemplate);

        // transactions
        txFixture.create(tx1, "TEST_TX", new BigDecimal("0.00"), now, null);
        txFixture.create(tx2, "TEST_TX", new BigDecimal("0.00"), now.plusSeconds(1), null);
        txFixture.create(txOther, "TEST_TX", new BigDecimal("0.00"), now.plusSeconds(2), null);

        // ledger entries for our client (CLIENT scope)
        // credits: 100.00 ; debits: 30.00 => expected 70.00
        ledgerFixture.insertEntry(uuid(), tx1, "CLIENT", "CREDIT", new BigDecimal("100.00"), clientId, now);
        ledgerFixture.insertEntry(uuid(), tx2, "CLIENT", "DEBIT", new BigDecimal("30.00"), clientId, now.plusSeconds(1));

        // noise: other client entries (must NOT be counted)
        ledgerFixture.insertEntry(uuid(), txOther, "CLIENT", "CREDIT", new BigDecimal("999.00"), otherClientId, now.plusSeconds(2));

        // When
        BalanceResult result = getBalanceUseCase.execute(
                GetBalanceCommand.self(new ActorContext(ActorType.CLIENT, clientId, Map.of()))
        );

        // Then (business result)
        assertEquals(LedgerAccount.CLIENT, result.ledgerAccount());
        assertEquals(clientId, result.referenceId());
        assertEquals(0, result.balance().compareTo(new BigDecimal("70.00")));

        // And (DB check)
        BigDecimal dbComputed = jdbcTemplate.queryForObject(
                """
                select coalesce(sum(case when entry_type = 'CREDIT' then amount else -amount end), 0)
                from ledger_entries
                where account = 'CLIENT' and reference_id = ?
                """,
                BigDecimal.class,
                clientId
        );
        assertNotNull(dbComputed);
        assertEquals(0, dbComputed.compareTo(new BigDecimal("70.00")));
    }
}

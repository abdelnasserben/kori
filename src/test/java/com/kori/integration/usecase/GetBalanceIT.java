package com.kori.integration.usecase;

import com.kori.application.command.GetBalanceCommand;
import com.kori.application.port.in.GetBalanceUseCase;
import com.kori.application.result.BalanceResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class GetBalanceIT {

    @Autowired GetBalanceUseCase getBalanceUseCase;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void getBalance_returnsSelfClientBalance_fromLedgerEntries() {
        // Given
        String clientId = UUID.randomUUID().toString();
        String otherClientId = UUID.randomUUID().toString();

        UUID tx1 = UUID.randomUUID();
        UUID tx2 = UUID.randomUUID();
        UUID txOther = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        // transactions
        jdbcTemplate.update(
                "insert into transactions (id, type, amount, created_at, original_transaction_id) values (?, ?, ?, ?, ?)",
                tx1, "TEST_TX", new BigDecimal("0.00"), now, null
        );
        jdbcTemplate.update(
                "insert into transactions (id, type, amount, created_at, original_transaction_id) values (?, ?, ?, ?, ?)",
                tx2, "TEST_TX", new BigDecimal("0.00"), now.plusSeconds(1), null
        );
        jdbcTemplate.update(
                "insert into transactions (id, type, amount, created_at, original_transaction_id) values (?, ?, ?, ?, ?)",
                txOther, "TEST_TX", new BigDecimal("0.00"), now.plusSeconds(2), null
        );

        // ledger entries for our client (CLIENT scope)
        // credits: 100.00 ; debits: 30.00 => expected 70.00
        jdbcTemplate.update(
                "insert into ledger_entries (id, transaction_id, account, entry_type, amount, reference_id, created_at) " +
                        "values (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), tx1, "CLIENT", "CREDIT", new BigDecimal("100.00"), clientId, now
        );
        jdbcTemplate.update(
                "insert into ledger_entries (id, transaction_id, account, entry_type, amount, reference_id, created_at) " +
                        "values (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), tx2, "CLIENT", "DEBIT", new BigDecimal("30.00"), clientId, now.plusSeconds(1)
        );

        // noise: other client entries (must NOT be counted)
        jdbcTemplate.update(
                "insert into ledger_entries (id, transaction_id, account, entry_type, amount, reference_id, created_at) " +
                        "values (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), txOther, "CLIENT", "CREDIT", new BigDecimal("999.00"), otherClientId, now.plusSeconds(2)
        );

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

package com.kori.application.usecase;

import com.kori.application.command.GetBalanceCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.result.BalanceResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.security.LedgerAccessPolicy;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBalanceServiceTest {

    @Mock LedgerQueryPort ledgerQueryPort;

    private GetBalanceService service;

    @BeforeEach
    void setUp() {
        service = new GetBalanceService(ledgerQueryPort, new LedgerAccessPolicy());
    }

    @Test
    void clientSelfBalance_sumsCreditsMinusDebits() {
        var actor = new ActorContext(ActorType.CLIENT, "c-1", Map.of());
        when(ledgerQueryPort.findEntries(LedgerAccount.CLIENT, "c-1"))
                .thenReturn(List.of(
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.CLIENT, Money.of(new BigDecimal("10.00")), "c-1"),
                        LedgerEntry.debit(TransactionId.of("tx-2"), LedgerAccount.CLIENT, Money.of(new BigDecimal("2.50")), "c-1")
                ));

        BalanceResult result = service.execute(GetBalanceCommand.self(actor));

        assertEquals(LedgerAccount.CLIENT, result.ledgerAccount());
        assertEquals("c-1", result.referenceId());
        assertEquals(new BigDecimal("7.50"), result.balance());
    }

    @Test
    void adminCanQueryArbitraryLedgerScope() {
        var actor = new ActorContext(ActorType.ADMIN, "admin", Map.of());
        when(ledgerQueryPort.findEntries(LedgerAccount.MERCHANT, "m-9"))
                .thenReturn(List.of(
                        LedgerEntry.credit(TransactionId.of("tx-1"), LedgerAccount.MERCHANT, Money.of(new BigDecimal("100.00")), "m-9")
                ));

        BalanceResult result = service.execute(new GetBalanceCommand(actor, LedgerAccount.MERCHANT, "m-9"));

        assertEquals(new BigDecimal("100.00"), result.balance());
    }

    @Test
    void nonAdminCannotSpecifyArbitraryScope() {
        var actor = new ActorContext(ActorType.AGENT, "a-1", Map.of());

        assertThrows(ForbiddenOperationException.class,
                () -> service.execute(new GetBalanceCommand(actor, LedgerAccount.CLIENT, "c-1")));
        verifyNoInteractions(ledgerQueryPort);
    }
}

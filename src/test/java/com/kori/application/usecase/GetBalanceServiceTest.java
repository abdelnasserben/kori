package com.kori.application.usecase;

import com.kori.application.command.GetBalanceCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.result.BalanceResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.security.LedgerAccessPolicy;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class GetBalanceServiceTest {

    @Mock LedgerQueryPort ledgerQueryPort;

    private static final LedgerAccessPolicy POLICY = new LedgerAccessPolicy();

    private static final TransactionId TX_1 = new TransactionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final TransactionId TX_2 = new TransactionId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    private static ActorContext adminActor() {
        return new ActorContext(ActorType.ADMIN, "admin-actor", Map.of());
    }

    private static ActorContext agentActor(String agentCode) {
        return new ActorContext(ActorType.AGENT, agentCode, Map.of());
    }

    @Test
    void admin_canReadSpecifiedAccount() {
        GetBalanceService service = new GetBalanceService(ledgerQueryPort, POLICY);

        LedgerAccountRef scope = new LedgerAccountRef(LedgerAccountType.MERCHANT, "M-123456");

        when(ledgerQueryPort.findEntries(scope)).thenReturn(List.of(
                LedgerEntry.credit(TX_1, scope, Money.of(new BigDecimal("100.00"))),
                LedgerEntry.debit(TX_1, scope, Money.of(new BigDecimal("30.00")))
        ));

        BalanceResult out = service.execute(new GetBalanceCommand(
                adminActor(),
                LedgerAccountType.MERCHANT.name(),
                "M-123456"
        ));

        assertEquals(LedgerAccountType.MERCHANT.name(), out.accountType());
        assertEquals("M-123456", out.ownerRef());
        assertEquals(new BigDecimal("70.00"), out.balance());

        verify(ledgerQueryPort).findEntries(scope);
        verifyNoMoreInteractions(ledgerQueryPort);
    }

    @Test
    void admin_forbidden_whenScopeMissing() {
        GetBalanceService service = new GetBalanceService(ledgerQueryPort, POLICY);

        assertThrows(ForbiddenOperationException.class, () -> service.execute(new GetBalanceCommand(
                adminActor(),
                null,
                null
        )));

        verifyNoInteractions(ledgerQueryPort);
    }

    @Test
    void agent_canReadOwnBalance_withoutProvidingScope() {
        GetBalanceService service = new GetBalanceService(ledgerQueryPort, POLICY);

        String agentCode = "A-123456";
        LedgerAccountRef scope = LedgerAccountRef.agent(agentCode);

        when(ledgerQueryPort.findEntries(scope)).thenReturn(List.of(
                LedgerEntry.credit(TX_1, scope, Money.of(new BigDecimal("12.50"))),
                LedgerEntry.debit(TX_2, scope, Money.of(new BigDecimal("2.25")))
        ));

        BalanceResult out = service.execute(new GetBalanceCommand(
                agentActor(agentCode),
                null,
                null
        ));

        assertEquals(LedgerAccountType.AGENT.name(), out.accountType());
        assertEquals(agentCode, out.ownerRef());
        assertEquals(new BigDecimal("10.25"), out.balance());

        ArgumentCaptor<LedgerAccountRef> captor = ArgumentCaptor.forClass(LedgerAccountRef.class);
        verify(ledgerQueryPort).findEntries(captor.capture());
        assertEquals(scope, captor.getValue());
    }

    @Test
    void agent_forbidden_whenTryingToReadAnotherAccount() {
        GetBalanceService service = new GetBalanceService(ledgerQueryPort, POLICY);

        assertThrows(ForbiddenOperationException.class, () -> service.execute(new GetBalanceCommand(
                agentActor("A-123456"),
                LedgerAccountType.AGENT.name(),
                "A-999999"
        )));

        verifyNoInteractions(ledgerQueryPort);
    }

    @Test
    void agent_throwsValidation_whenProvidesPartialScope() {
        GetBalanceService service = new GetBalanceService(ledgerQueryPort, POLICY);

        assertThrows(ValidationException.class, () -> service.execute(new GetBalanceCommand(
                agentActor("A-123456"),
                LedgerAccountType.AGENT.name(),
                null
        )));

        verifyNoInteractions(ledgerQueryPort);
    }

    @Test
    void terminal_forbidden() {
        GetBalanceService service = new GetBalanceService(ledgerQueryPort, POLICY);

        ActorContext terminal = new ActorContext(ActorType.TERMINAL, "T-1", Map.of());

        assertThrows(ForbiddenOperationException.class, () -> service.execute(new GetBalanceCommand(
                terminal,
                null,
                null
        )));

        verifyNoInteractions(ledgerQueryPort);
    }
}

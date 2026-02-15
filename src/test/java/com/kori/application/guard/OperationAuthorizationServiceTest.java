package com.kori.application.guard;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.application.usecase.OperationAuthorizationService;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.agent.AgentId;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationAuthorizationServiceTest {

    @Mock  AccountProfilePort accountProfilePort;
    @InjectMocks
    OperationAuthorizationService guards;

    private final static Instant NOW = Instant.now();

    // CLIENT

    @Test
    void authorizePayment() {
        Client client = activeClient();
        AccountProfile profile = activeProfile();

        LedgerAccountRef ref = LedgerAccountRef.client(client.id().value().toString());
        when(accountProfilePort.findByAccount(ref)).thenReturn(Optional.of(profile));

        assertDoesNotThrow(() -> guards.authorizeClientPayment(client));

        verify(accountProfilePort).findByAccount(ref);
    }

    @Test
    void authorizePayment() {
        Client client = clientWithStatus(Status.SUSPENDED);

        ForbiddenOperationException ex =
                assertThrows(ForbiddenOperationException.class,
                        () -> guards.authorizeClientPayment(client));

        assertEquals("CLIENT_NOT_ACTIVE", ex.getMessage());
        verifyNoInteractions(accountProfilePort);
    }

    @Test
    void authorizeClient_Payment_throws_whenAccountProfileMissing() {
        Client client = activeClient();
        LedgerAccountRef ref = LedgerAccountRef.client(client.id().value().toString());

        when(accountProfilePort.findByAccount(ref)).thenReturn(Optional.empty());

        ForbiddenOperationException ex =
                assertThrows(ForbiddenOperationException.class,
                        () -> guards.authorizeClientPayment(client));

        assertEquals("CLIENT_ACCOUNT_INACTIVE_OR_MISSING", ex.getMessage());
    }

    @Test
    void authorizeClient_Payment_throws_whenAccountProfileSuspended() {
        Client client = activeClient();
        AccountProfile profile = profileWithStatus(Status.SUSPENDED);

        LedgerAccountRef ref = LedgerAccountRef.client(client.id().value().toString());
        when(accountProfilePort.findByAccount(ref)).thenReturn(Optional.of(profile));

        ForbiddenOperationException ex =
                assertThrows(ForbiddenOperationException.class,
                        () -> guards.authorizeClientPayment(client));

        assertEquals("CLIENT_ACCOUNT_INACTIVE_OR_MISSING", ex.getMessage());
    }

    // MERCHANT

    @Test
    void authorizePayment() {
        Merchant merchant = activeMerchant();
        AccountProfile profile = activeProfile();

        LedgerAccountRef ref = LedgerAccountRef.merchant(merchant.id().value().toString());
        when(accountProfilePort.findByAccount(ref)).thenReturn(Optional.of(profile));

        assertDoesNotThrow(() -> guards.authorizeMerchantPayment(merchant));
    }

    @Test
    void authorizePayment() {
        Merchant merchant = merchantWithStatus(Status.CLOSED);

        ForbiddenOperationException ex =
                assertThrows(ForbiddenOperationException.class,
                        () -> guards.authorizeMerchantPayment(merchant));

        assertEquals("MERCHANT_NOT_ACTIVE", ex.getMessage());
        verifyNoInteractions(accountProfilePort);
    }

    // AGENT

    @Test
    void authorizeOperation() {
        Agent agent = activeAgent();
        AccountProfile profile = activeProfile();

        LedgerAccountRef walletRef = LedgerAccountRef.agentWallet(agent.id().value().toString());
        LedgerAccountRef clearingRef = LedgerAccountRef.agentCashClearing(agent.id().value().toString());
        when(accountProfilePort.findByAccount(walletRef)).thenReturn(Optional.of(profile));
        when(accountProfilePort.findByAccount(clearingRef)).thenReturn(Optional.of(profile));

        assertDoesNotThrow(() -> guards.authorizeAgentOperation(agent));
    }

    @Test
    void authorizeAgent_throws_whenAgentOperationSuspended() {
        Agent agent = agentWithStatus(Status.SUSPENDED);

        ForbiddenOperationException ex =
                assertThrows(ForbiddenOperationException.class,
                        () -> guards.authorizeAgentOperation(agent));

        assertEquals("AGENT_NOT_ACTIVE", ex.getMessage());
        verifyNoInteractions(accountProfilePort);
    }

    @Test
    void authorizeAgent_Operation_throws_whenClearingAccountMissing() {
        Agent agent = activeAgent();

        LedgerAccountRef walletRef = LedgerAccountRef.agentWallet(agent.id().value().toString());
        LedgerAccountRef clearingRef = LedgerAccountRef.agentCashClearing(agent.id().value().toString());
        when(accountProfilePort.findByAccount(walletRef)).thenReturn(Optional.of(activeProfile()));
        when(accountProfilePort.findByAccount(clearingRef)).thenReturn(Optional.empty());

        ForbiddenOperationException ex =
                assertThrows(ForbiddenOperationException.class,
                        () -> guards.authorizeAgentOperation(agent));

        assertEquals("AGENT_ACCOUNT_INACTIVE_OR_MISSING", ex.getMessage());
    }

    // HELPERS

    private Client activeClient() {
        return clientWithStatus(Status.ACTIVE);
    }

    private Client clientWithStatus(Status status) {
        return new Client(
                new ClientId(UUID.randomUUID()),
                "+123456789",
                status,
                NOW
        );
    }

    private Merchant activeMerchant() {
        return merchantWithStatus(Status.ACTIVE);
    }

    private Merchant merchantWithStatus(Status status) {
        return new Merchant(
                new MerchantId(UUID.randomUUID()),
                MerchantCode.of("M-123456"),
                status,
                NOW
        );
    }

    private Agent activeAgent() {
        return agentWithStatus(Status.ACTIVE);
    }

    private Agent agentWithStatus(Status status) {
        return new Agent(
                new AgentId(UUID.randomUUID()),
                AgentCode.of("A-123456"),
                NOW,
                status
        );
    }

    private AccountProfile activeProfile() {
        return profileWithStatus(Status.ACTIVE);
    }

    private AccountProfile profileWithStatus(Status status) {
        return Mockito.mock(AccountProfile.class, invocation -> {
            if ("status".equals(invocation.getMethod().getName())) {
                return status;
            }
            return invocation.callRealMethod();
        });
    }
}

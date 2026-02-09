package com.kori.application.usecase;

import com.kori.application.command.RequestClientRefundCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.*;
import com.kori.application.result.ClientRefundResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestClientRefundServiceTest {

    @Mock TimeProviderPort timeProviderPort;
    @Mock IdempotencyPort idempotencyPort;
    @Mock ClientRepositoryPort clientRepositoryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock LedgerQueryPort ledgerQueryPort;
    @Mock TransactionRepositoryPort transactionRepositoryPort;
    @Mock ClientRefundRepositoryPort clientRefundRepositoryPort;
    @Mock AuditPort auditPort;
    @Mock IdGeneratorPort idGeneratorPort;

    @InjectMocks RequestClientRefundService service;

    @BeforeEach
    void setUp() {
        lenient().when(idempotencyPort.reserve(anyString(), anyString(), any())).thenReturn(true);
    }

    @Test
    void forbidden_when_client_wallet_zero() {
        var clientId = UUID.randomUUID();
        var cmd = new RequestClientRefundCommand("idem", "hash", new ActorContext(ActorType.ADMIN, "a", Map.of()), clientId.toString());
        when(idempotencyPort.find(any(), any(), eq(ClientRefundResult.class))).thenReturn(Optional.empty());
        when(clientRepositoryPort.findById(any())).thenReturn(Optional.of(new Client(new ClientId(clientId), "7712345", Status.ACTIVE, Instant.now())));
        when(clientRefundRepositoryPort.existsRequestedForClient(any())).thenReturn(false);
        when(ledgerQueryPort.netBalance(any())).thenReturn(Money.zero());

        assertThrows(ForbiddenOperationException.class, () -> service.execute(cmd));
    }

    @Test
    void happy_path_requests_refund() {
        var clientId = UUID.randomUUID();
        var cmd = new RequestClientRefundCommand("idem", "hash", new ActorContext(ActorType.ADMIN, "a", Map.of()), clientId.toString());
        when(idempotencyPort.find(any(), any(), eq(ClientRefundResult.class))).thenReturn(Optional.empty());
        when(clientRepositoryPort.findById(any())).thenReturn(Optional.of(new Client(new ClientId(clientId), "7712345", Status.ACTIVE, Instant.now())));
        when(clientRefundRepositoryPort.existsRequestedForClient(any())).thenReturn(false);
        when(ledgerQueryPort.netBalance(any())).thenReturn(Money.of(new BigDecimal("50.00")));
        when(idGeneratorPort.newUuid()).thenReturn(UUID.randomUUID(), UUID.randomUUID());
        when(timeProviderPort.now()).thenReturn(Instant.now());

        var out = service.execute(cmd);

        assertEquals("REQUESTED", out.refundStatus());
        verify(ledgerAppendPort).append(any());
        verify(clientRefundRepositoryPort).save(any());
    }
}

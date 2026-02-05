package com.kori.application.usecase;

import com.kori.application.command.FailClientRefundCommand;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.ClientRefundRepositoryPort;
import com.kori.application.port.out.LedgerAppendPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.clientrefund.ClientRefund;
import com.kori.domain.model.clientrefund.ClientRefundId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailClientRefundServiceTest {
    @Mock TimeProviderPort timeProviderPort;
    @Mock ClientRefundRepositoryPort clientRefundRepositoryPort;
    @Mock LedgerAppendPort ledgerAppendPort;
    @Mock AuditPort auditPort;
    @InjectMocks FailClientRefundService service;

    @Test
    void fails_requested_refund() {
        var refund = ClientRefund.requested(new ClientRefundId(UUID.randomUUID()), new ClientId(UUID.randomUUID()), new TransactionId(UUID.randomUUID()), Money.of(new BigDecimal("10.00")), Instant.now());
        when(clientRefundRepositoryPort.findById(any())).thenReturn(Optional.of(refund));
        when(timeProviderPort.now()).thenReturn(Instant.now());

        var out = service.execute(new FailClientRefundCommand(new ActorContext(ActorType.ADMIN, "a", Map.of()), refund.id().value().toString(), "x"));

        assertEquals("FAILED", out.refundStatus());
        verify(ledgerAppendPort).append(any());
        verify(clientRefundRepositoryPort).save(refund);
    }
}

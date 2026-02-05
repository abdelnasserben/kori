package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.ClientRefundEntity;
import com.kori.adapters.out.jpa.repo.ClientRefundJpaRepository;
import com.kori.application.port.out.ClientRefundRepositoryPort;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.clientrefund.ClientRefund;
import com.kori.domain.model.clientrefund.ClientRefundId;
import com.kori.domain.model.clientrefund.ClientRefundStatus;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.TransactionId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class JpaClientRefundRepositoryAdapter implements ClientRefundRepositoryPort {

    private final ClientRefundJpaRepository repo;

    public JpaClientRefundRepositoryAdapter(ClientRefundJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public ClientRefund save(ClientRefund clientRefund) {
        repo.save(new ClientRefundEntity(
                clientRefund.id().value(),
                clientRefund.clientId().value(),
                clientRefund.transactionId().value(),
                clientRefund.amount().asBigDecimal(),
                clientRefund.status().name(),
                clientRefund.createdAt(),
                clientRefund.completedAt(),
                clientRefund.failedAt(),
                clientRefund.failureReason()
        ));
        return clientRefund;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientRefund> findById(ClientRefundId refundId) {
        return repo.findById(refundId.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsRequestedForClient(ClientId clientId) {
        return repo.existsByClientIdAndStatus(clientId.value(), ClientRefundStatus.REQUESTED.name());
    }

    private ClientRefund toDomain(ClientRefundEntity e) {
        return new ClientRefund(
                new ClientRefundId(e.getId()),
                new ClientId(e.getClientId()),
                new TransactionId(e.getTransactionId()),
                Money.of(e.getAmount()),
                ClientRefundStatus.valueOf(e.getStatus()),
                e.getCreatedAt(),
                e.getCompletedAt(),
                e.getFailedAt(),
                e.getFailureReason()
        );
    }
}

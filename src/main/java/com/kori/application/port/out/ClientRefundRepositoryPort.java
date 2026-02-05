package com.kori.application.port.out;

import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.clientrefund.ClientRefund;
import com.kori.domain.model.clientrefund.ClientRefundId;

import java.util.Optional;

public interface ClientRefundRepositoryPort {
    ClientRefund save(ClientRefund clientRefund);
    Optional<ClientRefund> findById(ClientRefundId refundId);
    boolean existsRequestedForClient(ClientId clientId);
}

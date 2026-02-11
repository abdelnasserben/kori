package com.kori.query.port.in;

import com.kori.query.model.BackofficeActorDetails;

public interface BackofficeActorDetailQueryUseCase {
    BackofficeActorDetails getAgentById(String agentId);

    BackofficeActorDetails getClientById(String clientId);

    BackofficeActorDetails getMerchantById(String merchantId);
}

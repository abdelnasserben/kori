package com.kori.application.port.in.query;

import com.kori.application.query.BackofficeActorDetails;

public interface BackofficeActorDetailQueryUseCase {
    BackofficeActorDetails getAgentById(String agentId);

    BackofficeActorDetails getClientById(String clientId);

    BackofficeActorDetails getMerchantById(String merchantId);
}

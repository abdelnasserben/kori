package com.kori.query.port.in;

import com.kori.query.model.BackofficeActorDetails;

public interface BackofficeActorDetailQueryUseCase {
    BackofficeActorDetails getAgentByRef(String agentCode);

    BackofficeActorDetails getClientByRef(String clientCode);

    BackofficeActorDetails getMerchantByRef(String merchantCode);
}

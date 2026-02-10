package com.kori.application.port.out.query;

import com.kori.application.query.BackofficeActorDetails;

import java.util.Optional;

public interface BackofficeActorDetailReadPort {
    Optional<BackofficeActorDetails> findAgentById(String agentId);

    Optional<BackofficeActorDetails> findClientById(String clientId);

    Optional<BackofficeActorDetails> findMerchantById(String merchantId);
}

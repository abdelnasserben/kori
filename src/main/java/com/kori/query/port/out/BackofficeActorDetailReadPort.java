package com.kori.query.port.out;

import com.kori.query.model.BackofficeActorDetails;

import java.util.Optional;

public interface BackofficeActorDetailReadPort {
    Optional<BackofficeActorDetails> findAgentById(String agentId);

    Optional<BackofficeActorDetails> findClientById(String clientId);

    Optional<BackofficeActorDetails> findMerchantById(String merchantId);
}

package com.kori.query.port.out;

import com.kori.query.model.BackofficeActorDetails;

import java.util.Optional;

public interface BackofficeActorDetailReadPort {
    Optional<BackofficeActorDetails> findAgentByRef(String agentCode);

    Optional<BackofficeActorDetails> findClientByRef(String clientCode);

    Optional<BackofficeActorDetails> findMerchantByRef(String merchantCode);

    Optional<BackofficeActorDetails> findTerminalByRef(String terminalUid);

    Optional<BackofficeActorDetails> findAdminByRef(String adminUsername);
}

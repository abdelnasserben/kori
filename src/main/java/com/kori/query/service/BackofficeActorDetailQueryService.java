package com.kori.query.service;

import com.kori.application.exception.NotFoundException;
import com.kori.query.model.BackofficeActorDetails;
import com.kori.query.port.in.BackofficeActorDetailQueryUseCase;
import com.kori.query.port.out.BackofficeActorDetailReadPort;

import java.util.Objects;

public class BackofficeActorDetailQueryService implements BackofficeActorDetailQueryUseCase {

    private final BackofficeActorDetailReadPort readPort;

    public BackofficeActorDetailQueryService(BackofficeActorDetailReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    public BackofficeActorDetails getAgentByRef(String agentCode) {
        return readPort.findAgentByRef(agentCode).orElseThrow(() -> new NotFoundException("Agent not found"));
    }

    @Override
    public BackofficeActorDetails getClientByRef(String clientCode) {
        return readPort.findClientByRef(clientCode).orElseThrow(() -> new NotFoundException("Client not found"));
    }

    @Override
    public BackofficeActorDetails getMerchantByRef(String merchantCode) {
        return readPort.findMerchantByRef(merchantCode).orElseThrow(() -> new NotFoundException("Merchant not found"));
    }
}

package com.kori.application.query.service;

import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.query.BackofficeActorDetailQueryUseCase;
import com.kori.application.port.out.query.BackofficeActorDetailReadPort;
import com.kori.application.query.BackofficeActorDetails;
import com.kori.application.utils.UuidParser;

import java.util.Objects;

public class BackofficeActorDetailQueryService implements BackofficeActorDetailQueryUseCase {

    private final BackofficeActorDetailReadPort readPort;

    public BackofficeActorDetailQueryService(BackofficeActorDetailReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public BackofficeActorDetails getAgentById(String agentId) {
        UuidParser.parse(agentId, "agentId");
        return readPort.findAgentById(agentId).orElseThrow(() -> new NotFoundException("Agent not found"));
    }

    @Override
    public BackofficeActorDetails getClientById(String clientId) {
        UuidParser.parse(clientId, "clientId");
        return readPort.findClientById(clientId).orElseThrow(() -> new NotFoundException("Client not found"));
    }

    @Override
    public BackofficeActorDetails getMerchantById(String merchantId) {
        UuidParser.parse(merchantId, "merchantId");
        return readPort.findMerchantById(merchantId).orElseThrow(() -> new NotFoundException("Merchant not found"));
    }
}

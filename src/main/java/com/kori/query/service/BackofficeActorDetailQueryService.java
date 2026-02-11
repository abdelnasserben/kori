package com.kori.query.service;

import com.kori.application.exception.NotFoundException;
import com.kori.application.utils.UuidParser;
import com.kori.query.model.BackofficeActorDetails;
import com.kori.query.port.in.BackofficeActorDetailQueryUseCase;
import com.kori.query.port.out.BackofficeActorDetailReadPort;

import java.util.Objects;

public class BackofficeActorDetailQueryService implements BackofficeActorDetailQueryUseCase {

    private final BackofficeActorDetailReadPort readPort;

    public BackofficeActorDetailQueryService(BackofficeActorDetailReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public BackofficeActorDetails getAgentById(String agentId) {
        UuidParser.parse(agentId, "agentCode");
        return readPort.findAgentById(agentId).orElseThrow(() -> new NotFoundException("Agent not found"));
    }

    @Override
    public BackofficeActorDetails getClientById(String clientId) {
        UuidParser.parse(clientId, "clientId");
        return readPort.findClientById(clientId).orElseThrow(() -> new NotFoundException("Client not found"));
    }

    @Override
    public BackofficeActorDetails getMerchantById(String merchantId) {
        UuidParser.parse(merchantId, "merchantCode");
        return readPort.findMerchantById(merchantId).orElseThrow(() -> new NotFoundException("Merchant not found"));
    }
}

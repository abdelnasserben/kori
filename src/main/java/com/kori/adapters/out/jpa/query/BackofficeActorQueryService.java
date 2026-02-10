package com.kori.adapters.out.jpa.query;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class BackofficeActorQueryService implements BackofficeActorQueryUseCase {

    private final BackofficeActorReadPort readPort;

    public BackofficeActorQueryService(BackofficeActorReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public QueryPage<BackofficeActorItem> listAgents(BackofficeActorQuery query) {
        return readPort.listAgents(query);
    }

    @Override
    public QueryPage<BackofficeActorItem> listClients(BackofficeActorQuery query) {
        return readPort.listClients(query);
    }

    @Override
    public QueryPage<BackofficeActorItem> listMerchants(BackofficeActorQuery query) {
        return readPort.listMerchants(query);
    }
}

package com.kori.query.service;

import com.kori.query.model.BackofficeActorItem;
import com.kori.query.model.BackofficeActorQuery;
import com.kori.query.model.QueryPage;
import com.kori.query.port.in.BackofficeActorQueryUseCase;
import com.kori.query.port.out.BackofficeActorReadPort;

import java.util.Objects;

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

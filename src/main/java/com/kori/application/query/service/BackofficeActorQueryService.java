package com.kori.application.query.service;

import com.kori.application.port.in.query.BackofficeActorQueryUseCase;
import com.kori.application.port.out.query.BackofficeActorReadPort;
import com.kori.application.query.BackofficeActorItem;
import com.kori.application.query.BackofficeActorQuery;
import com.kori.application.query.QueryPage;

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

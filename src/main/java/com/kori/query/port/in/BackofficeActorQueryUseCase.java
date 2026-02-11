package com.kori.query.port.in;

import com.kori.query.model.BackofficeActorItem;
import com.kori.query.model.BackofficeActorQuery;
import com.kori.query.model.QueryPage;

public interface BackofficeActorQueryUseCase {
    QueryPage<BackofficeActorItem> listAgents(BackofficeActorQuery query);
    QueryPage<BackofficeActorItem> listClients(BackofficeActorQuery query);
    QueryPage<BackofficeActorItem> listMerchants(BackofficeActorQuery query);
}

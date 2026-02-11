package com.kori.query.port.out;

import com.kori.query.model.BackofficeActorItem;
import com.kori.query.model.BackofficeActorQuery;
import com.kori.query.model.QueryPage;

public interface BackofficeActorReadPort {
    QueryPage<BackofficeActorItem> listAgents(BackofficeActorQuery query);
    QueryPage<BackofficeActorItem> listClients(BackofficeActorQuery query);
    QueryPage<BackofficeActorItem> listMerchants(BackofficeActorQuery query);
}

package com.kori.application.port.out.query;

import com.kori.application.query.BackofficeActorItem;
import com.kori.application.query.BackofficeActorQuery;
import com.kori.application.query.QueryPage;

public interface BackofficeActorReadPort {
    QueryPage<BackofficeActorItem> listAgents(BackofficeActorQuery query);
    QueryPage<BackofficeActorItem> listClients(BackofficeActorQuery query);
    QueryPage<BackofficeActorItem> listMerchants(BackofficeActorQuery query);
}

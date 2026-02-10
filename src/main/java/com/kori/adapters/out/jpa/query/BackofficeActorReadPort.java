package com.kori.adapters.out.jpa.query;

public interface BackofficeActorReadPort {
    QueryPage<BackofficeActorItem> listAgents(BackofficeActorQuery query);
    QueryPage<BackofficeActorItem> listClients(BackofficeActorQuery query);
    QueryPage<BackofficeActorItem> listMerchants(BackofficeActorQuery query);
}

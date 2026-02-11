package com.kori.query.port.in;

import com.kori.application.security.ActorContext;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;

import java.util.List;

public interface ClientMeQueryUseCase {
    MeQueryModels.MeProfile getProfile(ActorContext actorContext);

    MeQueryModels.MeBalance getBalance(ActorContext actorContext);

    List<MeQueryModels.MeCardItem> listCards(ActorContext actorContext);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(ActorContext actorContext, MeQueryModels.MeTransactionsFilter filter);
}

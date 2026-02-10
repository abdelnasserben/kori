package com.kori.application.port.in.query;

import com.kori.application.query.QueryPage;
import com.kori.application.query.model.MeQueryModels;
import com.kori.application.security.ActorContext;

import java.util.List;

public interface ClientMeQueryUseCase {
    MeQueryModels.MeProfile getProfile(ActorContext actorContext);

    MeQueryModels.MeBalance getBalance(ActorContext actorContext);

    List<MeQueryModels.MeCardItem> listCards(ActorContext actorContext);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(ActorContext actorContext, MeQueryModels.MeTransactionsFilter filter);
}

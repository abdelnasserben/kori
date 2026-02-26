package com.kori.query.port.in;

import com.kori.application.security.ActorContext;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;

import java.util.List;

public interface ClientMeQueryUseCase {
    MeQueryModels.ClientProfile getProfile(ActorContext actorContext);

    MeQueryModels.ActorBalance getBalance(ActorContext actorContext);

    List<MeQueryModels.MeCardItem> listCards(ActorContext actorContext);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(ActorContext actorContext, MeQueryModels.MeTransactionsFilter filter);
}

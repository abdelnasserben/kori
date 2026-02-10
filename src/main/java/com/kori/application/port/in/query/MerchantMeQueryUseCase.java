package com.kori.application.port.in.query;

import com.kori.application.query.QueryPage;
import com.kori.application.query.model.MeQueryModels;
import com.kori.application.security.ActorContext;

public interface MerchantMeQueryUseCase {
    MeQueryModels.MeProfile getProfile(ActorContext actorContext);

    MeQueryModels.MeBalance getBalance(ActorContext actorContext);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(ActorContext actorContext, MeQueryModels.MeTransactionsFilter filter);

    QueryPage<MeQueryModels.MeTerminalItem> listTerminals(ActorContext actorContext, MeQueryModels.MeTerminalsFilter filter);

    MeQueryModels.MeTerminalItem getTerminalDetails(ActorContext actorContext, String terminalUid);
}

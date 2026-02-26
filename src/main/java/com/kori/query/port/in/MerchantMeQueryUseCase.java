package com.kori.query.port.in;

import com.kori.application.security.ActorContext;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.MeQueryModels;

public interface MerchantMeQueryUseCase {
    MeQueryModels.MerchantProfile getProfile(ActorContext actorContext);

    MeQueryModels.ActorBalance getBalance(ActorContext actorContext);

    QueryPage<MeQueryModels.MeTransactionItem> listTransactions(ActorContext actorContext, MeQueryModels.MeTransactionsFilter filter);

    QueryPage<MeQueryModels.MeTerminalItem> listTerminals(ActorContext actorContext, MeQueryModels.MeTerminalsFilter filter);

    MeQueryModels.MeTerminalItem getTerminalDetails(ActorContext actorContext, String terminalUid);
}

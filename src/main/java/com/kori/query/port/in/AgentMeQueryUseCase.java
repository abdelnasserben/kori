package com.kori.query.port.in;

import com.kori.application.security.ActorContext;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.model.me.MeQueryModels;

public interface AgentMeQueryUseCase {
    MeQueryModels.AgentProfile getProfile(ActorContext actorContext);

    MeQueryModels.ActorBalance getBalance(ActorContext actorContext);

    QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(ActorContext actorContext, AgentQueryModels.AgentTransactionFilter filter);

    MeQueryModels.AgentTransactionDetails getTransactionDetails(ActorContext actorContext, String transactionRef);

    QueryPage<AgentQueryModels.AgentActivityItem> listActivities(ActorContext actorContext, AgentQueryModels.AgentActivityFilter filter);
}

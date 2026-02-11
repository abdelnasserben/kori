package com.kori.query.port.in;

import com.kori.application.security.ActorContext;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.AgentQueryModels;

public interface AgentMeQueryUseCase {
    AgentQueryModels.AgentSummary getSummary(ActorContext actorContext);

    QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(ActorContext actorContext, AgentQueryModels.AgentTransactionFilter filter);

    QueryPage<AgentQueryModels.AgentActivityItem> listActivities(ActorContext actorContext, AgentQueryModels.AgentActivityFilter filter);
}

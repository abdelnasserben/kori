package com.kori.application.port.in.query;

import com.kori.application.query.QueryPage;
import com.kori.application.query.model.AgentQueryModels;
import com.kori.application.security.ActorContext;

public interface AgentMeQueryUseCase {
    AgentQueryModels.AgentSummary getSummary(ActorContext actorContext);

    QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(ActorContext actorContext, AgentQueryModels.AgentTransactionFilter filter);

    QueryPage<AgentQueryModels.AgentActivityItem> listActivities(ActorContext actorContext, AgentQueryModels.AgentActivityFilter filter);
}

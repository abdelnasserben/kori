package com.kori.application.port.out.query;

import com.kori.application.query.QueryPage;
import com.kori.application.query.model.AgentQueryModels;

import java.util.Optional;

public interface AgentMeReadPort {
    Optional<AgentQueryModels.AgentSummary> findSummary(String agentId);

    QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(String agentId, AgentQueryModels.AgentTransactionFilter filter);

    QueryPage<AgentQueryModels.AgentActivityItem> listActivities(String agentId, AgentQueryModels.AgentActivityFilter filter);
}

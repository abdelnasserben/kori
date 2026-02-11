package com.kori.query.port.out;

import com.kori.query.model.QueryPage;
import com.kori.query.model.me.AgentQueryModels;

import java.util.Optional;

public interface AgentMeReadPort {
    Optional<AgentQueryModels.AgentSummary> findSummary(String agentId);

    QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(String agentId, AgentQueryModels.AgentTransactionFilter filter);

    QueryPage<AgentQueryModels.AgentActivityItem> listActivities(String agentId, AgentQueryModels.AgentActivityFilter filter);
}

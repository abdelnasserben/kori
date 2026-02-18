package com.kori.query.port.out;

import com.kori.query.model.QueryPage;
import com.kori.query.model.me.AgentQueryModels;

import java.util.Optional;

public interface AgentMeReadPort {
    Optional<AgentQueryModels.AgentSummary> findSummary(String agentCode);

    QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(String agentCode, AgentQueryModels.AgentTransactionFilter filter);

    QueryPage<AgentQueryModels.AgentActivityItem> listActivities(String agentCode, AgentQueryModels.AgentActivityFilter filter);
}

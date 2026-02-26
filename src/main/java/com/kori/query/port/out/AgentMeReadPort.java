package com.kori.query.port.out;

import com.kori.query.model.QueryPage;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.model.me.MeQueryModels;

import java.util.Optional;

public interface AgentMeReadPort {
    Optional<MeQueryModels.AgentProfile> findProfile(String agentCode);

    MeQueryModels.ActorBalance getBalance(String agentCode);

    QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(String agentCode, AgentQueryModels.AgentTransactionFilter filter);

    Optional<MeQueryModels.AgentTransactionDetails> findTransactionDetailsOwnedByAgent(String agentCode, String transactionRef);

    boolean existsTransaction(String transactionRef);

    QueryPage<AgentQueryModels.AgentActivityItem> listActivities(String agentCode, AgentQueryModels.AgentActivityFilter filter);
}

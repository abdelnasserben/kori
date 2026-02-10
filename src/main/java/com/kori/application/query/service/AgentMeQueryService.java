package com.kori.application.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.query.AgentMeQueryUseCase;
import com.kori.application.port.out.query.AgentMeReadPort;
import com.kori.application.query.QueryPage;
import com.kori.application.query.model.AgentQueryModels;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import org.springframework.stereotype.Service;

@Service
public class AgentMeQueryService implements AgentMeQueryUseCase {

    private final AgentMeReadPort readPort;

    public AgentMeQueryService(AgentMeReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public AgentQueryModels.AgentSummary getSummary(ActorContext actorContext) {
        requireAgent(actorContext);
        return readPort.findSummary(actorContext.actorId())
                .orElseThrow(() -> new NotFoundException("Agent not found"));
    }

    @Override
    public QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(ActorContext actorContext, AgentQueryModels.AgentTransactionFilter filter) {
        requireAgent(actorContext);
        return readPort.listTransactions(actorContext.actorId(), filter);
    }

    @Override
    public QueryPage<AgentQueryModels.AgentActivityItem> listActivities(ActorContext actorContext, AgentQueryModels.AgentActivityFilter filter) {
        requireAgent(actorContext);
        return readPort.listActivities(actorContext.actorId(), filter);
    }

    private void requireAgent(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Forbidden operation");
        }
    }
}

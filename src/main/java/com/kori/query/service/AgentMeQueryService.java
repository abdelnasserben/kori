package com.kori.query.service;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.query.model.QueryPage;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.port.in.AgentMeQueryUseCase;
import com.kori.query.port.out.AgentMeReadPort;
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
        return readPort.findSummary(actorContext.actorRef())
                .orElseThrow(() -> new NotFoundException("Agent not found"));
    }

    @Override
    public QueryPage<AgentQueryModels.AgentTransactionItem> listTransactions(ActorContext actorContext, AgentQueryModels.AgentTransactionFilter filter) {
        requireAgent(actorContext);
        return readPort.listTransactions(actorContext.actorRef(), filter);
    }

    @Override
    public QueryPage<AgentQueryModels.AgentActivityItem> listActivities(ActorContext actorContext, AgentQueryModels.AgentActivityFilter filter) {
        requireAgent(actorContext);
        return readPort.listActivities(actorContext.actorRef(), filter);
    }

    private void requireAgent(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Forbidden operation");
        }
    }
}

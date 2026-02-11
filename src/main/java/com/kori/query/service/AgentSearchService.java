package com.kori.query.service;

import com.kori.adapters.out.jpa.query.common.QueryInputValidator;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.ValidationException;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.port.in.AgentSearchUseCase;
import com.kori.query.port.out.AgentSearchReadPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AgentSearchService implements AgentSearchUseCase {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final AgentSearchReadPort readPort;

    public AgentSearchService(AgentSearchReadPort readPort) {
        this.readPort = readPort;
    }

    @Override
    public List<AgentQueryModels.AgentSearchItem> search(ActorContext actorContext, AgentQueryModels.AgentSearchFilter filter) {
        requireAgent(actorContext);

        int count = notBlank(filter.phone()) + notBlank(filter.cardUid()) + notBlank(filter.terminalUid());
        if (count == 0) {
            throw new ValidationException("One lookup parameter is required", Map.of("field", "phone|cardUid|terminalUid"));
        }
        if (count > 1) {
            throw new ValidationException("Only one lookup parameter is allowed", Map.of("field", "phone|cardUid|terminalUid"));
        }

        int limit = QueryInputValidator.normalizeLimit(filter.limit(), DEFAULT_LIMIT, MAX_LIMIT);
        if (notBlank(filter.phone()) == 1) {
            return readPort.searchByPhone(filter.phone().trim(), limit);
        }
        if (notBlank(filter.cardUid()) == 1) {
            return readPort.searchByCardUid(filter.cardUid().trim(), limit);
        }
        return readPort.searchByTerminalUid(filter.terminalUid().trim(), limit);
    }

    private int notBlank(String value) {
        return value != null && !value.isBlank() ? 1 : 0;
    }

    private void requireAgent(ActorContext actorContext) {
        if (actorContext.actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Forbidden operation");
        }
    }
}

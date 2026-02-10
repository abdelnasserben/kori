package com.kori.application.port.in.query;

import com.kori.application.query.model.AgentQueryModels;
import com.kori.application.security.ActorContext;

import java.util.List;

public interface AgentSearchUseCase {
    List<AgentQueryModels.AgentSearchItem> search(ActorContext actorContext, AgentQueryModels.AgentSearchFilter filter);
}

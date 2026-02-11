package com.kori.query.port.in;

import com.kori.application.security.ActorContext;
import com.kori.query.model.me.AgentQueryModels;

import java.util.List;

public interface AgentSearchUseCase {
    List<AgentQueryModels.AgentSearchItem> search(ActorContext actorContext, AgentQueryModels.AgentSearchFilter filter);
}

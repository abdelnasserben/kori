package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.AgentResponses;
import com.kori.application.security.ActorContext;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.port.in.AgentSearchUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.AGENT_SEARCH)
public class AgentSearchController {

    private final AgentSearchUseCase searchUseCase;

    public AgentSearchController(AgentSearchUseCase searchUseCase) {
        this.searchUseCase = searchUseCase;
    }

    @GetMapping
    public AgentResponses.SearchResponse search(
            ActorContext actorContext,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String cardUid,
            @RequestParam(required = false) String terminalUid,
            @RequestParam(required = false) Integer limit
    ) {
        var items = searchUseCase.search(actorContext, new AgentQueryModels.AgentSearchFilter(phone, cardUid, terminalUid, limit));
        return new AgentResponses.SearchResponse(items.stream()
                .map(item -> new AgentResponses.SearchItem(item.entityType(), item.entityId(), item.display(), item.status(), item.links()))
                .toList());
    }
}

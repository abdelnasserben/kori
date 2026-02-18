package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.AgentResponses;
import com.kori.application.security.ActorContext;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.port.in.AgentMeQueryUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping(ApiPaths.AGENT_ME)
public class AgentMeController {

    private final AgentMeQueryUseCase queryUseCase;

    public AgentMeController(AgentMeQueryUseCase queryUseCase) {
        this.queryUseCase = queryUseCase;
    }

    @GetMapping("/summary")
    public AgentResponses.SummaryResponse summary(ActorContext actorContext) {
        var item = queryUseCase.getSummary(actorContext);
        return new AgentResponses.SummaryResponse(
                item.agentCode(),
                item.code(),
                item.status(),
                item.cashBalance(),
                item.commissionBalance(),
                item.txCount7d()
        );
    }

    @GetMapping("/transactions")
    public AgentResponses.ListResponse<AgentResponses.TransactionItem> transactions(
            ActorContext actorContext,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        var page = queryUseCase.listTransactions(actorContext, new AgentQueryModels.AgentTransactionFilter(type, status, from, to, min, max, limit, cursor, sort));
        return new AgentResponses.ListResponse<>(
                page.items().stream()
                        .map(item -> new AgentResponses.TransactionItem(item.transactionRef(), item.type(), item.status(), item.amount(), item.currency(), item.createdAt()))
                        .toList(),
                new AgentResponses.CursorPage(page.nextCursor(), page.hasMore())
        );
    }

    @GetMapping("/activities")
    public AgentResponses.ListResponse<AgentResponses.ActivityItem> activities(
            ActorContext actorContext,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        var page = queryUseCase.listActivities(actorContext, new AgentQueryModels.AgentActivityFilter(action, from, to, limit, cursor, sort));
        return new AgentResponses.ListResponse<>(
                page.items().stream()
                        .map(item -> new AgentResponses.ActivityItem(item.eventRef(), item.occurredAt(), item.action(), item.resourceType(), item.resourceRef(), item.metadata()))
                        .toList(),
                new AgentResponses.CursorPage(page.nextCursor(), page.hasMore())
        );
    }
}

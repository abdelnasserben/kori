package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.AgentResponses;
import com.kori.adapters.in.rest.dto.MeResponses;
import com.kori.application.security.ActorContext;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.AgentMeQueryUseCase;
import com.kori.query.service.DashboardQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.AGENT_ME)
public class AgentMeController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final AgentMeQueryUseCase queryUseCase;
    private final DashboardQueryService dashboardQueryService;

    public AgentMeController(AgentMeQueryUseCase queryUseCase, DashboardQueryService dashboardQueryService) {
        this.queryUseCase = queryUseCase;
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/profile")
    public MeResponses.AgentProfileResponse profile(ActorContext actorContext) {
        var item = queryUseCase.getProfile(actorContext);
        return new MeResponses.AgentProfileResponse(item.code(), item.status(), item.createdAt());
    }

    @GetMapping("/balance")
    public MeResponses.ActorBalanceResponse balance(ActorContext actorContext) {
        return toBalanceResponse(queryUseCase.getBalance(actorContext));
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

    @GetMapping("/transactions/{transactionRef}")
    public MeResponses.TransactionDetailsResponse transactionDetails(ActorContext actorContext, @PathVariable String transactionRef) {
        var d = queryUseCase.getTransactionDetails(actorContext, transactionRef);
        return new MeResponses.TransactionDetailsResponse(
                d.transactionRef(), d.type(), d.status(), d.amount(), d.fee(), d.totalDebited(), d.currency(),
                d.clientCode(), d.merchantCode(), actorContext.actorRef(), d.terminalUid(), d.originalTransactionRef(), d.createdAt()
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

    @GetMapping("/dashboard")
    public MeResponses.AgentDashboardResponse dashboard(ActorContext actorContext) {
        var recentTxPage = queryUseCase.listTransactions(actorContext, new AgentQueryModels.AgentTransactionFilter(null, null, null, null, null, null, DEFAULT_PAGE_SIZE, null, "createdAt:desc"));
        var tx7d = queryUseCase.listTransactions(actorContext, new AgentQueryModels.AgentTransactionFilter(null, null, Instant.now().minus(7, ChronoUnit.DAYS), Instant.now(), null, null, DEFAULT_PAGE_SIZE, null, "createdAt:desc"));
        var kpis = dashboardQueryService.computeKpis7dFromTransactions(tx7d.items().stream().map(i -> new MeQueryModels.MeTransactionItem(i.transactionRef(), i.type(), i.status(), i.amount(), i.currency(), i.createdAt())).toList());
        var recentActivities = activities(actorContext, null, null, null, DEFAULT_PAGE_SIZE, null, "occurredAt:desc").items();
        List<MeResponses.AlertItem> alerts = kpis.failedCount() >= 3
                ? List.of(new MeResponses.AlertItem("FAILED_TX_SPIKE", "Plusieurs transactions en échec sur 7 jours."))
                : List.of();
        return new MeResponses.AgentDashboardResponse(
                profile(actorContext),
                balance(actorContext),
                new MeResponses.Kpis7dResponse(kpis.txCount(), kpis.txVolume(), kpis.failedCount()),
                recentTxPage.items().stream().map(item -> new MeResponses.TransactionItem(item.transactionRef(), item.type(), item.status(), item.amount(), item.currency(), item.createdAt())).toList(),
                recentActivities,
                alerts
        );
    }

    private MeResponses.ActorBalanceResponse toBalanceResponse(MeQueryModels.ActorBalance balance) {
        return new MeResponses.ActorBalanceResponse(
                balance.ownerRef(),
                balance.currency(),
                balance.balances().stream().map(i -> new MeResponses.BalanceItemResponse(i.kind(), i.amount())).toList()
        );
    }
}

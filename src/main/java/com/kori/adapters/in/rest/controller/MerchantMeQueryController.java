package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.MeResponses;
import com.kori.application.security.ActorContext;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.MerchantMeQueryUseCase;
import com.kori.query.port.in.MerchantMeTxDetailQueryUseCase;
import com.kori.query.service.DashboardQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiPaths.MERCHANT_ME)
public class MerchantMeQueryController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final MerchantMeQueryUseCase merchantMeQueryUseCase;
    private final MerchantMeTxDetailQueryUseCase merchantMeTxDetailQueryUseCase;
    private final DashboardQueryService dashboardQueryService;

    public MerchantMeQueryController(MerchantMeQueryUseCase merchantMeQueryUseCase, MerchantMeTxDetailQueryUseCase merchantMeTxDetailQueryUseCase, DashboardQueryService dashboardQueryService) {
        this.merchantMeQueryUseCase = merchantMeQueryUseCase;
        this.merchantMeTxDetailQueryUseCase = merchantMeTxDetailQueryUseCase;
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/profile")
    public MeResponses.MerchantProfileResponse profile(ActorContext actorContext) {
        var item = merchantMeQueryUseCase.getProfile(actorContext);
        return new MeResponses.MerchantProfileResponse(item.code(), item.status(), item.createdAt());
    }

    @GetMapping("/balance")
    public MeResponses.ActorBalanceResponse balance(ActorContext actorContext) {
        return toBalanceResponse(merchantMeQueryUseCase.getBalance(actorContext));
    }

    @GetMapping("/transactions")
    public MeResponses.ListResponse<MeResponses.TransactionItem> transactions(
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
        var page = merchantMeQueryUseCase.listTransactions(actorContext, new MeQueryModels.MeTransactionsFilter(type, status, from, to, min, max, limit, cursor, sort));
        return new MeResponses.ListResponse<>(
                page.items().stream().map(i -> new MeResponses.TransactionItem(i.transactionRef(), i.type(), i.status(), i.amount(), i.currency(), i.createdAt())).toList(),
                new MeResponses.CursorPage(page.nextCursor(), page.hasMore())
        );
    }

    @GetMapping("/transactions/{transactionRef}")
    public MeResponses.TransactionDetailsResponse transactionDetails(ActorContext actorContext, @PathVariable String transactionRef) {
        var d = merchantMeTxDetailQueryUseCase.getByRef(actorContext, transactionRef);
        return new MeResponses.TransactionDetailsResponse(
                d.transactionRef(), d.type(), d.status(), d.amount(), d.fee(), d.totalDebited(), d.currency(),
                d.clientCode(), null, d.agentCode(), null, d.originalTransactionRef(), d.createdAt()
        );
    }

    @GetMapping("/terminals")
    public MeResponses.ListResponse<MeResponses.TerminalItem> terminals(
            ActorContext actorContext,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String terminalUid,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        var page = merchantMeQueryUseCase.listTerminals(actorContext, new MeQueryModels.MeTerminalsFilter(status, terminalUid, limit, cursor, sort));
        return new MeResponses.ListResponse<>(
                page.items().stream().map(i -> new MeResponses.TerminalItem(i.terminalUid(), i.status(), i.createdAt(), i.lastSeen(), i.merchantCode())).toList(),
                new MeResponses.CursorPage(page.nextCursor(), page.hasMore())
        );
    }

    @GetMapping("/terminals/{terminalUid}")
    public MeResponses.TerminalItem terminalDetails(ActorContext actorContext, @PathVariable String terminalUid) {
        var item = merchantMeQueryUseCase.getTerminalDetails(actorContext, terminalUid);
        return new MeResponses.TerminalItem(item.terminalUid(), item.status(), item.createdAt(), item.lastSeen(), item.merchantCode());
    }

    @GetMapping("/dashboard")
    public MeResponses.MerchantDashboardResponse dashboard(ActorContext actorContext) {
        var recentTx = merchantMeQueryUseCase.listTransactions(actorContext, new MeQueryModels.MeTransactionsFilter(null, null, null, null, null, null, DEFAULT_PAGE_SIZE, null, "createdAt:desc")).items();
        var tx7d = merchantMeQueryUseCase.listTransactions(actorContext, new MeQueryModels.MeTransactionsFilter(null, null, Instant.now().minus(7, ChronoUnit.DAYS), Instant.now(), null, null, DEFAULT_PAGE_SIZE, null, "createdAt:desc")).items();
        var terminals = merchantMeQueryUseCase.listTerminals(actorContext, new MeQueryModels.MeTerminalsFilter(null, null, DEFAULT_PAGE_SIZE, null, "createdAt:desc")).items();
        Map<String, Long> byStatus = terminals.stream().collect(Collectors.groupingBy(MeQueryModels.MeTerminalItem::status, Collectors.counting()));
        long stale = terminals.stream().filter(t -> t.lastSeen() != null && t.lastSeen().isBefore(Instant.now().minus(30, ChronoUnit.DAYS))).count();

        var kpis = dashboardQueryService.computeKpis7dFromTransactions(tx7d);
        return new MeResponses.MerchantDashboardResponse(
                profile(actorContext),
                balance(actorContext),
                new MeResponses.Kpis7dResponse(kpis.txCount(), kpis.txVolume(), kpis.failedCount()),
                recentTx.stream().map(i -> new MeResponses.TransactionItem(i.transactionRef(), i.type(), i.status(), i.amount(), i.currency(), i.createdAt())).toList(),
                new MeResponses.TerminalsSummaryResponse((long) terminals.size(), byStatus, stale)
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

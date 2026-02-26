package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.MeResponses;
import com.kori.application.security.ActorContext;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.ClientMeQueryUseCase;
import com.kori.query.port.in.ClientMeTxDetailQueryUseCase;
import com.kori.query.service.DashboardQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.CLIENT_ME)
public class ClientMeQueryController {

    private final ClientMeQueryUseCase clientMeQueryUseCase;
    private final ClientMeTxDetailQueryUseCase clientMeTxDetailQueryUseCase;

    public ClientMeQueryController(ClientMeQueryUseCase clientMeQueryUseCase, ClientMeTxDetailQueryUseCase clientMeTxDetailQueryUseCase, DashboardQueryService dashboardQueryService) {
        this.clientMeQueryUseCase = clientMeQueryUseCase;
        this.clientMeTxDetailQueryUseCase = clientMeTxDetailQueryUseCase;
    }

    @GetMapping("/profile")
    public MeResponses.ClientProfileResponse profile(ActorContext actorContext) {
        var item = clientMeQueryUseCase.getProfile(actorContext);
        return new MeResponses.ClientProfileResponse(item.code(), item.phone(), item.status(), item.createdAt());
    }

    @GetMapping("/balance")
    public MeResponses.ActorBalanceResponse balance(ActorContext actorContext) {
        return toBalanceResponse(clientMeQueryUseCase.getBalance(actorContext));
    }

    @GetMapping("/cards")
    public java.util.List<MeResponses.CardItem> cards(ActorContext actorContext) {
        return clientMeQueryUseCase.listCards(actorContext).stream()
                .map(i -> new MeResponses.CardItem(i.cardUid(), i.status(), i.createdAt()))
                .toList();
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
        var page = clientMeQueryUseCase.listTransactions(actorContext, new MeQueryModels.MeTransactionsFilter(type, status, from, to, min, max, limit, cursor, sort));
        return new MeResponses.ListResponse<>(
                page.items().stream().map(i -> new MeResponses.TransactionItem(i.transactionRef(), i.type(), i.status(), i.amount(), i.currency(), i.createdAt())).toList(),
                new MeResponses.CursorPage(page.nextCursor(), page.hasMore())
        );
    }

    @GetMapping("/transactions/{transactionRef}")
    public MeResponses.TransactionDetailsResponse transactionDetails(
            ActorContext actorContext,
            @PathVariable String transactionRef) {
        var d = clientMeTxDetailQueryUseCase.getByRef(actorContext, transactionRef);
        return new MeResponses.TransactionDetailsResponse(
                d.transactionRef(), d.type(), d.status(), d.amount(), d.fee(), d.totalDebited(), d.currency(),
                null, d.merchantCode(), null, null, d.originalTransactionRef(), d.createdAt()
        );
    }

    @GetMapping("/dashboard")
    public MeResponses.ClientDashboardResponse dashboard(ActorContext actorContext) {
        var profile = profile(actorContext);
        var balance = balance(actorContext);
        var cards = cards(actorContext).stream().limit(5).toList();
        var recentTransactions = clientMeQueryUseCase.listTransactions(actorContext, new MeQueryModels.MeTransactionsFilter(null, null, null, null, null, null, 10, null, "createdAt:desc"))
                .items().stream().map(i -> new MeResponses.TransactionItem(i.transactionRef(), i.type(), i.status(), i.amount(), i.currency(), i.createdAt())).toList();
        var alerts = new ArrayList<MeResponses.AlertItem>();
        if (cards.stream().anyMatch(c -> !"ACTIVE".equals(c.status()))) {
            alerts.add(new MeResponses.AlertItem("CARD_INACTIVE", "Une ou plusieurs cartes ne sont pas actives."));
        }
        var failed7d = clientMeQueryUseCase.listTransactions(actorContext, new MeQueryModels.MeTransactionsFilter(null, "FAILED", Instant.now().minus(7, ChronoUnit.DAYS), Instant.now(), null, null, 10, null, "createdAt:desc"));
        if (!failed7d.items().isEmpty()) {
            alerts.add(new MeResponses.AlertItem("FAILED_TX_7D", "Des transactions ont échoué dans les 7 derniers jours."));
        }
        return new MeResponses.ClientDashboardResponse(profile, balance, cards, recentTransactions, alerts.stream().limit(5).toList());
    }

    private MeResponses.ActorBalanceResponse toBalanceResponse(MeQueryModels.ActorBalance balance) {
        return new MeResponses.ActorBalanceResponse(
                balance.ownerRef(),
                balance.currency(),
                balance.balances().stream().map(i -> new MeResponses.BalanceItemResponse(i.kind(), i.amount())).toList()
        );
    }
}

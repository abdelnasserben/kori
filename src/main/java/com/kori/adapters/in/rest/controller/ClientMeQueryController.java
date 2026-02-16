package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.MeResponses;
import com.kori.application.security.ActorContext;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.ClientMeQueryUseCase;
import com.kori.query.port.in.ClientMeTxDetailQueryUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping(ApiPaths.CLIENT_ME)
public class ClientMeQueryController {

    private final ClientMeQueryUseCase clientMeQueryUseCase;
    private final ClientMeTxDetailQueryUseCase clientMeTxDetailQueryUseCase;

    public ClientMeQueryController(ClientMeQueryUseCase clientMeQueryUseCase, ClientMeTxDetailQueryUseCase clientMeTxDetailQueryUseCase) {
        this.clientMeQueryUseCase = clientMeQueryUseCase;
        this.clientMeTxDetailQueryUseCase = clientMeTxDetailQueryUseCase;
    }

    @GetMapping("/home")
    public MeResponses.ListResponse<MeResponses.TransactionItem> home(ActorContext actorContext) {
        var page = clientMeQueryUseCase.listTransactions(actorContext, new MeQueryModels.MeTransactionsFilter(null, null, null, null, null, null, 10, null, "createdAt:desc"));
        return new MeResponses.ListResponse<>(
                page.items().stream().map(i -> new MeResponses.TransactionItem(i.transactionId(), i.type(), i.status(), i.amount(), i.currency(), i.createdAt())).toList(),
                new MeResponses.CursorPage(page.nextCursor(), page.hasMore())
        );
    }

    @GetMapping("/profile")
    public MeResponses.ProfileResponse profile(ActorContext actorContext) {
        var item = clientMeQueryUseCase.getProfile(actorContext);
        return new MeResponses.ProfileResponse(item.actorRef(), item.code(), item.status(), item.createdAt());
    }

    @GetMapping("/balance")
    public MeResponses.BalanceResponse balance(ActorContext actorContext) {
        var item = clientMeQueryUseCase.getBalance(actorContext);
        return new MeResponses.BalanceResponse(item.accountType(), item.ownerRef(), item.balance(), item.currency());
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
                page.items().stream().map(i -> new MeResponses.TransactionItem(i.transactionId(), i.type(), i.status(), i.amount(), i.currency(), i.createdAt())).toList(),
                new MeResponses.CursorPage(page.nextCursor(), page.hasMore())
        );
    }

    @GetMapping("/transactions/{transactionRef}")
    public MeResponses.ClientTransactionDetailsResponse transactionDetails(
            ActorContext actorContext,
            @PathVariable String transactionRef) {
        var d = clientMeTxDetailQueryUseCase.getById(actorContext, transactionRef);
        return new MeResponses.ClientTransactionDetailsResponse(
                d.transactionId(),
                d.type(),
                d.status(),
                d.amount(),
                d.fee(),
                d.totalDebited(),
                d.currency(),
                d.merchantCode(),
                d.originalTransactionId(),
                d.createdAt()
        );
    }
}

package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.MeResponses;
import com.kori.application.port.in.query.MerchantMeQueryUseCase;
import com.kori.application.port.in.query.MerchantMeTxDetailQueryUseCase;
import com.kori.application.query.model.MeQueryModels;
import com.kori.application.security.ActorContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping(ApiPaths.MERCHANT_ME)
public class MerchantMeQueryController {

    private final MerchantMeQueryUseCase merchantMeQueryUseCase;
    private final MerchantMeTxDetailQueryUseCase merchantMeTxDetailQueryUseCase;

    public MerchantMeQueryController(MerchantMeQueryUseCase merchantMeQueryUseCase, MerchantMeTxDetailQueryUseCase merchantMeTxDetailQueryUseCase) {
        this.merchantMeQueryUseCase = merchantMeQueryUseCase;
        this.merchantMeTxDetailQueryUseCase = merchantMeTxDetailQueryUseCase;
    }

    @GetMapping("/profile")
    public MeResponses.ProfileResponse profile(ActorContext actorContext) {
        var item = merchantMeQueryUseCase.getProfile(actorContext);
        return new MeResponses.ProfileResponse(item.actorId(), item.code(), item.status(), item.createdAt());
    }

    @GetMapping("/balance")
    public MeResponses.BalanceResponse balance(ActorContext actorContext) {
        var item = merchantMeQueryUseCase.getBalance(actorContext);
        return new MeResponses.BalanceResponse(item.accountType(), item.ownerRef(), item.balance(), item.currency());
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
                page.items().stream().map(i -> new MeResponses.TransactionItem(i.transactionId(), i.type(), i.status(), i.amount(), i.currency(), i.createdAt())).toList(),
                new MeResponses.CursorPage(page.nextCursor(), page.hasMore())
        );
    }

    @GetMapping("/transactions/{transactionId}")
    public MeResponses.MerchantTransactionDetailsResponse transactionDetails(ActorContext actorContext,
                                                                             @PathVariable String transactionId) {
        var d = merchantMeTxDetailQueryUseCase.getById(actorContext, transactionId);
        return new MeResponses.MerchantTransactionDetailsResponse(
                d.transactionId(),
                d.type(),
                d.status(),
                d.amount(),
                d.currency(),
                d.agentCode(),
                d.clientId(),
                d.originalTransactionId(),
                d.createdAt()
        );
    }

    @GetMapping("/terminals")
    public MeResponses.ListResponse<MeResponses.TerminalItem> terminals(
            ActorContext actorContext,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        var page = merchantMeQueryUseCase.listTerminals(actorContext, new MeQueryModels.MeTerminalsFilter(status, query, limit, cursor, sort));
        return new MeResponses.ListResponse<>(
                page.items().stream().map(i -> new MeResponses.TerminalItem(i.terminalUid(), i.status(), i.createdAt(), i.lastSeen(), i.merchantId())).toList(),
                new MeResponses.CursorPage(page.nextCursor(), page.hasMore())
        );
    }

    @GetMapping("/terminals/{terminalUid}")
    public MeResponses.TerminalItem terminalDetails(ActorContext actorContext, @PathVariable String terminalUid) {
        var item = merchantMeQueryUseCase.getTerminalDetails(actorContext, terminalUid);
        return new MeResponses.TerminalItem(item.terminalUid(), item.status(), item.createdAt(), item.lastSeen(), item.merchantId());
    }
}

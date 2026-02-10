package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.BackofficeResponses;
import com.kori.application.port.in.query.BackofficeActorQueryUseCase;
import com.kori.application.port.in.query.BackofficeAuditEventQueryUseCase;
import com.kori.application.port.in.query.BackofficeTransactionQueryUseCase;
import com.kori.application.query.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
@RequestMapping(ApiPaths.BACKOFFICE)
public class BackofficeQueryController {

    private final BackofficeTransactionQueryUseCase transactionQueryUseCase;
    private final BackofficeAuditEventQueryUseCase auditEventQueryUseCase;
    private final BackofficeActorQueryUseCase actorQueryUseCase;

    public BackofficeQueryController(BackofficeTransactionQueryUseCase transactionQueryUseCase,
                                     BackofficeAuditEventQueryUseCase auditEventQueryUseCase,
                                     BackofficeActorQueryUseCase actorQueryUseCase) {
        this.transactionQueryUseCase = transactionQueryUseCase;
        this.auditEventQueryUseCase = auditEventQueryUseCase;
        this.actorQueryUseCase = actorQueryUseCase;
    }

    @GetMapping("/transactions")
    public BackofficeResponses.ListResponse<BackofficeResponses.TransactionItem> listTransactions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        var result = transactionQueryUseCase.list(new BackofficeTransactionQuery(query, type, status, actorType, actorId, from, to, min, max, limit, cursor, sort));
        return new BackofficeResponses.ListResponse<>(
                result.items().stream().map(i -> new BackofficeResponses.TransactionItem(i.transactionId(), i.type(), i.status(), i.amount(), i.currency(), i.merchantCode(), i.agentCode(), i.clientId(), i.createdAt())).toList(),
                new BackofficeResponses.CursorPage(result.nextCursor(), result.hasMore())
        );
    }

    @GetMapping("/transactions/{transactionId}")
    public BackofficeResponses.TransactionDetails getTransaction(@PathVariable String transactionId) {
        var d = transactionQueryUseCase.getById(transactionId);
        return new BackofficeResponses.TransactionDetails(d.transactionId(), d.type(), d.status(), d.amount(), d.currency(), d.merchantCode(), d.agentCode(), d.clientId(), d.originalTransactionId(), d.createdAt());
    }

    @GetMapping("/audit-events")
    public BackofficeResponses.ListResponse<BackofficeResponses.AuditEventItem> listAuditEvents(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        var result = auditEventQueryUseCase.list(new BackofficeAuditEventQuery(action, actorType, actorId, from, to, limit, cursor, sort));
        return new BackofficeResponses.ListResponse<>(
                result.items().stream().map(i -> new BackofficeResponses.AuditEventItem(i.eventId(), i.occurredAt(), i.actorType(), i.actorId(), i.action(), i.resourceType(), i.resourceId(), i.metadata())).toList(),
                new BackofficeResponses.CursorPage(result.nextCursor(), result.hasMore())
        );
    }

    @GetMapping("/agents")
    public BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> listAgents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listAgents(new BackofficeActorQuery(query, status, createdFrom, createdTo, limit, cursor, sort)));
    }

    @GetMapping("/clients")
    public BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> listClients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listClients(new BackofficeActorQuery(query, status, createdFrom, createdTo, limit, cursor, sort)));
    }

    @GetMapping("/merchants")
    public BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> listMerchants(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listMerchants(new BackofficeActorQuery(query, status, createdFrom, createdTo, limit, cursor, sort)));
    }

    private BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> toActorResponse(QueryPage<BackofficeActorItem> result) {
        return new BackofficeResponses.ListResponse<>(
                result.items().stream().map(i -> new BackofficeResponses.ActorItem(i.actorId(), i.code(), i.status(), i.createdAt())).toList(),
                new BackofficeResponses.CursorPage(result.nextCursor(), result.hasMore())
        );
    }
}

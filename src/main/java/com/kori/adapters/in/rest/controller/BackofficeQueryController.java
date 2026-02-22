package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.BackofficeResponses;
import com.kori.query.model.*;
import com.kori.query.port.in.*;
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
    private final BackofficeActorDetailQueryUseCase actorDetailQueryUseCase;
    private final BackofficeLookupQueryUseCase lookupQueryUseCase;

    public BackofficeQueryController(BackofficeTransactionQueryUseCase transactionQueryUseCase,
                                     BackofficeAuditEventQueryUseCase auditEventQueryUseCase,
                                     BackofficeActorQueryUseCase actorQueryUseCase, BackofficeActorDetailQueryUseCase actorDetailQueryUseCase, BackofficeLookupQueryUseCase lookupQueryUseCase) {
        this.transactionQueryUseCase = transactionQueryUseCase;
        this.auditEventQueryUseCase = auditEventQueryUseCase;
        this.actorQueryUseCase = actorQueryUseCase;
        this.actorDetailQueryUseCase = actorDetailQueryUseCase;
        this.lookupQueryUseCase = lookupQueryUseCase;
    }

    @GetMapping("/transactions")
    public BackofficeResponses.ListResponse<BackofficeResponses.TransactionItem> listTransactions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String actorRef,
            @RequestParam(required = false) String terminalUid,
            @RequestParam(required = false) String cardUid,
            @RequestParam(required = false) String merchantCode,
            @RequestParam(required = false) String agentCode,
            @RequestParam(required = false) String clientPhone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        var result = transactionQueryUseCase.list(new BackofficeTransactionQuery(
                query,
                type,
                status,
                actorType,
                actorRef,
                terminalUid,
                cardUid,
                merchantCode,
                agentCode,
                clientPhone,
                from,
                to,
                min,
                max,
                limit,
                cursor,
                sort
        ));
        return new BackofficeResponses.ListResponse<>(
                result.items().stream().map(i -> new BackofficeResponses.TransactionItem(i.transactionRef(), i.type(), i.status(), i.amount(), i.currency(), i.merchantCode(), i.agentCode(), i.clientCode(), i.createdAt())).toList(),
                new BackofficeResponses.CursorPage(result.nextCursor(), result.hasMore())
        );
    }

    @GetMapping("/transactions/{transactionRef}")
    public BackofficeResponses.TransactionDetails getTransaction(@PathVariable String transactionRef) {
        var d = transactionQueryUseCase.getByRef(transactionRef);
        return new BackofficeResponses.TransactionDetails(
                d.transactionRef(),
                d.type(),
                d.status(),
                d.amount(),
                d.currency(),
                d.merchantCode(),
                d.agentCode(),
                d.clientCode(),
                d.clientPhone(),
                d.terminalUid(),
                d.cardUid(),
                d.originalTransactionRef(),
                d.payout() == null ? null : new BackofficeResponses.TransactionPayout(d.payout().payoutRef(), d.payout().status(), d.payout().amount(), d.payout().createdAt(), d.payout().completedAt(), d.payout().failedAt(), d.payout().failureReason()),
                d.clientRefund() == null ? null : new BackofficeResponses.TransactionClientRefund(d.clientRefund().refundRef(), d.clientRefund().status(), d.clientRefund().amount(), d.clientRefund().createdAt(), d.clientRefund().completedAt(), d.clientRefund().failedAt(), d.clientRefund().failureReason()),
                d.ledgerLines().stream().map(l -> new BackofficeResponses.TransactionLedgerLine(l.accountType(), l.ownerRef(), l.entryType(), l.amount(), l.currency())).toList(),
                d.createdAt()
        );
    }

    @GetMapping("/audit-events")
    public BackofficeResponses.ListResponse<BackofficeResponses.AuditEventItem> listAuditEvents(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String actorRef,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceRef,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        var result = auditEventQueryUseCase.list(
                new BackofficeAuditEventQuery(
                        action,
                        actorType,
                        actorRef,
                        resourceType,
                        resourceRef,
                        from,
                        to,
                        limit,
                        cursor,
                        sort
                )
        );

        return new BackofficeResponses.ListResponse<>(
                result.items().stream().map(i ->
                        new BackofficeResponses.AuditEventItem(
                                i.eventRef(),
                                i.occurredAt(),
                                i.actorType(),
                                i.actorRef(),
                                i.action(),
                                i.resourceType(),
                                i.resourceRef(),
                                i.metadata())).toList(),
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

    @GetMapping("/terminals")
    public BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> listTerminals(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listTerminals(new BackofficeActorQuery(query, status, createdFrom, createdTo, limit, cursor, sort)));
    }

    @GetMapping("/admins")
    public BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> listAdmins(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listAdmins(new BackofficeActorQuery(query, status, createdFrom, createdTo, limit, cursor, sort)));
    }

    @GetMapping("/actors/{actorType}/{actorRef}")
    public BackofficeResponses.ActorDetails getActor(@PathVariable String actorType, @PathVariable String actorRef) {
        var d = switch (actorType.toUpperCase()) {
            case "AGENT" -> actorDetailQueryUseCase.getAgentByRef(actorRef);
            case "CLIENT" -> actorDetailQueryUseCase.getClientByRef(actorRef);
            case "MERCHANT" -> actorDetailQueryUseCase.getMerchantByRef(actorRef);
            case "TERMINAL" -> actorDetailQueryUseCase.getTerminalByRef(actorRef);
            case "ADMIN" -> actorDetailQueryUseCase.getAdminByRef(actorRef);
            default -> throw new IllegalArgumentException("Unsupported actorType");
        };
        return new BackofficeResponses.ActorDetails(
                d.actorRef(),
                d.display(),
                d.status(),
                d.createdAt(),
                d.lastActivityAt()
        );
    }

    @GetMapping("/lookups")
    public BackofficeResponses.ListResponse<BackofficeResponses.LookupItem> lookups(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer limit
    ) {
        var results = lookupQueryUseCase.search(new BackofficeLookupQuery(q, type, limit));
        return new BackofficeResponses.ListResponse<>(
                results.stream().map(i -> new BackofficeResponses.LookupItem(
                        i.entityType(),
                        i.entityRef(),
                        i.display(),
                        i.status(),
                        null
                )).toList(),
                new BackofficeResponses.CursorPage(null, false)
        );
    }

    private BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> toActorResponse(QueryPage<BackofficeActorItem> result) {
        return new BackofficeResponses.ListResponse<>(
                result.items().stream().map(i ->
                        new BackofficeResponses.ActorItem(i.actorRef(), i.status(), i.createdAt())).toList(),
                new BackofficeResponses.CursorPage(result.nextCursor(), result.hasMore())
        );
    }
}

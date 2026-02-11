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
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String agentId
    ) {
        var resolvedActorRef = firstNonBlank(actorRef, actorId);
        var resolvedMerchantCode = firstNonBlank(merchantCode, merchantId);
        var resolvedAgentCode = firstNonBlank(agentCode, agentId);
        var result = transactionQueryUseCase.list(new BackofficeTransactionQuery(
                query,
                type,
                status,
                actorType,
                resolvedActorRef,
                terminalUid,
                cardUid,
                resolvedMerchantCode,
                resolvedAgentCode,
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
                result.items().stream().map(i -> new BackofficeResponses.TransactionItem(i.transactionId(), i.type(), i.status(), i.amount(), i.currency(), i.merchantCode(), i.agentCode(), i.clientId(), i.createdAt())).toList(),
                new BackofficeResponses.CursorPage(result.nextCursor(), result.hasMore())
        );
    }

    @GetMapping("/transactions/{transactionId}")
    public BackofficeResponses.TransactionDetails getTransaction(@PathVariable String transactionId) {
        var d = transactionQueryUseCase.getById(transactionId);
        return new BackofficeResponses.TransactionDetails(
                d.transactionId(),
                d.type(),
                d.status(),
                d.amount(),
                d.currency(),
                d.merchantCode(),
                d.agentCode(),
                d.clientId(),
                d.clientPhone(),
                d.merchantId(),
                d.agentId(),
                d.terminalUid(),
                d.cardUid(),
                d.originalTransactionId(),
                d.payout() == null ? null : new BackofficeResponses.TransactionPayout(d.payout().payoutId(), d.payout().status(), d.payout().amount(), d.payout().createdAt(), d.payout().completedAt(), d.payout().failedAt(), d.payout().failureReason()),
                d.clientRefund() == null ? null : new BackofficeResponses.TransactionClientRefund(d.clientRefund().refundId(), d.clientRefund().status(), d.clientRefund().amount(), d.clientRefund().createdAt(), d.clientRefund().completedAt(), d.clientRefund().failedAt(), d.clientRefund().failureReason()),
                d.ledgerLines().stream().map(l -> new BackofficeResponses.TransactionLedgerLine(l.accountType(), l.ownerRef(), l.entryType(), l.amount(), l.currency())).toList(),
                d.createdAt()
        );
    }

    @GetMapping("/audit-events")
    public BackofficeResponses.ListResponse<BackofficeResponses.AuditEventItem> listAuditEvents(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        var result = auditEventQueryUseCase.list(new BackofficeAuditEventQuery(action, actorType, actorId, resourceType, resourceId, from, to, limit, cursor, sort));
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

    @GetMapping("/agents/{agentId}")
    public BackofficeResponses.ActorDetails getAgent(@PathVariable String agentId) {
        var d = actorDetailQueryUseCase.getAgentById(agentId);
        return new BackofficeResponses.ActorDetails(d.actorId(), d.display(), d.status(), d.createdAt(), d.lastActivityAt());
    }

    @GetMapping("/clients/{clientId}")
    public BackofficeResponses.ActorDetails getClient(@PathVariable String clientId) {
        var d = actorDetailQueryUseCase.getClientById(clientId);
        return new BackofficeResponses.ActorDetails(d.actorId(), d.display(), d.status(), d.createdAt(), d.lastActivityAt());
    }

    @GetMapping("/merchants/{merchantId}")
    public BackofficeResponses.ActorDetails getMerchant(@PathVariable String merchantId) {
        var d = actorDetailQueryUseCase.getMerchantById(merchantId);
        return new BackofficeResponses.ActorDetails(d.actorId(), d.display(), d.status(), d.createdAt(), d.lastActivityAt());
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
                        i.entityId(),
                        i.display(),
                        i.status(),
                        toLookupDetailUrl(i.entityType(), i.entityId())
                )).toList(),
                new BackofficeResponses.CursorPage(null, false)
        );
    }

    private String toLookupDetailUrl(String entityType, String entityId) {
        return switch (entityType) {
            case "AGENT" -> ApiPaths.BACKOFFICE_AGENTS + "/" + entityId;
            case "CLIENT" -> ApiPaths.BACKOFFICE_CLIENTS + "/" + entityId;
            case "MERCHANT" -> ApiPaths.BACKOFFICE_MERCHANTS + "/" + entityId;
            case "TRANSACTION" -> ApiPaths.BACKOFFICE_TRANSACTIONS + "/" + entityId;
            default -> null;
        };
    }

    private BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> toActorResponse(QueryPage<BackofficeActorItem> result) {
        return new BackofficeResponses.ListResponse<>(
                result.items().stream().map(i -> new BackofficeResponses.ActorItem(i.actorId(), i.code(), i.status(), i.createdAt())).toList(),
                new BackofficeResponses.CursorPage(result.nextCursor(), result.hasMore())
        );
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return (fallback != null && !fallback.isBlank()) ? fallback : null;
    }
}

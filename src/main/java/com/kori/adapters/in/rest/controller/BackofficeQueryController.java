package com.kori.adapters.in.rest.controller;

import com.kori.adapters.in.rest.ApiPaths;
import com.kori.adapters.in.rest.dto.BackofficeResponses;
import com.kori.adapters.in.rest.dto.QueryFiltersEnums.BackofficeActorTypeFilter;
import com.kori.adapters.in.rest.dto.QueryFiltersEnums.LookupType;
import com.kori.adapters.in.rest.dto.QueryFiltersEnums.TransactionStatusFilter;
import com.kori.application.security.ActorContext;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.transaction.TransactionType;
import com.kori.query.model.*;
import com.kori.query.port.in.*;
import com.kori.query.service.DashboardQueryService;
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
    private final DashboardQueryService dashboardQueryService;

    public BackofficeQueryController(BackofficeTransactionQueryUseCase transactionQueryUseCase,
                                     BackofficeAuditEventQueryUseCase auditEventQueryUseCase,
                                     BackofficeActorQueryUseCase actorQueryUseCase, BackofficeActorDetailQueryUseCase actorDetailQueryUseCase, BackofficeLookupQueryUseCase lookupQueryUseCase, DashboardQueryService dashboardQueryService) {
        this.transactionQueryUseCase = transactionQueryUseCase;
        this.auditEventQueryUseCase = auditEventQueryUseCase;
        this.actorQueryUseCase = actorQueryUseCase;
        this.actorDetailQueryUseCase = actorDetailQueryUseCase;
        this.lookupQueryUseCase = lookupQueryUseCase;
        this.dashboardQueryService = dashboardQueryService;
    }

    @GetMapping("/transactions")
    public BackofficeResponses.ListResponse<BackofficeResponses.TransactionItem> listTransactions(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatusFilter status,
            @RequestParam(required = false) BackofficeActorTypeFilter actorType,
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
                type == null ? null : type.name(),
                status == null ? null : status.name(),
                actorType == null ? null : actorType.name(),
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
            @RequestParam(required = false) BackofficeActorTypeFilter actorType,
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
                        actorType == null ? null : actorType.name(),
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
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listAgents(new BackofficeActorQuery(query, getStatus(status), createdFrom, createdTo, limit, cursor, sort)));
    }

    @GetMapping("/clients")
    public BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> listClients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listClients(new BackofficeActorQuery(query, getStatus(status), createdFrom, createdTo, limit, cursor, sort)));
    }

    @GetMapping("/merchants")
    public BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> listMerchants(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listMerchants(new BackofficeActorQuery(query, getStatus(status), createdFrom, createdTo, limit, cursor, sort)));
    }

    @GetMapping("/terminals")
    public BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> listTerminals(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listTerminals(new BackofficeActorQuery(query, getStatus(status), createdFrom, createdTo, limit, cursor, sort)));
    }

    @GetMapping("/admins")
    public BackofficeResponses.ListResponse<BackofficeResponses.ActorItem> listAdmins(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sort
    ) {
        return toActorResponse(actorQueryUseCase.listAdmins(new BackofficeActorQuery(query, getStatus(status), createdFrom, createdTo, limit, cursor, sort)));
    }

    private static String getStatus(Status status) {
        return status == null ? null : status.name();
    }

    @GetMapping("/actors/{actorType}/{actorRef}")
    public BackofficeResponses.ActorDetails getActor(@PathVariable BackofficeActorTypeFilter actorType, @PathVariable String actorRef) {
        var d = switch (actorType) {
            case AGENT -> actorDetailQueryUseCase.getAgentByRef(actorRef);
            case CLIENT -> actorDetailQueryUseCase.getClientByRef(actorRef);
            case MERCHANT -> actorDetailQueryUseCase.getMerchantByRef(actorRef);
            case TERMINAL -> actorDetailQueryUseCase.getTerminalByRef(actorRef);
            case ADMIN -> actorDetailQueryUseCase.getAdminByRef(actorRef);
            default -> throw new IllegalArgumentException("Unsupported actorType");
        };
        return new BackofficeResponses.ActorDetails(
                d.actorRef(),
                d.displayName(),
                d.display(),
                d.status(),
                d.createdAt(),
                d.lastActivityAt()
        );
    }

    @GetMapping("/dashboard")
    public BackofficeResponses.BackofficeDashboardResponse dashboard(ActorContext actorContext) {
        var d = dashboardQueryService.buildBackofficeDashboard(actorContext);
        return new BackofficeResponses.BackofficeDashboardResponse(
                new BackofficeResponses.BackofficeStatusKpis(d.kpisToday().txCount(), d.kpisToday().txVolume(), d.kpisToday().byStatus()),
                new BackofficeResponses.BackofficeStatusKpis(d.kpis7d().txCount(), d.kpis7d().txVolume(), d.kpis7d().byStatus()),
                new BackofficeResponses.QueueCounters(d.agentPayoutRequestedCount(), d.clientRefundRequestedCount()),
                d.recentAuditEvents().stream().map(i -> new BackofficeResponses.AuditEventItem(i.eventRef(), i.occurredAt(), i.actorType(), i.actorRef(), i.action(), i.resourceType(), i.resourceRef(), i.metadata())).toList(),
                new BackofficeResponses.PlatformFunds(d.platformFunds().currency(), d.platformFunds().accounts().stream().map(a -> new BackofficeResponses.PlatformFundAccount(a.accountType(), a.balance())).toList(), d.platformFunds().netPosition())
        );
    }

    @GetMapping("/lookups")
    public BackofficeResponses.ListResponse<BackofficeResponses.LookupItem> lookups(
            @RequestParam String q,
            @RequestParam(required = false) LookupType type,
            @RequestParam(required = false) Integer limit
    ) {
        var results = lookupQueryUseCase.search(new BackofficeLookupQuery(q, type == null ? null : type.name(), limit));
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
                        new BackofficeResponses.ActorItem(i.actorRef(), i.displayName(), i.status(), i.createdAt())).toList(),
                new BackofficeResponses.CursorPage(result.nextCursor(), result.hasMore())
        );
    }
}

package com.kori.query.service;

import com.kori.application.command.GetBalanceCommand;
import com.kori.application.port.in.GetBalanceUseCase;
import com.kori.application.security.ActorContext;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.query.model.BackofficeAuditEventQuery;
import com.kori.query.model.BackofficeTransactionQuery;
import com.kori.query.model.me.AgentQueryModels;
import com.kori.query.model.me.DashboardQueryModels;
import com.kori.query.model.me.MeQueryModels;
import com.kori.query.port.in.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardQueryService {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ClientMeQueryUseCase clientMeQueryUseCase;
    private final MerchantMeQueryUseCase merchantMeQueryUseCase;
    private final AgentMeQueryUseCase agentMeQueryUseCase;
    private final BackofficeTransactionQueryUseCase backofficeTransactionQueryUseCase;
    private final BackofficeAuditEventQueryUseCase backofficeAuditEventQueryUseCase;
    private final GetBalanceUseCase getBalanceUseCase;

    public DashboardQueryService(ClientMeQueryUseCase clientMeQueryUseCase,
                                 MerchantMeQueryUseCase merchantMeQueryUseCase,
                                 AgentMeQueryUseCase agentMeQueryUseCase,
                                 BackofficeTransactionQueryUseCase backofficeTransactionQueryUseCase,
                                 BackofficeAuditEventQueryUseCase backofficeAuditEventQueryUseCase,
                                 GetBalanceUseCase getBalanceUseCase) {
        this.clientMeQueryUseCase = clientMeQueryUseCase;
        this.merchantMeQueryUseCase = merchantMeQueryUseCase;
        this.agentMeQueryUseCase = agentMeQueryUseCase;
        this.backofficeTransactionQueryUseCase = backofficeTransactionQueryUseCase;
        this.backofficeAuditEventQueryUseCase = backofficeAuditEventQueryUseCase;
        this.getBalanceUseCase = getBalanceUseCase;
    }

    public DashboardQueryModels.Kpis computeKpis7dFromTransactions(List<MeQueryModels.MeTransactionItem> items) {
        long failed = items.stream().filter(i -> "FAILED".equals(i.status())).count();
        BigDecimal volume = items.stream().map(MeQueryModels.MeTransactionItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardQueryModels.Kpis((long) items.size(), volume, failed);
    }

    public DashboardQueryModels.BackofficeDashboard buildBackofficeDashboard(ActorContext actorContext) {
        Instant now = Instant.now();
        Instant dayStart = now.truncatedTo(ChronoUnit.DAYS);
        var todayTx = backofficeTransactionQueryUseCase.list(new BackofficeTransactionQuery(null, null, null, null, null, null, null, null, null, null, dayStart, now, null, null, DEFAULT_PAGE_SIZE, null, "createdAt:desc")).items();
        var sevenDaysTx = backofficeTransactionQueryUseCase.list(new BackofficeTransactionQuery(null, null, null, null, null, null, null, null, null, null, now.minus(7, ChronoUnit.DAYS), now, null, null, DEFAULT_PAGE_SIZE, null, "createdAt:desc")).items();
        var recentAudit = backofficeAuditEventQueryUseCase.list(new BackofficeAuditEventQuery(null, null, null, null, null, null, null, DEFAULT_PAGE_SIZE, null, "occurredAt:desc")).items();

        var payouts = backofficeTransactionQueryUseCase.list(new BackofficeTransactionQuery(null, "AGENT_PAYOUT", "REQUESTED", null, null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE_SIZE, null, "createdAt:desc")).items();
        var refunds = backofficeTransactionQueryUseCase.list(new BackofficeTransactionQuery(null, "CLIENT_REFUND", "REQUESTED", null, null, null, null, null, null, null, null, null, null, null, DEFAULT_PAGE_SIZE, null, "createdAt:desc")).items();

        var feeRevenue = getBalanceUseCase.execute(new GetBalanceCommand(actorContext, LedgerAccountType.PLATFORM_FEE_REVENUE.name(), "PLATFORM"));
        var clearing = getBalanceUseCase.execute(new GetBalanceCommand(actorContext, LedgerAccountType.PLATFORM_CLEARING.name(), "PLATFORM"));
        var refundClearing = getBalanceUseCase.execute(new GetBalanceCommand(actorContext, LedgerAccountType.PLATFORM_CLIENT_REFUND_CLEARING.name(), "PLATFORM"));
        var bank = getBalanceUseCase.execute(new GetBalanceCommand(actorContext, LedgerAccountType.PLATFORM_BANK.name(), "PLATFORM"));

        BigDecimal netPosition = bank.balance().subtract(clearing.balance()).subtract(refundClearing.balance());

        return new DashboardQueryModels.BackofficeDashboard(
                statusKpis(todayTx),
                statusKpis(sevenDaysTx),
                (long) payouts.size(),
                (long) refunds.size(),
                recentAudit,
                new DashboardQueryModels.PlatformFunds("KMF", List.of(
                        new DashboardQueryModels.PlatformFund("PLATFORM_FEE_REVENUE", feeRevenue.balance()),
                        new DashboardQueryModels.PlatformFund("PLATFORM_CLEARING", clearing.balance()),
                        new DashboardQueryModels.PlatformFund("PLATFORM_CLIENT_REFUND_CLEARING", refundClearing.balance()),
                        new DashboardQueryModels.PlatformFund("PLATFORM_BANK", bank.balance())
                ), netPosition)
        );
    }

    private DashboardQueryModels.BackofficeStatusKpis statusKpis(List<com.kori.query.model.BackofficeTransactionItem> items) {
        BigDecimal volume = items.stream().map(com.kori.query.model.BackofficeTransactionItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Long> byStatus = items.stream().collect(Collectors.groupingBy(com.kori.query.model.BackofficeTransactionItem::status, Collectors.counting()));
        return new DashboardQueryModels.BackofficeStatusKpis((long) items.size(), volume, byStatus);
    }
}

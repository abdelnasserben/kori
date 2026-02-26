package com.kori.query.model.me;

import com.kori.query.model.BackofficeAuditEventItem;
import com.kori.query.model.BackofficeTransactionItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class DashboardQueryModels {
    private DashboardQueryModels() {}

    public record Kpis(Long txCount, BigDecimal txVolume, Long failedCount) {}

    public record TerminalsSummary(Long total, Map<String, Long> byStatus, Long staleTerminals) {}

    public record BackofficeStatusKpis(Long txCount, BigDecimal txVolume, Map<String, Long> byStatus) {}

    public record PlatformFund(String accountType, BigDecimal balance) {}

    public record PlatformFunds(String currency, List<PlatformFund> accounts, BigDecimal netPosition) {}

    public record BackofficeDashboard(
            BackofficeStatusKpis kpisToday,
            BackofficeStatusKpis kpis7d,
            Long agentPayoutRequestedCount,
            Long clientRefundRequestedCount,
            List<BackofficeAuditEventItem> recentAuditEvents,
            PlatformFunds platformFunds) {}
}

package com.kori.application.guard;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.port.out.PlatformConfigPort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.common.Money;

import java.util.Objects;

public final class AgentCashLimitGuard {

    private final LedgerQueryPort ledgerQueryPort;
    private final PlatformConfigPort platformConfigPort;

    public AgentCashLimitGuard(LedgerQueryPort ledgerQueryPort, PlatformConfigPort platformConfigPort) {
        this.ledgerQueryPort = Objects.requireNonNull(ledgerQueryPort);
        this.platformConfigPort = Objects.requireNonNull(platformConfigPort);
    }

    public void ensureProjectedBalanceWithinLimit(String agentId, Money debitAmount, Money creditAmount) {
        Money debit = Objects.requireNonNull(debitAmount);
        Money credit = Objects.requireNonNull(creditAmount);

        LedgerAccountRef cashClearing = LedgerAccountRef.agentCashClearing(agentId);
        Money currentBalance = ledgerQueryPort.getBalance(cashClearing);
        Money newBalance = currentBalance.plus(credit).minus(debit);

        Money cashLimit = Money.of(platformConfigPort.get()
                .orElseThrow(() -> new ForbiddenOperationException("Platform config not found"))
                .agentCashLimitGlobal());

        Money minAllowed = Money.zero().minus(cashLimit);
        if (minAllowed.isGreaterThan(newBalance)) {
            throw new ForbiddenOperationException(
                    "Agent cash limit exceeded: new balance " + newBalance + " is below " + minAllowed
            );
        }
    }
}

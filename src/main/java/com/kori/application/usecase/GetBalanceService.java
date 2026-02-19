package com.kori.application.usecase;

import com.kori.application.command.GetBalanceCommand;
import com.kori.application.port.in.GetBalanceUseCase;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.result.BalanceResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.common.Money;

import java.util.Objects;

public final class GetBalanceService implements GetBalanceUseCase {

    private final AdminAccessService adminAccessService;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerOwnerRefResolver ledgerOwnerRefResolver;

    public GetBalanceService(LedgerQueryPort ledgerQueryPort, AdminAccessService adminAccessService, LedgerOwnerRefResolver ledgerOwnerRefResolver) {
        this.ledgerQueryPort = Objects.requireNonNull(ledgerQueryPort);
        this.adminAccessService = Objects.requireNonNull(adminAccessService);
        this.ledgerOwnerRefResolver = Objects.requireNonNull(ledgerOwnerRefResolver);
    }

    @Override
    public BalanceResult execute(GetBalanceCommand cmd) {
        Objects.requireNonNull(cmd);
        adminAccessService.requireActiveAdmin(cmd.actorContext(), "consult balance");

        LedgerAccountRef requestedScope = LedgerAccountRef.of(cmd.accountType(), cmd.ownerRef());
        LedgerAccountRef scope = ledgerOwnerRefResolver.resolveToLedgerKey(requestedScope);

        var entries = ledgerQueryPort.findEntries(scope);

        Money balance = Money.zero();
        for (var e : entries) {
            if (e.type() == LedgerEntryType.CREDIT) {
                balance = balance.plus(e.amount());
            } else if (e.type() == LedgerEntryType.DEBIT) {
                balance = balance.minus(e.amount());
            }
        }

        return new BalanceResult(scope.type().name(), scope.ownerRef(), balance.asBigDecimal());
    }
}

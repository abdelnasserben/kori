package com.kori.application.usecase;

import com.kori.application.command.GetBalanceCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.GetBalanceUseCase;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.result.BalanceResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.security.LedgerAccessPolicy;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.ledger.LedgerEntryType;
import com.kori.domain.model.common.Money;

import java.util.Objects;

public final class GetBalanceService implements GetBalanceUseCase {

    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAccessPolicy ledgerAccessPolicy;

    public GetBalanceService(LedgerQueryPort ledgerQueryPort, LedgerAccessPolicy ledgerAccessPolicy) {
        this.ledgerQueryPort = Objects.requireNonNull(ledgerQueryPort);
        this.ledgerAccessPolicy = Objects.requireNonNull(ledgerAccessPolicy);
    }

    @Override
    public BalanceResult execute(GetBalanceCommand cmd) {
        Objects.requireNonNull(cmd);

        LedgerAccountRef accountRef = resolveLedgerAccountRef(cmd.actorContext(), cmd.accountType(), cmd.ownerRef());
        ledgerAccessPolicy.assertCanReadLedger(cmd.actorContext(), accountRef);

        var entries = ledgerQueryPort.findEntries(accountRef);

        Money balance = Money.zero();
        for (var e : entries) {
            if (e.type() == LedgerEntryType.CREDIT) {
                balance = balance.plus(e.amount());
            } else {
                balance = balance.minus(e.amount());
            }
        }

        return new BalanceResult(cmd.accountType(), cmd.ownerRef(), balance.asBigDecimal());
    }

    private LedgerAccountRef resolveLedgerAccountRef(ActorContext actorContext, String accountType, String ownerRef) {

        LedgerAccountType type = LedgerAccountType.valueOf(accountType);
        LedgerAccountRef ledgerAccountRef = new LedgerAccountRef(type, ownerRef);

        if (actorContext.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can specify an arbitrary ledger scope");
        }

        return ledgerAccountRef;
    }
}

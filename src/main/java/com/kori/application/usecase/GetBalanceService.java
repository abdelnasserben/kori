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
    public BalanceResult execute(GetBalanceCommand command) {
        Objects.requireNonNull(command);

        LedgerAccountRef scope = resolveScope(command.actorContext(), command.ledgerAccountRef());
        ledgerAccessPolicy.assertCanReadLedger(command.actorContext(), scope);

        var entries = ledgerQueryPort.findEntries(scope);

        Money balance = Money.zero();
        for (var e : entries) {
            if (e.type() == LedgerEntryType.CREDIT) {
                balance = balance.plus(e.amount());
            } else {
                balance = balance.minus(e.amount());
            }
        }

        return new BalanceResult(scope, balance.asBigDecimal());
    }

    private LedgerAccountRef resolveScope(ActorContext actorContext, LedgerAccountRef requestedScope) {
        if (requestedScope != null) {
            if (actorContext.actorType() != ActorType.ADMIN) {
                throw new ForbiddenOperationException("Only ADMIN can specify an arbitrary ledger scope");
            }
            return requestedScope;
        }

        return switch (actorContext.actorType()) {
            case CLIENT -> LedgerAccountRef.client(actorContext.actorId());
            case MERCHANT -> LedgerAccountRef.merchant(actorContext.actorId());
            case AGENT -> LedgerAccountRef.agent(actorContext.actorId());
            default -> throw new ForbiddenOperationException("Actor type cannot consult balance");
        };
    }
}

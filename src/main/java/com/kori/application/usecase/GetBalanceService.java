package com.kori.application.usecase;

import com.kori.application.command.GetBalanceCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.GetBalanceUseCase;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.result.BalanceResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.application.security.LedgerAccessPolicy;
import com.kori.domain.ledger.LedgerAccount;
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

        var scope = resolveScope(command.actorContext(), command.ledgerAccount(), command.referenceId());
        ledgerAccessPolicy.assertCanReadLedger(command.actorContext(), scope.ledgerAccount, scope.referenceId);

        var entries = ledgerQueryPort.findEntries(scope.ledgerAccount, scope.referenceId);
        Money balance = Money.zero();
        for (var e : entries) {
            if (e.type() == LedgerEntryType.CREDIT) {
                balance = balance.plus(e.amount());
            } else {
                balance = balance.minus(e.amount());
            }
        }

        return new BalanceResult(scope.ledgerAccount, scope.referenceId, balance.asBigDecimal());
    }

    private Scope resolveScope(ActorContext actorContext, LedgerAccount requestedAccount, String requestedRef) {
        if (requestedAccount != null || requestedRef != null) {
            if (actorContext.actorType() != ActorType.ADMIN) {
                throw new ForbiddenOperationException("Only ADMIN can specify an arbitrary ledger scope");
            }
            if (requestedAccount == null || requestedRef == null) {
                throw new IllegalArgumentException("ledgerAccount and referenceId must both be provided");
            }
            return new Scope(requestedAccount, requestedRef);
        }

        return switch (actorContext.actorType()) {
            case CLIENT -> new Scope(LedgerAccount.CLIENT, actorContext.actorId());
            case MERCHANT -> new Scope(LedgerAccount.MERCHANT, actorContext.actorId());
            case AGENT -> new Scope(LedgerAccount.AGENT, actorContext.actorId());
            default -> throw new ForbiddenOperationException("Actor type cannot consult balance");
        };
    }

    private record Scope(LedgerAccount ledgerAccount, String referenceId) {
        private Scope {
            Objects.requireNonNull(ledgerAccount);
            Objects.requireNonNull(referenceId);
        }
    }
}

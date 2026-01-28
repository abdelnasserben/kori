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

        LedgerAccountRef scope = resolveScope(cmd.actorContext(), cmd.accountType(), cmd.ownerRef());

        // centralized authorization (admin-any, others self-only, terminal forbidden, etc.)
        ledgerAccessPolicy.assertCanReadLedger(cmd.actorContext(), scope);

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

    private LedgerAccountRef resolveScope(ActorContext actor, String accountType, String ownerRef) {

        // ADMIN -> explicit scope required (any scope allowed)
        if (actor.actorType() == ActorType.ADMIN) {
            if (accountType == null || ownerRef == null) {
                throw new ForbiddenOperationException("ADMIN must specify accountType and ownerRef");
            }
            return new LedgerAccountRef(LedgerAccountType.valueOf(accountType), ownerRef);
        }

        // Non-admin -> own scope
        LedgerAccountRef own = ownScope(actor);

        // If provided, must match own scope (prevents reading others even before policy)
        if (accountType != null && ownerRef != null) {
            LedgerAccountRef requested = new LedgerAccountRef(LedgerAccountType.valueOf(accountType), ownerRef);
            if (!requested.equals(own)) {
                throw new ForbiddenOperationException("You can only consult your own balance");
            }
        }

        return own;
    }

    private static LedgerAccountRef ownScope(ActorContext actor) {
        return switch (actor.actorType()) {
            case AGENT -> LedgerAccountRef.agent(actor.actorId());
            case MERCHANT -> LedgerAccountRef.merchant(actor.actorId());
            case CLIENT -> LedgerAccountRef.client(actor.actorId());
            default -> throw new ForbiddenOperationException("Actor type cannot consult ledger");
        };
    }
}

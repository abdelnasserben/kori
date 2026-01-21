package com.kori.application.command;

import com.kori.application.security.ActorContext;
import com.kori.domain.ledger.LedgerAccount;

import java.util.Objects;

/**
 * Generic balance consultation.
 * For non-ADMIN actors, omit ledgerAccount/referenceId to consult their own balance.
 */
public record GetBalanceCommand(
        ActorContext actorContext,
        LedgerAccount ledgerAccount,
        String referenceId
) {
    public GetBalanceCommand {
        Objects.requireNonNull(actorContext);
        // ledgerAccount/referenceId may be null for self-scope consultation
    }

    public static GetBalanceCommand self(ActorContext actorContext) {
        return new GetBalanceCommand(actorContext, null, null);
    }
}

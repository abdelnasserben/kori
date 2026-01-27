package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

/**
 * Generic balance consultation.
 * For non-ADMIN actors, omit ledgerAccount/referenceId to consult their own balance.
 */
public record GetBalanceCommand(
        ActorContext actorContext,
        String accountType,
        String ownerRef
) {
    public GetBalanceCommand {
        Objects.requireNonNull(actorContext);
        Objects.requireNonNull(accountType);
        Objects.requireNonNull(ownerRef);
    }
}

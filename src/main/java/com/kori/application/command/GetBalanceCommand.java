package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

/**
 * Generic balance consultation.
 * - ADMIN must provide accountType + ownerRef (explicit scope)
 * - Non-ADMIN may omit both to consult their own balance
 */
public record GetBalanceCommand(
        ActorContext actorContext,
        String accountType, // nullable
        String ownerRef     // nullable
) {
    public GetBalanceCommand {
        Objects.requireNonNull(actorContext, "actorContext");

        // coherence: either both provided or both null
        boolean typeNull = (accountType == null);
        boolean ownerNull = (ownerRef == null);
        if (typeNull != ownerNull) {
            throw new IllegalArgumentException("accountType and ownerRef must be provided together or both omitted");
        }

        if (accountType != null) {
            accountType = accountType.trim();
        }
        if (ownerRef != null) {
            ownerRef = ownerRef.trim();
        }
    }
}

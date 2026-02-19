package com.kori.application.command;

import com.kori.application.security.ActorContext;

import java.util.Objects;

public record GetBalanceCommand(
        ActorContext actorContext,
        String accountType,
        String ownerRef
) {
    public GetBalanceCommand {
        Objects.requireNonNull(actorContext, "actorContext");
        Objects.requireNonNull(accountType, "accountType");
        Objects.requireNonNull(ownerRef, "ownerRef");
    }
}

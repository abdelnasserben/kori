package com.kori.application.command;

import com.kori.application.security.ActorContext;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.common.Status;

import java.util.Objects;

public record UpdateAccountProfileStatusCommand(
        ActorContext actorContext,
        LedgerAccountRef accountRef,
        Status targetStatus,
        String reason) {

    public UpdateAccountProfileStatusCommand(ActorContext actorContext, LedgerAccountRef accountRef, Status targetStatus, String reason) {
        this.actorContext = Objects.requireNonNull(actorContext);
        this.accountRef = Objects.requireNonNull(accountRef);
        this.targetStatus = Objects.requireNonNull(targetStatus);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}

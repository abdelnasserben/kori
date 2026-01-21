package com.kori.application.command;

import com.kori.application.security.ActorContext;
import com.kori.domain.model.account.AdminAccountStatusAction;

import java.util.Objects;

public record AdminUpdateAccountStatusCommand(String idempotencyKey, ActorContext actorContext, String accountId,
                                              AdminAccountStatusAction action, String reason) {

    public AdminUpdateAccountStatusCommand(String idempotencyKey,
                                           ActorContext actorContext,
                                           String accountId,
                                           AdminAccountStatusAction action,
                                           String reason) {
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        this.actorContext = Objects.requireNonNull(actorContext);
        this.accountId = Objects.requireNonNull(accountId);
        this.action = Objects.requireNonNull(action);
        this.reason = (reason == null || reason.isBlank()) ? "N/A" : reason;
    }
}
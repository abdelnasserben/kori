package com.kori.application.guard;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;

public final class ActorGuards {

    private ActorGuards() {
    }

    public static void requireAdmin(ActorContext actorContext, String action) {
        if (actorContext == null || actorContext.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can " + action);
        }
    }

    public static void requireAgent(ActorContext actorContext, String action) {
        if (actorContext == null || actorContext.actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Only AGENT can " + action);
        }
    }

    public static void requireTerminal(ActorContext actorContext, String action) {
        if (actorContext == null || actorContext.actorType() != ActorType.TERMINAL) {
            throw new ForbiddenOperationException("Only TERMINAL can " + action);
        }
    }
}

package com.kori.application.guard;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;

public final class ActorTypeGuards {

    private ActorTypeGuards() {}

    public static void onlyAdminCan(ActorContext ctx, String action) {
        require(ctx, ActorType.ADMIN, action);
    }

    public static void onlyAgentCan(ActorContext ctx, String action) {
        require(ctx, ActorType.AGENT, action);
    }

    public static void onlyTerminalCan(ActorContext ctx, String action) {
        require(ctx, ActorType.TERMINAL, action);
    }

    private static void require(ActorContext ctx, ActorType expected, String action) {
        if (ctx == null || ctx.actorType() != expected) {
            throw new ForbiddenOperationException("Only " + expected + " can " + action);
        }
    }
}

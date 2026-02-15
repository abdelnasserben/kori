package com.kori.application.security;

import java.util.Objects;

public final class PrincipalActorContextBridge {
    private PrincipalActorContextBridge() {}

    public static ActorContext from(ActorPrincipal principal) {
        Objects.requireNonNull(principal, "principal");
        return new ActorContext(principal.actorType(), principal.actorRef(), principal.authSubject(), principal.metadata());
    }
}

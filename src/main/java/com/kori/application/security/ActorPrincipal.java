package com.kori.application.security;

import java.util.Map;

public interface ActorPrincipal {
    ActorType actorType();

    String actorRef();

    default AuthSubject authSubject() {
        return null;
    }

    default Map<String, String> metadata() {
        return Map.of();
    }
}

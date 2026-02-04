package com.kori.application.security;

import java.util.Map;

public interface ActorPrincipal {
    ActorType actorType();

    String actorId();

    default Map<String, String> metadata() {
        return Map.of();
    }
}

package com.kori.adapters.in.rest;

import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorPrincipal;
import com.kori.application.security.ActorType;
import com.kori.application.security.PrincipalActorContextBridge;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class RestActorContextResolver {

    public static final String ACTOR_TYPE_HEADER = ApiHeaders.ACTOR_TYPE;
    public static final String ACTOR_ID_HEADER = ApiHeaders.ACTOR_ID;

    private RestActorContextResolver() {}

    public static ActorContext resolve(String actorTypeRaw, String actorIdRaw) {
        Objects.requireNonNull(actorTypeRaw, "actorType");
        Objects.requireNonNull(actorIdRaw, "actorId");
        String normalizedType = actorTypeRaw.trim().toUpperCase(Locale.ROOT);
        ActorType actorType = ActorType.valueOf(normalizedType);
        String actorId = actorIdRaw.trim();
        if (actorId.isBlank()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
        return PrincipalActorContextBridge.from(new RestActorPrincipal(actorType, actorId));
    }

    private record RestActorPrincipal(ActorType actorType, String actorId) implements ActorPrincipal {
        @Override
        public Map<String, String> metadata() {
            return Map.of();
        }
    }
}

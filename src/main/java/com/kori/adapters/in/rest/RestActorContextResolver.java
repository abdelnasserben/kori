package com.kori.adapters.in.rest;

import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class RestActorContextResolver {

    public static final String ACTOR_TYPE_HEADER = "X-Actor-Type";
    public static final String ACTOR_ID_HEADER = "X-Actor-Id";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private RestActorContextResolver() {
    }

    public static ActorContext resolve(String actorTypeRaw, String actorIdRaw) {
        Objects.requireNonNull(actorTypeRaw, "actorType");
        Objects.requireNonNull(actorIdRaw, "actorId");
        String normalizedType = actorTypeRaw.trim().toUpperCase(Locale.ROOT);
        ActorType actorType = ActorType.valueOf(normalizedType);
        String actorId = actorIdRaw.trim();
        if (actorId.isBlank()) {
            throw new IllegalArgumentException("actorId must not be blank");
        }
        return new ActorContext(actorType, actorId, Map.of());
    }
}

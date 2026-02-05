package com.kori.application.security;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ActorContextClaimsExtractor {

    private static final List<String> ACTOR_TYPE_CLAIMS = List.of("actor_type", "actorType");
    private static final List<String> ACTOR_ID_CLAIMS = List.of("actor_id", "actorId", "sub");

    public ActorContext extract(Map<String, Object> claims) {
        Objects.requireNonNull(claims, "claims");

        String actorTypeRaw = firstPresentClaim(claims, ACTOR_TYPE_CLAIMS);
        String actorIdRaw = firstPresentClaim(claims, ACTOR_ID_CLAIMS);

        if (actorTypeRaw == null || actorIdRaw == null) {
            throw new IllegalArgumentException("Missing actor claims");
        }

        ActorType actorType = ActorType.valueOf(actorTypeRaw.trim().toUpperCase(Locale.ROOT));
        String actorId = actorIdRaw.trim();
        if (actorId.isBlank()) {
            throw new IllegalArgumentException("actorId claim must not be blank");
        }

        return new ActorContext(actorType, actorId, claims.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof String)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (String) entry.getValue()
                )));
    }

    private String firstPresentClaim(Map<String, Object> claims, List<String> candidateClaims) {
        for (String claimName : candidateClaims) {
            Object claimValue = claims.get(claimName);
            if (claimValue instanceof String value && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}

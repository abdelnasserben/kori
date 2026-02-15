package com.kori.application.security;

import com.kori.application.exception.ActorContextAuthenticationException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ActorContextClaimsExtractor {

    private static final List<String> ACTOR_TYPE_CLAIMS = List.of("actor_type", "actorType");
    private static final List<String> ACTOR_REF_CLAIMS = List.of("actor_ref", "actorRef", "actor_id");

    public ActorContext extract(Map<String, Object> claims) {
        Objects.requireNonNull(claims, "claims");

        String actorTypeRaw = firstPresentClaim(claims, ACTOR_TYPE_CLAIMS);
        String actorRefRaw = firstPresentClaim(claims, ACTOR_REF_CLAIMS);

        if (actorTypeRaw == null || actorRefRaw == null) {
            throw new ActorContextAuthenticationException("Authentication required");
        }

        ActorType actorType;
        try {
            actorType = ActorType.valueOf(actorTypeRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ActorContextAuthenticationException("Authentication required");
        }
        String actorRef = actorRefRaw.trim();
        if (actorRef.isBlank()) {
            throw new ActorContextAuthenticationException("Authentication required");
        }

        AuthSubject authSubject = null;
        String authSubjectRaw = firstPresentClaim(claims, List.of("sub"));
        if (authSubjectRaw != null) {
            authSubject = AuthSubject.of(authSubjectRaw);
        }

        return new ActorContext(actorType, actorRef, authSubject, claims.entrySet().stream()
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

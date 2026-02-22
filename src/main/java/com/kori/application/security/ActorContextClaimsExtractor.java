package com.kori.application.security;

import com.kori.application.exception.ActorContextAuthenticationException;

import java.util.*;
import java.util.stream.Collectors;

public class ActorContextClaimsExtractor {

    private static final String API_CLIENT_ID = "kori-api";
    private static final List<String> ACTOR_TYPE_CLAIMS = List.of("actor_type", "actorType");
    private static final List<String> ACTOR_REF_CLAIMS = List.of("actor_ref", "actorRef");

    public ActorContext extract(Map<String, Object> claims) {
        Objects.requireNonNull(claims, "claims");

        String actorTypeRaw = firstPresentClaim(claims, ACTOR_TYPE_CLAIMS);
        String actorRefRaw = firstPresentClaim(claims, ACTOR_REF_CLAIMS);

        if (actorTypeRaw == null) {
            actorTypeRaw = inferActorTypeFromRoles(claims).orElse(null);
        }

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

    private Optional<String> inferActorTypeFromRoles(Map<String, Object> claims) {
        Map<String, Object> resourceAccess = asMap(claims.get("resource_access"));
        if (resourceAccess != null) {
            Map<String, Object> apiAccess = asMap(resourceAccess.get(API_CLIENT_ID));
            String actorTypeFromClientRole = firstMatchingActorType(extractRolesFromAccessMap(apiAccess));
            if (actorTypeFromClientRole != null) {
                return Optional.of(actorTypeFromClientRole);
            }
        }

        Map<String, Object> realmAccess = asMap(claims.get("realm_access"));
        return Optional.ofNullable(firstMatchingActorType(extractRolesFromAccessMap(realmAccess)));
    }

    private String firstMatchingActorType(List<String> roles) {
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
            try {
                ActorType.valueOf(normalizedRole);
                return normalizedRole;
            } catch (IllegalArgumentException ignored) {
                // Role is not mapped to a domain actor type.
            }
        }
        return null;
    }

    private List<String> extractRolesFromAccessMap(Map<String, Object> accessMap) {
        if (accessMap == null) {
            return List.of();
        }

        Object rolesObject = accessMap.get("roles");
        if (!(rolesObject instanceof List<?> rolesList)) {
            return List.of();
        }

        return rolesList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }
}

package com.kori.application.security;

import com.kori.application.exception.ActorContextAuthenticationException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActorContextClaimsExtractorTest {

    private final ActorContextClaimsExtractor extractor = new ActorContextClaimsExtractor();

    @Test
    void should_extract_actor_context_from_snake_case_claims() {
        Map<String, Object> claims = Map.of(
                "sub", "subject-1",
                "actor_type", "admin",
                "actor_id", "admin.user"
        );

        ActorContext context = extractor.extract(claims);

        assertEquals(ActorType.ADMIN, context.actorType());
        assertEquals("admin.user", context.actorRef());
        assertEquals("subject-1", context.authSubject().value());
    }

    @Test
    void should_extract_actor_context_from_camel_case_claims() {
        Map<String, Object> claims = Map.of(
                "sub", "subject-1",
                "actorType", "terminal",
                "actorRef", "TERM-1001"
        );

        ActorContext context = extractor.extract(claims);

        assertEquals(ActorType.TERMINAL, context.actorType());
        assertEquals("TERM-1001", context.actorRef());
    }

    @Test
    void should_fail_when_actor_ref_is_missing_even_if_subject_exists() {
        Map<String, Object> claims = Map.of(
                "sub", "A-000042",
                "actor_type", "agent"
        );

        assertThrows(ActorContextAuthenticationException.class, () -> extractor.extract(claims));
    }

    @Test
    void should_fail_when_actor_type_is_missing() {
        Map<String, Object> claims = Map.of("sub", "admin-1");

        assertThrows(ActorContextAuthenticationException.class, () -> extractor.extract(claims));
    }

}

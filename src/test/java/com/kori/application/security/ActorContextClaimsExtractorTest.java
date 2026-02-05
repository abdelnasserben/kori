package com.kori.application.security;

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
                "actor_id", "admin-1"
        );

        ActorContext context = extractor.extract(claims);

        assertEquals(ActorType.ADMIN, context.actorType());
        assertEquals("admin-1", context.actorId());
    }

    @Test
    void should_extract_actor_context_from_camel_case_claims() {
        Map<String, Object> claims = Map.of(
                "sub", "subject-1",
                "actorType", "terminal",
                "actorId", "term-1"
        );

        ActorContext context = extractor.extract(claims);

        assertEquals(ActorType.TERMINAL, context.actorType());
        assertEquals("term-1", context.actorId());
    }

    @Test
    void should_fallback_to_subject_for_actor_id() {
        Map<String, Object> claims = Map.of(
                "sub", "agent-42",
                "actor_type", "agent"
        );

        ActorContext context = extractor.extract(claims);

        assertEquals(ActorType.AGENT, context.actorType());
        assertEquals("agent-42", context.actorId());
    }

    @Test
    void should_fail_when_actor_type_is_missing() {
        Map<String, Object> claims = Map.of("sub", "admin-1");

        assertThrows(IllegalArgumentException.class, () -> extractor.extract(claims));
    }

}

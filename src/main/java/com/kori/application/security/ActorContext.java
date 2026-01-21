package com.kori.application.security;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record ActorContext(ActorType actorType, String actorId, Map<String, String> metadata) {
    public ActorContext(ActorType actorType, String actorId, Map<String, String> metadata) {
        this.actorType = Objects.requireNonNull(actorType);
        this.actorId = Objects.requireNonNull(actorId);
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }
}

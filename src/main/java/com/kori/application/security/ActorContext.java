package com.kori.application.security;

import com.kori.domain.model.admin.AdminUsername;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.client.ClientCode;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.terminal.TerminalUid;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record ActorContext(
        ActorType actorType,
        String actorRef,
        AuthSubject authSubject,
        Map<String, String> metadata
) {

    public ActorContext(ActorType actorType, String actorRef, Map<String, String> metadata) {
        this(actorType, actorRef, null, metadata);
    }

    public ActorContext(ActorType actorType, String actorRef, AuthSubject authSubject, Map<String, String> metadata) {
        this.actorType = Objects.requireNonNull(actorType, "actor type is required");
        this.actorRef = Objects.requireNonNull(actorRef, "actor ref is required").trim();
        if (this.actorRef.isBlank()) {
            throw new IllegalArgumentException("actor ref is required");
        }
        validateActorRef(actorType, this.actorRef);
        this.authSubject = authSubject;
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    private static void validateActorRef(ActorType actorType, String actorRef) {
        switch (actorType) {
            case CLIENT -> ClientCode.of(actorRef);
            case AGENT -> AgentCode.of(actorRef);
            case MERCHANT -> MerchantCode.of(actorRef);
            case TERMINAL -> TerminalUid.of(actorRef);
            case ADMIN -> AdminUsername.of(actorRef);
            default -> {}
        }
    }
}

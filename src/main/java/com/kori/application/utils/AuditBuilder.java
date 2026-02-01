package com.kori.application.utils;

import com.kori.application.security.ActorContext;
import com.kori.domain.model.audit.AuditEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class AuditBuilder {

    private AuditBuilder() {
    }

    /**
     * Build a standard audit event for ADMIN status updates.
     *
     * @param actionPrefix e.g. "ADMIN_UPDATE_CLIENT_STATUS"
     * @param actorContext actor performing the action (ADMIN)
     * @param occurredAt timestamp (usually TimeProviderPort.now())
     * @param subjectKey key used in metadata, e.g. "clientId", "merchantCode", "agentCode"
     * @param subjectValue id/code value
     * @param before before status name
     * @param after after status name
     * @param reason normalized reason (already N/A if blank)
     * @param extra extra metadata (optional)
     */
    public static AuditEvent buildStatusChangeAudit(
            String actionPrefix,
            ActorContext actorContext,
            Instant occurredAt,
            String subjectKey,
            String subjectValue,
            String before,
            String after,
            String reason,
            Map<String, String> extra
    ) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(subjectKey, subjectValue);
        metadata.put("before", before);
        metadata.put("after", after);
        metadata.put("reason", reason);

        if (extra != null && !extra.isEmpty()) {
            metadata.putAll(extra);
        }

        return new AuditEvent(
                actionPrefix + "_" + after,
                actorContext.actorType().name(),
                actorContext.actorId(),
                occurredAt,
                metadata
        );
    }

    public static AuditEvent buildStatusChangeAudit(
            String actionPrefix,
            ActorContext actorContext,
            Instant occurredAt,
            String subjectKey,
            String subjectValue,
            String before,
            String after,
            String reason
    ) {
        return buildStatusChangeAudit(
                actionPrefix,
                actorContext,
                occurredAt,
                subjectKey,
                subjectValue,
                before,
                after,
                reason,
                Map.of()
        );
    }

    public static AuditEvent buildBasicAudit(
            String action,
            ActorContext actor,
            Instant occurredAt,
            Map<String, String> metadata) {

        return new AuditEvent(
                action,
                actor.actorType().name(),
                actor.actorId(),
                occurredAt,
                metadata);
    }
}

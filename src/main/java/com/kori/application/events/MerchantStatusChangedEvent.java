package com.kori.application.events;

import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.MerchantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Événement publié quand le statut d'un Merchant change.
 */
public record MerchantStatusChangedEvent(
        String eventId,
        Instant occurredAt,
        MerchantId merchantId,
        Status before,
        Status after,
        String reason
) implements DomainEvent {

    public MerchantStatusChangedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(merchantId, "merchantCode");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
    }
}

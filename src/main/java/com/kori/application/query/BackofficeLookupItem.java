package com.kori.application.query;

public record BackofficeLookupItem(
        String entityType,
        String entityId,
        String display,
        String status
) {
}

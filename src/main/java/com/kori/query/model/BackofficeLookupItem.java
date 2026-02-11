package com.kori.query.model;

public record BackofficeLookupItem(
        String entityType,
        String entityId,
        String display,
        String status
) {
}

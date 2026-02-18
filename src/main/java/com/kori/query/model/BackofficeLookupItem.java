package com.kori.query.model;

public record BackofficeLookupItem(
        String entityType,
        String entityRef,
        String display,
        String status
) {
}

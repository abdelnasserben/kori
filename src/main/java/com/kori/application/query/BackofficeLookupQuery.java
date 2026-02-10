package com.kori.application.query;

public record BackofficeLookupQuery(
        String q,
        String type,
        Integer limit
) {
}


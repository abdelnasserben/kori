package com.kori.query.model;

public record BackofficeLookupQuery(
        String q,
        String type,
        Integer limit
) {
}


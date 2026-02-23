package com.kori.query.service;

import com.kori.application.exception.ValidationException;
import com.kori.query.model.BackofficeLookupItem;
import com.kori.query.model.BackofficeLookupQuery;
import com.kori.query.port.in.BackofficeLookupQueryUseCase;
import com.kori.query.port.out.BackofficeLookupReadPort;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BackofficeLookupQueryService implements BackofficeLookupQueryUseCase {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MIN_Q_LENGTH = 2;
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "CLIENT_CODE",
            "CARD_UID",
            "TERMINAL_UID",
            "TRANSACTION_REF",
            "MERCHANT_CODE",
            "AGENT_CODE",
            "ADMIN_USERNAME"
    );

    private final BackofficeLookupReadPort readPort;

    public BackofficeLookupQueryService(BackofficeLookupReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public List<BackofficeLookupItem> search(BackofficeLookupQuery query) {
        String q = query.q() == null ? "" : query.q().trim();
        if (q.length() < MIN_Q_LENGTH) {
            throw new ValidationException("q must be at least 2 characters", Map.of("field", "q"));
        }

        int limit = query.limit() == null ? DEFAULT_LIMIT : query.limit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ValidationException("limit must be between 1 and " + MAX_LIMIT, Map.of("field", "limit", "maxLimit", MAX_LIMIT));
        }

        String type = query.type();
        if (type != null) {
            type = type.trim().toUpperCase();
        }
        if (type != null && !type.isBlank() && !SUPPORTED_TYPES.contains(type)) {
            throw new ValidationException("Unsupported lookup type", Map.of("field", "type"));
        }

        return readPort.search(new BackofficeLookupQuery(q, type, limit));
    }
}

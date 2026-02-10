package com.kori.application.query.service;

import com.kori.application.exception.ValidationException;
import com.kori.application.port.in.query.BackofficeLookupQueryUseCase;
import com.kori.application.port.out.query.BackofficeLookupReadPort;
import com.kori.application.query.BackofficeLookupItem;
import com.kori.application.query.BackofficeLookupQuery;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BackofficeLookupQueryService implements BackofficeLookupQueryUseCase {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MIN_Q_LENGTH = 2;
    private static final Set<String> SUPPORTED_TYPES = Set.of("CLIENT_PHONE", "CARD_UID", "TERMINAL_ID", "TRANSACTION_ID", "MERCHANT_CODE", "AGENT_CODE");

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

        Integer limit = query.limit() == null ? DEFAULT_LIMIT : query.limit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ValidationException("limit must be between 1 and " + MAX_LIMIT, Map.of("field", "limit", "maxLimit", MAX_LIMIT));
        }

        String type = query.type();
        if (type != null && !type.isBlank() && !SUPPORTED_TYPES.contains(type)) {
            throw new ValidationException("Unsupported lookup type", Map.of("field", "type"));
        }

        return readPort.search(new BackofficeLookupQuery(q, type, limit));
    }
}

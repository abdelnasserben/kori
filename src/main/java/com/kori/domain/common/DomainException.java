package com.kori.domain.common;

import java.util.Map;
import java.util.Objects;

public class DomainException extends RuntimeException {
    private final DomainErrorCode code;
    private final DomainErrorCategory category;
    private final Map<String, Object> metadata;

    public DomainException(DomainErrorCode code, DomainErrorCategory category, String message) {
        this(code, category, message, Map.of());
    }

    public DomainException(DomainErrorCode code,
                           DomainErrorCategory category,
                           String message,
                           Map<String, Object> metadata) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.category = Objects.requireNonNull(category, "category");
        this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }

    public DomainErrorCode code() {
        return code;
    }

    public DomainErrorCategory category() {
        return category;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }
}

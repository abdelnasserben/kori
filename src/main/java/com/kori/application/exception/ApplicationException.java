package com.kori.application.exception;

import java.util.Map;
import java.util.Objects;

public class ApplicationException extends RuntimeException {

    private final ApplicationErrorCode code;
    private final ApplicationErrorCategory category;
    private final Map<String, Object> metadata;

    public ApplicationException(ApplicationErrorCode code, ApplicationErrorCategory category, String message) {
            this(code, category, message, Map.of());
        }

    public ApplicationException(ApplicationErrorCode code,
                ApplicationErrorCategory category,
                String message,
                Map<String, Object> metadata) {
            super(message);
            this.code = Objects.requireNonNull(code, "code");
            this.category = Objects.requireNonNull(category, "category");
            this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
        }

        public ApplicationErrorCode code() {
            return code;
        }

        public ApplicationErrorCategory category() {
            return category;
        }

        public Map<String, Object> metadata() {
            return metadata;
        }
    }
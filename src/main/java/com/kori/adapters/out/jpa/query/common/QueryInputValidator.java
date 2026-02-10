package com.kori.adapters.out.jpa.query.common;

import com.kori.application.exception.ValidationException;

import java.util.Map;
import java.util.regex.Pattern;

public final class QueryInputValidator {
    private static final Pattern SAFE_ENUM_FILTER = Pattern.compile("^[A-Z_]{2,64}$");

    private QueryInputValidator() {
    }

    public static int normalizeLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1 || limit > maxLimit) {
            throw new ValidationException("limit must be between 1 and " + maxLimit, Map.of("field", "limit", "rejectedValue", limit));
        }
        return limit;
    }

    public static boolean resolveSort(String sortRaw, String field) {
        if (sortRaw == null || sortRaw.isBlank()) {
            return true;
        }

        String asc = field + ":asc";
        String desc = field + ":desc";
        if (!asc.equals(sortRaw) && !desc.equals(sortRaw)) {
            throw new ValidationException("Invalid sort format. Use <field>:<asc|desc>", Map.of("field", "sort", "rejectedValue", sortRaw));
        }
        return sortRaw.endsWith("desc");
    }

    public static void validateEnumFilter(String field, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!SAFE_ENUM_FILTER.matcher(value).matches()) {
            throw new ValidationException("Invalid filter format", Map.of("field", field));
        }
    }
}

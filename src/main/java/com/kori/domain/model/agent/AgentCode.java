package com.kori.domain.model.agent;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Code agent "terrain-friendly" : A-XXXXXX (6 digits).
 * - Format/validation: domaine
 * - Unicité: application (via repo)
 */
public final class AgentCode {

    private static final String PREFIX = "A-";
    private static final Pattern FORMAT = Pattern.compile("^A-[0-9]{6}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String value;

    private AgentCode(String value) {
        this.value = value;
    }

    public static AgentCode of(String raw) {
        Objects.requireNonNull(raw, "agentCode");
        String normalized = raw.trim().toUpperCase();
        if (!FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid agentCode format. Expected A-XXXXXX (6 digits).");
        }
        return new AgentCode(normalized);
    }

    public static AgentCode generate() {
        int number = RANDOM.nextInt(1_000_000); // 000000..999999
        return new AgentCode(String.format("%s%06d", PREFIX, number));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentCode other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}

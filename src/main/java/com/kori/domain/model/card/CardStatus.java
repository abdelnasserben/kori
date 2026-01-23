package com.kori.domain.model.card;

public enum CardStatus {
    ACTIVE,
    BLOCKED,
    LOST,
    SUSPENDED,
    INACTIVE;

    /**
     * Hard business transitions for Card status.
     * This is the single source of truth (services must not bypass it).
     */
    public boolean canTransitionTo(CardStatus target) {
        if (target == null) return false;
        if (this == target) return true;

        return switch (this) {
            case ACTIVE -> target == BLOCKED || target == LOST || target == SUSPENDED || target == INACTIVE;
            case BLOCKED -> target == LOST || target == SUSPENDED || target == INACTIVE;
            case LOST -> target == INACTIVE; // LOST is terminal (except admin deactivation)
            case SUSPENDED -> target == INACTIVE; // no direct reactivation here
            case INACTIVE -> false; // terminal
        };
    }

    public boolean isPayable() {
        return this == ACTIVE;
    }

    public boolean isTerminal() {
        return this == LOST || this == INACTIVE;
    }
}

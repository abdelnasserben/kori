package com.kori.domain.model.terminal;

import com.kori.domain.common.InvalidStatusTransitionException;
import com.kori.domain.model.common.DisplayName;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.MerchantId;

import java.time.Instant;
import java.util.Objects;

public final class Terminal {
    private final TerminalId id;
    private final TerminalUid terminalUid;
    private final MerchantId merchantId;
    private final DisplayName displayName;
    private Status status;
    private final Instant createdAt;

    public Terminal(TerminalId id, TerminalUid terminalUid, MerchantId merchantId, DisplayName displayName, Status status, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.terminalUid = Objects.requireNonNull(terminalUid);
        this.merchantId = Objects.requireNonNull(merchantId);
        this.displayName = displayName;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public Terminal(TerminalId id, TerminalUid terminalUid, MerchantId merchantId, Status status, Instant createdAt) {
        this(id, terminalUid, merchantId, null, status, createdAt);
    }

    public TerminalId id() {
        return id;
    }

    public TerminalUid terminalUid(){
        return terminalUid;
    }

    public MerchantId merchantId() {
        return merchantId;
    }

    public DisplayName displayName() {
        return displayName;
    }

    public String display() {
        return displayName != null ? displayName.value() : terminalUid.value();
    }

    public Status status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void suspend() {
        ensureNotClosed("suspend");
        if (status == Status.SUSPENDED) return;
        status = Status.SUSPENDED;
    }

    public void activate() {
        ensureNotClosed("activate");
        if (status == Status.ACTIVE) return;
        status = Status.ACTIVE;
    }

    public void close() {
        if (status == Status.CLOSED) return;
        status = Status.CLOSED;
    }

    private void ensureNotClosed(String action) {
        if (status == Status.CLOSED) {
            throw new InvalidStatusTransitionException("Cannot " + action + " a CLOSED terminal");
        }
    }
}

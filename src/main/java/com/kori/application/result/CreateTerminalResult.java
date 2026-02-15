package com.kori.application.result;

import java.util.Objects;

public record CreateTerminalResult(String terminalUid, String merchantCode) {
    public CreateTerminalResult(String terminalUid, String merchantCode) {
        this.terminalUid = Objects.requireNonNull(terminalUid);
        this.merchantCode = Objects.requireNonNull(merchantCode);
    }
}

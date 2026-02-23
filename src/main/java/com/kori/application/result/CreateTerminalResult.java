package com.kori.application.result;

import java.util.Objects;

public record CreateTerminalResult(String terminalUid, String merchantCode, String displayName) {
    public CreateTerminalResult {
        Objects.requireNonNull(terminalUid, "terminalUid");
        Objects.requireNonNull(merchantCode, "merchantCode");
    }
}

package com.kori.application.result;

import java.util.Objects;

public record CreateTerminalResult(String terminalId, String merchantCode) {
    public CreateTerminalResult(String terminalId, String merchantCode) {
        this.terminalId = Objects.requireNonNull(terminalId);
        this.merchantCode = Objects.requireNonNull(merchantCode);
    }
}

package com.kori.application.port.out;

import com.kori.domain.model.terminal.Terminal;

import java.util.Optional;

public interface TerminalRepositoryPort {
    Optional<Terminal> findById(String terminalId);

    void save(Terminal terminal);
}

package com.kori.application.port.out;

import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;

import java.util.Optional;

public interface TerminalRepositoryPort {
    Optional<Terminal> findById(TerminalId terminalId);

    void save(Terminal terminal);
}

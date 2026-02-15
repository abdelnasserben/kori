package com.kori.application.port.out;

import com.kori.domain.model.merchant.MerchantId;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import com.kori.domain.model.terminal.TerminalUid;

import java.util.List;
import java.util.Optional;

public interface TerminalRepositoryPort {
    Optional<Terminal> findById(TerminalId terminalId);

    void save(Terminal terminal);

    List<Terminal> findByMerchantId(MerchantId merchantId);

    Optional<Terminal> findByUid(TerminalUid terminalUid);

    boolean existsByUid(TerminalUid terminalUid);
}

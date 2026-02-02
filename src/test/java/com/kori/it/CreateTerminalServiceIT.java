package com.kori.it;

import com.kori.application.command.CreateTerminalCommand;
import com.kori.application.port.in.CreateTerminalUseCase;
import com.kori.application.result.CreateTerminalResult;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class CreateTerminalServiceIT extends IntegrationTestBase {

    private static final String MERCHANT_CODE = "M-303030";

    @Autowired
    CreateTerminalUseCase createTerminalUseCase;

    @Test
    void createTerminal_happyPath_persistsTerminalAndAudit() {
        Merchant merchant = createActiveMerchant(MERCHANT_CODE);

        CreateTerminalResult result = createTerminalUseCase.execute(new CreateTerminalCommand(
                "idem-create-terminal-1",
                adminActor(),
                merchant.code().value()
        ));

        assertNotNull(result.terminalId());

        Terminal terminal = terminalRepositoryPort.findById(new TerminalId(java.util.UUID.fromString(result.terminalId())))
                .orElseThrow();
        assertEquals(Status.ACTIVE, terminal.status());
        assertEquals(merchant.id(), terminal.merchantId());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("TERMINAL_CREATED"))
        );
    }
}

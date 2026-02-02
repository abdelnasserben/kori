package com.kori.it;

import com.kori.application.command.UpdateTerminalStatusCommand;
import com.kori.application.port.in.UpdateTerminalStatusUseCase;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateTerminalStatusServiceIT extends IntegrationTestBase {

    @Autowired
    UpdateTerminalStatusUseCase updateTerminalStatusUseCase;

    @Test
    void updateTerminalStatus_suspendsTerminalAndWritesAudit() {
        Merchant merchant = createActiveMerchant("M-404040");
        Terminal terminal = createActiveTerminal(merchant);

        updateTerminalStatusUseCase.execute(new UpdateTerminalStatusCommand(
                adminActor(),
                terminal.id().value().toString(),
                Status.SUSPENDED.name(),
                "test"
        ));

        Terminal updated = terminalRepositoryPort.findById(new TerminalId(terminal.id().value())).orElseThrow();
        assertEquals(Status.SUSPENDED, updated.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_UPDATE_TERMINAL_STATUS"))
        );
    }
}

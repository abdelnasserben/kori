package com.kori.it;

import com.kori.application.command.UpdateMerchantStatusCommand;
import com.kori.application.port.in.UpdateMerchantStatusUseCase;
import com.kori.application.result.UpdateMerchantStatusResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpdateMerchantStatusServiceIT extends IntegrationTestBase {

    private static final String MERCHANT_CODE = "M-101010";

    @Autowired
    UpdateMerchantStatusUseCase updateMerchantStatusUseCase;

    @Test
    void updateMerchantStatus_suspendsAccountProfileAndTerminals() {
        Merchant merchant = createActiveMerchant(MERCHANT_CODE);
        Terminal terminal = createActiveTerminal(merchant);

        LedgerAccountRef merchantAccount = LedgerAccountRef.merchant(merchant.id().value().toString());
        AccountProfile profile = accountProfilePort.findByAccount(merchantAccount).orElseThrow();
        assertEquals(Status.ACTIVE, profile.status());

        UpdateMerchantStatusResult result = updateMerchantStatusUseCase.execute(new UpdateMerchantStatusCommand(
                adminActor(),
                MERCHANT_CODE,
                Status.SUSPENDED.name(),
                "test"
        ));

        assertNotNull(result.merchantCode());

        Merchant updatedMerchant = merchantRepositoryPort.findByCode(merchant.code()).orElseThrow();
        assertEquals(Status.SUSPENDED, updatedMerchant.status());

        AccountProfile updatedProfile = accountProfilePort.findByAccount(merchantAccount).orElseThrow();
        assertEquals(Status.SUSPENDED, updatedProfile.status());

        List<Terminal> terminals = terminalRepositoryPort.findByMerchantId(merchant.id());
        assertEquals(1, terminals.size());
        assertEquals(Status.SUSPENDED, terminals.get(0).status());

        Terminal storedTerminal = terminalRepositoryPort.findById(new TerminalId(terminal.id().value())).orElseThrow();
        assertEquals(Status.SUSPENDED, storedTerminal.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().equals("ADMIN_UPDATE_MERCHANT_STATUS_SUSPENDED"))
        );
    }
}

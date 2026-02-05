package com.kori.it;

import com.kori.application.command.UpdateAccountProfileStatusCommand;
import com.kori.application.port.in.UpdateAccountProfileStatusUseCase;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerAccountType;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateAccountProfileStatusServiceIT extends IntegrationTestBase {

    @Autowired
    UpdateAccountProfileStatusUseCase updateAccountProfileStatusUseCase;

    @Test
    void updateAccountProfileStatus_suspendsProfileAndWritesAudit() {
        String ownerRef = UUID.randomUUID().toString();
        LedgerAccountRef accountRef = new LedgerAccountRef(LedgerAccountType.AGENT_WALLET, ownerRef);
        createActiveAccountProfile(accountRef);

        updateAccountProfileStatusUseCase.execute(new UpdateAccountProfileStatusCommand(
                adminActor(),
                LedgerAccountType.AGENT_WALLET.name(),
                ownerRef,
                Status.SUSPENDED.name(),
                "test"
        ));

        AccountProfile updated = accountProfilePort.findByAccount(accountRef).orElseThrow();
        assertEquals(Status.SUSPENDED, updated.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("ADMIN_UPDATE_ACCOUNT_PROFILE_STATUS"))
        );
    }
}

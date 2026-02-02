package com.kori.it;

import com.kori.application.command.CreateMerchantCommand;
import com.kori.application.port.in.CreateMerchantUseCase;
import com.kori.application.result.CreateMerchantResult;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class CreateMerchantServiceIT extends IntegrationTestBase {

    @Autowired
    CreateMerchantUseCase createMerchantUseCase;

    @Test
    void createMerchant_happyPath_persistsMerchantProfileAndAudit() {
        CreateMerchantResult result = createMerchantUseCase.execute(new CreateMerchantCommand(
                "idem-create-merchant-1",
                adminActor()
        ));

        assertNotNull(result.merchantId());
        assertNotNull(result.code());

        Merchant merchant = merchantRepositoryPort.findByCode(MerchantCode.of(result.code())).orElseThrow();
        assertEquals(Status.ACTIVE, merchant.status());

        LedgerAccountRef merchantAccount = LedgerAccountRef.merchant(result.merchantId());
        AccountProfile profile = accountProfilePort.findByAccount(merchantAccount).orElseThrow();
        assertEquals(Status.ACTIVE, profile.status());

        assertTrue(auditEventJpaRepository.findAll().stream()
                .anyMatch(event -> event.getAction().contains("MERCHANT_CREATED"))
        );
    }
}

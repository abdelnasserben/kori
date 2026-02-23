package com.kori.application.usecase;

import com.kori.application.command.CreateMerchantCommand;
import com.kori.application.exception.ApplicationErrorCategory;
import com.kori.application.exception.ApplicationErrorCode;
import com.kori.application.exception.ApplicationException;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.CreateMerchantUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateMerchantResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.common.DisplayName;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class CreateMerchantService implements CreateMerchantUseCase {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;

    private final AdminAccessService adminAccessService;
    private final MerchantRepositoryPort merchantRepository;
    private final AccountProfilePort accountProfilePort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;
    private final CodeGeneratorPort codeGeneratorPort;
    private final IdGeneratorPort idGeneratorPort;
    private final IdempotencyExecutor idempotencyExecutor;

    public CreateMerchantService(AdminAccessService adminAccessService, MerchantRepositoryPort merchantRepository, AccountProfilePort accountProfilePort, AuditPort auditPort, TimeProviderPort timeProviderPort, IdempotencyPort idempotencyPort, CodeGeneratorPort codeGeneratorPort, IdGeneratorPort idGeneratorPort) {
        this.adminAccessService = adminAccessService;
        this.merchantRepository = merchantRepository;
        this.accountProfilePort = accountProfilePort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
        this.codeGeneratorPort = codeGeneratorPort;
        this.idGeneratorPort = idGeneratorPort;
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public CreateMerchantResult execute(CreateMerchantCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                CreateMerchantResult.class,
                () -> {

                    var actorContext = command.actorContext();
                    adminAccessService.requireActiveAdmin(actorContext, "create a merchant.");

                    MerchantCode code = generateUniqueMerchantCode();
                    MerchantId id = new MerchantId(idGeneratorPort.newUuid());
                    Instant now = timeProviderPort.now();
                    DisplayName displayName = DisplayName.ofNullable(command.displayName());

                    Merchant merchant = new Merchant(id, code, displayName, Status.ACTIVE, now);
                    merchantRepository.save(merchant);

                    LedgerAccountRef merchantAccount = LedgerAccountRef.merchant(id.value().toString());

                    accountProfilePort.findByAccount(merchantAccount).ifPresent(existing -> {
                        throw new ForbiddenOperationException("Merchant account already exists for " + merchantAccount);
                    });

                    AccountProfile profile = AccountProfile.activeNew(merchantAccount, now);
                    accountProfilePort.save(profile);

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("merchantCode", merchant.code().value());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "MERCHANT_CREATED",
                            actorContext,
                            now,
                            metadata
                    ));

                    return new CreateMerchantResult(id.value().toString(), code.value(), merchant.display());
                }
        );
    }

    private MerchantCode generateUniqueMerchantCode() {
        for (int i = 0; i < MAX_CODE_GENERATION_ATTEMPTS; i++) {
            String digits = codeGeneratorPort.next6Digits();
            MerchantCode candidate = MerchantCode.of("M-" + digits);
            if (!merchantRepository.existsByCode(candidate)) {
                return candidate;
            }
        }

        throw new ApplicationException(
                ApplicationErrorCode.TECHNICAL_FAILURE,
                ApplicationErrorCategory.TECHNICAL,
                "Unable to generate unique merchantCode."
        );
    }
}

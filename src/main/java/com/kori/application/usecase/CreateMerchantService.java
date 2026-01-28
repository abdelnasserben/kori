package com.kori.application.usecase;

import com.kori.application.command.CreateMerchantCommand;
import com.kori.application.exception.ApplicationException;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.CreateMerchantUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateMerchantResult;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class CreateMerchantService implements CreateMerchantUseCase {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;

    private final MerchantRepositoryPort merchantRepository;
    private final AccountProfilePort accountProfilePort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final CodeGeneratorPort codeGeneratorPort;
    private final IdGeneratorPort idGeneratorPort;

    public CreateMerchantService(MerchantRepositoryPort merchantRepository, AccountProfilePort accountProfilePort, AuditPort auditPort, TimeProviderPort timeProviderPort, IdempotencyPort idempotencyPort, CodeGeneratorPort codeGeneratorPort, IdGeneratorPort idGeneratorPort) {
        this.merchantRepository = merchantRepository;
        this.accountProfilePort = accountProfilePort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.codeGeneratorPort = codeGeneratorPort;
        this.idGeneratorPort = idGeneratorPort;
    }

    @Override
    public CreateMerchantResult execute(CreateMerchantCommand command) {
        var actorContext = command.actorContext();

        if (actorContext.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can create a merchant.");
        }

        // Idempotency first (same key => same result, no side effects)
        var cached = idempotencyPort.find(command.idempotencyKey(), CreateMerchantResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        MerchantCode code = generateUniqueMerchantCode();
        MerchantId id = new MerchantId(idGeneratorPort.newUuid());
        Instant now = timeProviderPort.now();

        Merchant merchant = new Merchant(id, code, Status.ACTIVE, now);
        merchantRepository.save(merchant);

        // Create ledger accountRef ref + profile
        LedgerAccountRef merchantAccount = LedgerAccountRef.merchant(id.value().toString());

        accountProfilePort.findByAccount(merchantAccount).ifPresent(existing -> {
            throw new ForbiddenOperationException("Merchant account already exists for " + merchantAccount);
        });

        AccountProfile profile = AccountProfile.activeNew(merchantAccount, now);
        accountProfilePort.save(profile);

        CreateMerchantResult result = new CreateMerchantResult(id.value().toString(), code.value());
        idempotencyPort.save(command.idempotencyKey(), result);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("adminId", actorContext.actorId());
        metadata.put("merchantCode", merchant.code().value());

        auditPort.publish(new AuditEvent(
                "MERCHANT_CREATED",
                actorContext.actorType().name(),
                actorContext.actorId(),
                now,
                metadata
        ));

        return result;
    }

    private MerchantCode generateUniqueMerchantCode() {
        for (int i = 0; i < MAX_CODE_GENERATION_ATTEMPTS; i++) {
            String digits = codeGeneratorPort.next6Digits();
            MerchantCode candidate = MerchantCode.of("M-" + digits);
            if (!merchantRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new ApplicationException("Unable to generate unique merchantCode.");
    }
}

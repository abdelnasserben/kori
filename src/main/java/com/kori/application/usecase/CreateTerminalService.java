package com.kori.application.usecase;

import com.kori.application.command.CreateTerminalCommand;
import com.kori.application.exception.ApplicationErrorCategory;
import com.kori.application.exception.ApplicationErrorCode;
import com.kori.application.exception.ApplicationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorStatusGuards;
import com.kori.application.idempotency.IdempotencyExecutor;
import com.kori.application.port.in.CreateTerminalUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateTerminalResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;
import com.kori.domain.model.terminal.TerminalUid;

import java.math.BigInteger;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CreateTerminalService implements CreateTerminalUseCase {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 20;

    private final AdminAccessService adminAccessService;
    private final TerminalRepositoryPort terminalRepositoryPort;
    private final MerchantRepositoryPort merchantRepositoryPort;
    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final AuditPort auditPort;
    private final IdempotencyExecutor idempotencyExecutor;

    public CreateTerminalService(AdminAccessService adminAccessService, TerminalRepositoryPort terminalRepositoryPort, MerchantRepositoryPort merchantRepositoryPort, IdempotencyPort idempotencyPort, TimeProviderPort timeProviderPort, IdGeneratorPort idGeneratorPort, AuditPort auditPort) {
        this.adminAccessService = adminAccessService;
        this.terminalRepositoryPort = terminalRepositoryPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.timeProviderPort = timeProviderPort;
        this.idGeneratorPort = idGeneratorPort;
        this.auditPort = auditPort;
        this.idempotencyExecutor = new IdempotencyExecutor(idempotencyPort);
    }

    @Override
    public CreateTerminalResult execute(CreateTerminalCommand command) {
        return idempotencyExecutor.execute(
                command.idempotencyKey(),
                command.idempotencyRequestHash(),
                CreateTerminalResult.class,
                () -> {

                    var actorContext = command.actorContext();
                    adminAccessService.requireActiveAdmin(actorContext, "create a terminal.");

                    // Merchant
                    Merchant merchant = merchantRepositoryPort.findByCode(MerchantCode.of(command.merchantCode()))
                            .orElseThrow(() -> new NotFoundException("Merchant not found"));
                    ActorStatusGuards.requireActiveMerchant(merchant);

                    Instant now = timeProviderPort.now();

                    TerminalId terminalId = new TerminalId(idGeneratorPort.newUuid());
                    TerminalUid terminalUid = generateUniqueTerminalUid();
                    Terminal terminal = new Terminal(terminalId, terminalUid, merchant.id(), Status.ACTIVE, now);
                    terminalRepositoryPort.save(terminal);

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("terminalUid", terminalId.value().toString());
                    metadata.put("merchantCode", merchant.code().value());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "TERMINAL_CREATED",
                            actorContext,
                            now,
                            metadata
                    ));

                    return new CreateTerminalResult(terminalUid.value(), command.merchantCode());
                }
        );
    }

    private TerminalUid generateUniqueTerminalUid() {
        for (int i = 0; i < MAX_CODE_GENERATION_ATTEMPTS; i++) {
            Random random = new Random();
            String suffix = new BigInteger(50, random).toString(36).toUpperCase();
            TerminalUid.of("T-" + suffix);
            TerminalUid candidate = TerminalUid.of("T-" + suffix);;
            if (!terminalRepositoryPort.existsByUid(candidate)) {
                return candidate;
            }
        }

        throw new ApplicationException(
                ApplicationErrorCode.TECHNICAL_FAILURE,
                ApplicationErrorCategory.TECHNICAL,
                "Unable to generate unique terminalUid."
        );
    }
}

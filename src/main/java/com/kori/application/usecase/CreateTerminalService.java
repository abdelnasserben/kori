package com.kori.application.usecase;

import com.kori.application.command.CreateTerminalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class CreateTerminalService implements CreateTerminalUseCase {

    private final TerminalRepositoryPort terminalRepositoryPort;
    private final MerchantRepositoryPort merchantRepositoryPort;
    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final AuditPort auditPort;
    private final IdempotencyExecutor idempotencyExecutor;

    public CreateTerminalService(TerminalRepositoryPort terminalRepositoryPort, MerchantRepositoryPort merchantRepositoryPort, IdempotencyPort idempotencyPort, TimeProviderPort timeProviderPort, IdGeneratorPort idGeneratorPort, AuditPort auditPort) {
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
                    // business logic

                    var actorContext = command.actorContext();

                    ActorGuards.requireAdmin(command.actorContext(), "create a terminal.");

                    // Merchant
                    Merchant merchant = merchantRepositoryPort.findByCode(MerchantCode.of(command.merchantCode()))
                            .orElseThrow(() -> new NotFoundException("Merchant not found"));

                    if(merchant.status() != Status.ACTIVE) {
                        throw new ForbiddenOperationException("Merchant is not active");
                    }

                    Instant now = timeProviderPort.now();

                    TerminalId terminalId = new TerminalId(idGeneratorPort.newUuid());
                    Terminal terminal = new Terminal(terminalId, merchant.id(), Status.ACTIVE, now);
                    terminalRepositoryPort.save(terminal);

                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("adminId", actorContext.actorId());
                    metadata.put("terminalId", terminalId.value().toString());
                    metadata.put("merchantCode", merchant.code().value());

                    auditPort.publish(AuditBuilder.buildBasicAudit(
                            "TERMINAL_CREATED",
                            actorContext,
                            now,
                            metadata
                    ));

                    return new CreateTerminalResult(terminalId.value().toString(), command.merchantCode());
                }
        );
    }
}

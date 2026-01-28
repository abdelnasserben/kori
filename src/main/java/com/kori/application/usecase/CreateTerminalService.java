package com.kori.application.usecase;

import com.kori.application.command.CreateTerminalCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.CreateTerminalUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.CreateTerminalResult;
import com.kori.application.security.ActorType;
import com.kori.domain.model.audit.AuditEvent;
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
    private final IdempotencyPort idempotencyPort;
    private final TimeProviderPort timeProviderPort;
    private final IdGeneratorPort idGeneratorPort;
    private final AuditPort auditPort;

    public CreateTerminalService(TerminalRepositoryPort terminalRepositoryPort, MerchantRepositoryPort merchantRepositoryPort, IdempotencyPort idempotencyPort, TimeProviderPort timeProviderPort, IdGeneratorPort idGeneratorPort, AuditPort auditPort) {
        this.terminalRepositoryPort = terminalRepositoryPort;
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.idempotencyPort = idempotencyPort;
        this.timeProviderPort = timeProviderPort;
        this.idGeneratorPort = idGeneratorPort;
        this.auditPort = auditPort;
    }

    @Override
    public CreateTerminalResult execute(CreateTerminalCommand command) {
        var actorContext = command.actorContext();

        if (actorContext.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can create a terminal.");
        }

        // Idempotency first (same key => same result, no side effects)
        var cached = idempotencyPort.find(command.idempotencyKey(), CreateTerminalResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

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

        CreateTerminalResult result = new CreateTerminalResult(terminalId.value().toString(), command.merchantCode());
        idempotencyPort.save(command.idempotencyKey(), result);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("adminId", actorContext.actorId());
        metadata.put("terminalId", terminalId.value().toString());
        metadata.put("merchantCode", merchant.code().value());

        auditPort.publish(new AuditEvent(
                "TERMINAL_CREATED",
                actorContext.actorType().name(),
                actorContext.actorId(),
                now,
                metadata
        ));

        return result;
    }
}

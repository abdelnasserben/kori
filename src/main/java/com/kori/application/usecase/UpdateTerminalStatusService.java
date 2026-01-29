package com.kori.application.usecase;

import com.kori.application.command.UpdateTerminalStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateTerminalStatusUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.TerminalRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateTerminalStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UpdateTerminalStatusService implements UpdateTerminalStatusUseCase {

    private final TerminalRepositoryPort terminalRepositoryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdateTerminalStatusService(
            TerminalRepositoryPort terminalRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort) {
        this.terminalRepositoryPort = terminalRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdateTerminalStatusResult execute(UpdateTerminalStatusCommand cmd) {
        requireAdmin(cmd.actorContext());

        Terminal terminal = terminalRepositoryPort.findById(new TerminalId(UUID.fromString(cmd.terminalId())))
                .orElseThrow(() -> new NotFoundException("Terminal not found"));

        // For audit
        String before = terminal.status().name();

        // Apply updating
        switch (Status.valueOf(cmd.targetStatus())) {
            case ACTIVE -> terminal.activate();
            case SUSPENDED -> terminal.suspend();
            case CLOSED -> terminal.close();
        }
        terminalRepositoryPort.save(terminal);

        // Audit
        String auditAction = "ADMIN_UPDATE_TERMINAL_STATUS_" + cmd.targetStatus();
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("terminalId", cmd.terminalId());
        metadata.put("before", before);
        metadata.put("after", cmd.targetStatus());
        metadata.put("reason", cmd.reason());

        auditPort.publish(new AuditEvent(
                auditAction,
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorId(),
                now,
                metadata
        ));
        return new UpdateTerminalStatusResult(cmd.terminalId(), before, cmd.targetStatus());
    }

    private void requireAdmin(ActorContext actor) {
        if (actor == null || actor.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update client status");
        }
    }
}

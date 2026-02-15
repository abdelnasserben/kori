package com.kori.application.usecase;

import com.kori.application.command.UpdateTerminalStatusCommand;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateTerminalStatusUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.TerminalRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateTerminalStatusResult;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.terminal.Terminal;
import com.kori.domain.model.terminal.TerminalUid;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UpdateTerminalStatusService implements UpdateTerminalStatusUseCase {

    private final AdminAccessService adminAccessService;
    private final TerminalRepositoryPort terminalRepositoryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdateTerminalStatusService(
            AdminAccessService adminAccessService,
            TerminalRepositoryPort terminalRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort) {
        this.adminAccessService = adminAccessService;
        this.terminalRepositoryPort = terminalRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdateTerminalStatusResult execute(UpdateTerminalStatusCommand cmd) {
        adminAccessService.requireActiveAdmin(cmd.actorContext(), "update terminal status");

        Terminal terminal = terminalRepositoryPort.findByUid(TerminalUid.of(cmd.terminalUid()))
                .orElseThrow(() -> new NotFoundException("Terminal not found"));

        // For audit
        String before = terminal.status().name();
        Status afterStatus = Status.parseStatus(cmd.targetStatus());

        // Apply updating
        switch (afterStatus) {
            case ACTIVE -> terminal.activate();
            case SUSPENDED -> terminal.suspend();
            case CLOSED -> terminal.close();
        }
        terminalRepositoryPort.save(terminal);

        // Audit
        Instant now = timeProviderPort.now();
        String reason = ReasonNormalizer.normalize(cmd.reason());

        Map<String, String> metadata = new HashMap<>();
        metadata.put("terminalUid", cmd.terminalUid());
        metadata.put("before", before);
        metadata.put("after", afterStatus.name());
        metadata.put("reason", reason);

        auditPort.publish(new AuditEvent(
                "ADMIN_UPDATE_TERMINAL_STATUS",
                cmd.actorContext().actorType().name(),
                cmd.actorContext().actorRef(),
                now,
                metadata
        ));
        return new UpdateTerminalStatusResult(cmd.terminalUid(), before, cmd.targetStatus());
    }
}

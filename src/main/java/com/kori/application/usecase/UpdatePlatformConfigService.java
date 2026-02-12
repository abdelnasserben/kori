package com.kori.application.usecase;

import com.kori.application.command.UpdatePlatformConfigCommand;
import com.kori.application.exception.ValidationException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.UpdatePlatformConfigUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.PlatformConfigPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdatePlatformConfigResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.model.config.PlatformConfig;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UpdatePlatformConfigService implements UpdatePlatformConfigUseCase {

    private final PlatformConfigPort platformConfigPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdatePlatformConfigService(
            PlatformConfigPort platformConfigPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        this.platformConfigPort = platformConfigPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdatePlatformConfigResult execute(UpdatePlatformConfigCommand cmd) {
        ActorGuards.requireAdmin(cmd.actorContext(), "update platform config");
        validate(cmd.agentCashLimitGlobal());

        Optional<PlatformConfig> previous = platformConfigPort.get();
        PlatformConfig updated = new PlatformConfig(cmd.agentCashLimitGlobal());
        platformConfigPort.upsert(updated);

        String reason = ReasonNormalizer.normalize(cmd.reason());
        Map<String, String> metadata = new HashMap<>();
        metadata.put("reason", reason);
        metadata.put("agentCashLimitGlobal", cmd.agentCashLimitGlobal().toPlainString());
        previous.ifPresent(cfg -> metadata.put("previousAgentCashLimitGlobal", cfg.agentCashLimitGlobal().toPlainString()));

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "ADMIN_UPDATE_PLATFORM_CONFIG",
                cmd.actorContext(),
                timeProviderPort.now(),
                metadata
        ));

        return new UpdatePlatformConfigResult(cmd.agentCashLimitGlobal());
    }

    private void validate(BigDecimal agentCashLimitGlobal) {
        if (agentCashLimitGlobal.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Invalid platform configuration", Map.of("agentCashLimitGlobal", "must be >= 0"));
        }
    }
}

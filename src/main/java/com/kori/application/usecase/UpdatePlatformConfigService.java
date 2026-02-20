package com.kori.application.usecase;

import com.kori.application.command.UpdatePlatformConfigCommand;
import com.kori.application.exception.ValidationException;
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

    private final AdminAccessService adminAccessService;
    private final PlatformConfigPort platformConfigPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdatePlatformConfigService(
            AdminAccessService adminAccessService,
            PlatformConfigPort platformConfigPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        this.adminAccessService = adminAccessService;
        this.platformConfigPort = platformConfigPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdatePlatformConfigResult execute(UpdatePlatformConfigCommand cmd) {
        adminAccessService.requireActiveAdmin(cmd.actorContext(), "update platform config");

        validate(cmd);

        Optional<PlatformConfig> previous = platformConfigPort.get();
        PlatformConfig updated = new PlatformConfig(
                cmd.agentCashLimitGlobal(),
                cmd.clientTransferMaxPerTransaction(),
                cmd.clientTransferDailyMax()
        );
        platformConfigPort.upsert(updated);

        String reason = ReasonNormalizer.normalize(cmd.reason());
        Map<String, String> metadata = new HashMap<>();
        metadata.put("reason", reason);
        metadata.put("agentCashLimitGlobal", cmd.agentCashLimitGlobal().toPlainString());
        metadata.put("clientTransferMaxPerTransaction", cmd.clientTransferMaxPerTransaction().toPlainString());
        metadata.put("clientTransferDailyMax", cmd.clientTransferDailyMax().toPlainString());
        previous.ifPresent(cfg -> {
            metadata.put("previousAgentCashLimitGlobal", cfg.agentCashLimitGlobal().toPlainString());
            metadata.put("previousClientTransferMaxPerTransaction", cfg.clientTransferMaxPerTransaction().toPlainString());
            metadata.put("previousClientTransferDailyMax", cfg.clientTransferDailyMax().toPlainString());
        });

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "ADMIN_UPDATE_PLATFORM_CONFIG",
                cmd.actorContext(),
                timeProviderPort.now(),
                metadata
        ));

        return new UpdatePlatformConfigResult(
                cmd.agentCashLimitGlobal(),
                cmd.clientTransferMaxPerTransaction(),
                cmd.clientTransferDailyMax()
        );
    }

    private void validate(UpdatePlatformConfigCommand cmd) {
        Map<String, Object> errors = new HashMap<>();
        validateNonNegative(cmd.agentCashLimitGlobal(), "agentCashLimitGlobal", errors);
        validateNonNegative(cmd.clientTransferMaxPerTransaction(), "clientTransferMaxPerTransaction", errors);
        validateNonNegative(cmd.clientTransferDailyMax(), "clientTransferDailyMax", errors);
        if (cmd.clientTransferDailyMax().compareTo(cmd.clientTransferMaxPerTransaction()) < 0) {
            errors.put("clientTransferDailyMax", "must be >= clientTransferMaxPerTransaction");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid platform configuration", errors);
        }
    }

    private void validateNonNegative(BigDecimal value, String field, Map<String, Object> errors) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            errors.put(field, "must be >= 0");
        }
    }
}

package com.kori.application.usecase;

import com.kori.application.command.UpdateCommissionConfigCommand;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.in.UpdateCommissionConfigUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.CommissionConfigPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.CommissionConfigResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.model.config.CommissionConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UpdateCommissionConfigService implements UpdateCommissionConfigUseCase {

    private final AdminAccessService adminAccessService;
    private final CommissionConfigPort commissionConfigPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdateCommissionConfigService(
            AdminAccessService adminAccessService,
            CommissionConfigPort commissionConfigPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        this.adminAccessService = adminAccessService;
        this.commissionConfigPort = commissionConfigPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public CommissionConfigResult execute(UpdateCommissionConfigCommand cmd) {
        adminAccessService.requireActiveAdmin(cmd.actorContext(), "update commission config");

        validate(cmd);

        Optional<CommissionConfig> previous = commissionConfigPort.get();

        CommissionConfig updated = new CommissionConfig(
                cmd.cardEnrollmentAgentCommission(),
                cmd.merchantWithdrawCommissionRate(),
                cmd.merchantWithdrawCommissionMin(),
                cmd.merchantWithdrawCommissionMax()
        );

        commissionConfigPort.upsert(updated);

        String reason = ReasonNormalizer.normalize(cmd.reason());
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("reason", reason);
        metadata.put("cardEnrollmentAgentCommission", cmd.cardEnrollmentAgentCommission().toPlainString());
        metadata.put("merchantWithdrawCommissionRate", cmd.merchantWithdrawCommissionRate().toPlainString());
        metadata.put("merchantWithdrawCommissionMin", toPlainString(cmd.merchantWithdrawCommissionMin()));
        metadata.put("merchantWithdrawCommissionMax", toPlainString(cmd.merchantWithdrawCommissionMax()));

        previous.ifPresent(cfg -> {
            metadata.put("previousCardEnrollmentAgentCommission", cfg.cardEnrollmentAgentCommission().toPlainString());
            metadata.put("previousMerchantWithdrawCommissionRate", cfg.merchantWithdrawCommissionRate().toPlainString());
            metadata.put("previousMerchantWithdrawCommissionMin", toPlainString(cfg.merchantWithdrawCommissionMin()));
            metadata.put("previousMerchantWithdrawCommissionMax", toPlainString(cfg.merchantWithdrawCommissionMax()));
        });

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "ADMIN_UPDATE_COMMISSION_CONFIG",
                cmd.actorContext(),
                now,
                metadata
        ));

        return new CommissionConfigResult(
                cmd.cardEnrollmentAgentCommission(),
                cmd.merchantWithdrawCommissionRate(),
                cmd.merchantWithdrawCommissionMin(),
                cmd.merchantWithdrawCommissionMax()
        );
    }

    private void validate(UpdateCommissionConfigCommand cmd) {
        Map<String, Object> errors = new HashMap<>();

        validateNonNegative(cmd.cardEnrollmentAgentCommission(), errors);
        validateRate(cmd.merchantWithdrawCommissionRate(), errors);
        validateOptionalNonNegative(cmd.merchantWithdrawCommissionMin(), "merchantWithdrawCommissionMin", errors);
        validateOptionalNonNegative(cmd.merchantWithdrawCommissionMax(), "merchantWithdrawCommissionMax", errors);
        validateOptionalMinMax(
                cmd.merchantWithdrawCommissionMin(),
                cmd.merchantWithdrawCommissionMax(),
                "merchantWithdrawCommission",
                errors
        );

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid commission configuration", errors);
        }
    }

    private void validateNonNegative(BigDecimal value, Map<String, Object> errors) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            errors.put("cardEnrollmentAgentCommission", "must be >= 0");
        }
    }

    private void validateOptionalNonNegative(BigDecimal value, String field, Map<String, Object> errors) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            errors.put(field, "must be >= 0");
        }
    }

    private void validateRate(BigDecimal rate, Map<String, Object> errors) {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            errors.put("merchantWithdrawCommissionRate", "must be between 0 and 1");
        }
    }

    private void validateOptionalMinMax(BigDecimal min, BigDecimal max, String field, Map<String, Object> errors) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            errors.put(field, "min must be <= max");
        }
    }

    private String toPlainString(BigDecimal value) {
        return value == null ? "null" : value.toPlainString();
    }
}

package com.kori.application.usecase;

import com.kori.application.command.UpdateFeeConfigCommand;
import com.kori.application.exception.ValidationException;
import com.kori.application.port.in.UpdateFeeConfigUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.FeeConfigPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateFeeConfigResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.model.config.FeeConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UpdateFeeConfigService implements UpdateFeeConfigUseCase {

    private final AdminAccessService adminAccessService;
    private final FeeConfigPort feeConfigPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdateFeeConfigService(
            AdminAccessService adminAccessService,
            FeeConfigPort feeConfigPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        this.adminAccessService = adminAccessService;
        this.feeConfigPort = feeConfigPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdateFeeConfigResult execute(UpdateFeeConfigCommand cmd) {
        adminAccessService.requireActiveAdmin(cmd.actorContext(), "update fee config");

        validate(cmd);

        Optional<FeeConfig> previous = feeConfigPort.get();

        FeeConfig updated = new FeeConfig(
                cmd.cardEnrollmentPrice(),
                cmd.cardPaymentFeeRate(),
                cmd.cardPaymentFeeMin(),
                cmd.cardPaymentFeeMax(),
                cmd.merchantWithdrawFeeRate(),
                cmd.merchantWithdrawFeeMin(),
                cmd.merchantWithdrawFeeMax(),
                cmd.clientTransferFeeRate(),
                cmd.clientTransferFeeMin(),
                cmd.clientTransferFeeMax(),
                cmd.cardPaymentFeeRefundable(),
                cmd.merchantWithdrawFeeRefundable(),
                cmd.cardEnrollmentPriceRefundable()
        );

        feeConfigPort.upsert(updated);

        String reason = ReasonNormalizer.normalize(cmd.reason());
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("reason", reason);
        metadata.put("cardEnrollmentPrice", cmd.cardEnrollmentPrice().toPlainString());
        metadata.put("cardPaymentFeeRate", cmd.cardPaymentFeeRate().toPlainString());
        metadata.put("cardPaymentFeeMin", cmd.cardPaymentFeeMin().toPlainString());
        metadata.put("cardPaymentFeeMax", cmd.cardPaymentFeeMax().toPlainString());
        metadata.put("merchantWithdrawFeeRate", cmd.merchantWithdrawFeeRate().toPlainString());
        metadata.put("merchantWithdrawFeeMin", cmd.merchantWithdrawFeeMin().toPlainString());
        metadata.put("merchantWithdrawFeeMax", cmd.merchantWithdrawFeeMax().toPlainString());
        metadata.put("clientTransferFeeRate", cmd.clientTransferFeeRate().toPlainString());
        metadata.put("clientTransferFeeMin", cmd.clientTransferFeeMin().toPlainString());
        metadata.put("clientTransferFeeMax", cmd.clientTransferFeeMax().toPlainString());
        metadata.put("cardPaymentFeeRefundable", String.valueOf(cmd.cardPaymentFeeRefundable()));
        metadata.put("merchantWithdrawFeeRefundable", String.valueOf(cmd.merchantWithdrawFeeRefundable()));
        metadata.put("cardEnrollmentPriceRefundable", String.valueOf(cmd.cardEnrollmentPriceRefundable()));

        previous.ifPresent(cfg -> {
            metadata.put("previousCardEnrollmentPrice", cfg.cardEnrollmentPrice().toPlainString());
            metadata.put("previousCardPaymentFeeRate", cfg.cardPaymentFeeRate().toPlainString());
            metadata.put("previousCardPaymentFeeMin", cfg.cardPaymentFeeMin().toPlainString());
            metadata.put("previousCardPaymentFeeMax", cfg.cardPaymentFeeMax().toPlainString());
            metadata.put("previousMerchantWithdrawFeeRate", cfg.merchantWithdrawFeeRate().toPlainString());
            metadata.put("previousMerchantWithdrawFeeMin", cfg.merchantWithdrawFeeMin().toPlainString());
            metadata.put("previousMerchantWithdrawFeeMax", cfg.merchantWithdrawFeeMax().toPlainString());
            metadata.put("previousClientTransferFeeRate", cfg.clientTransferFeeRate().toPlainString());
            metadata.put("previousClientTransferFeeMin", cfg.clientTransferFeeMin().toPlainString());
            metadata.put("previousClientTransferFeeMax", cfg.clientTransferFeeMax().toPlainString());
        });

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "ADMIN_UPDATE_FEE_CONFIG",
                cmd.actorContext(),
                now,
                metadata
        ));

        return new UpdateFeeConfigResult(
                cmd.cardEnrollmentPrice(),
                cmd.cardPaymentFeeRate(),
                cmd.cardPaymentFeeMin(),
                cmd.cardPaymentFeeMax(),
                cmd.merchantWithdrawFeeRate(),
                cmd.merchantWithdrawFeeMin(),
                cmd.merchantWithdrawFeeMax(),
                cmd.clientTransferFeeRate(),
                cmd.clientTransferFeeMin(),
                cmd.clientTransferFeeMax(),
                cmd.cardPaymentFeeRefundable(),
                cmd.merchantWithdrawFeeRefundable(),
                cmd.cardEnrollmentPriceRefundable()
        );
    }

    private void validate(UpdateFeeConfigCommand cmd) {
        Map<String, Object> errors = new HashMap<>();

        validateNonNegative(cmd.cardEnrollmentPrice(), "cardEnrollmentPrice", errors);
        validateRate(cmd.cardPaymentFeeRate(), "cardPaymentFeeRate", errors);
        validateNonNegative(cmd.cardPaymentFeeMin(), "cardPaymentFeeMin", errors);
        validateNonNegative(cmd.cardPaymentFeeMax(), "cardPaymentFeeMax", errors);
        validateMinMax(cmd.cardPaymentFeeMin(), cmd.cardPaymentFeeMax(), "cardPaymentFee", errors);

        validateRate(cmd.merchantWithdrawFeeRate(), "merchantWithdrawFeeRate", errors);
        validateNonNegative(cmd.merchantWithdrawFeeMin(), "merchantWithdrawFeeMin", errors);
        validateNonNegative(cmd.merchantWithdrawFeeMax(), "merchantWithdrawFeeMax", errors);
        validateMinMax(cmd.merchantWithdrawFeeMin(), cmd.merchantWithdrawFeeMax(), "merchantWithdrawFee", errors);

        validateRate(cmd.clientTransferFeeRate(), "clientTransferFeeRate", errors);
        validateNonNegative(cmd.clientTransferFeeMin(), "clientTransferFeeMin", errors);
        validateNonNegative(cmd.clientTransferFeeMax(), "clientTransferFeeMax", errors);
        validateMinMax(cmd.clientTransferFeeMin(), cmd.clientTransferFeeMax(), "clientTransferFee", errors);

        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid fee configuration", errors);
        }
    }

    private void validateNonNegative(BigDecimal value, String field, Map<String, Object> errors) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            errors.put(field, "must be >= 0");
        }
    }

    private void validateRate(BigDecimal rate, String field, Map<String, Object> errors) {
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            errors.put(field, "must be between 0 and 1");
        }
    }

    private void validateMinMax(BigDecimal min, BigDecimal max, String field, Map<String, Object> errors) {
        if (min.compareTo(max) > 0) {
            errors.put(field, "min must be <= max");
        }
    }
}

package com.kori.application.usecase;

import com.kori.application.command.UpdateMerchantStatusCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.UpdateMerchantStatusUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.MerchantRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateMerchantStatusResult;
import com.kori.application.security.ActorContext;
import com.kori.application.security.ActorType;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UpdateMerchantStatusService implements UpdateMerchantStatusUseCase {

    private final MerchantRepositoryPort merchantRepositoryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public UpdateMerchantStatusService(
            MerchantRepositoryPort merchantRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort) {
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
    }

    @Override
    public UpdateMerchantStatusResult execute(UpdateMerchantStatusCommand cmd) {
        requireAdmin(cmd.actorContext());

        Merchant merchant = merchantRepositoryPort.findByCode(MerchantCode.of(cmd.merchantCode()))
                .orElseThrow(() -> new NotFoundException("Merchant not found"));

        // For audit
        String before = merchant.status().name();

        // Apply updating
        switch (Status.valueOf(cmd.targetStatus())) {
            case ACTIVE -> merchant.activate();
            case SUSPENDED -> merchant.suspend();
            case CLOSED -> merchant.close();
        }
        merchantRepositoryPort.save(merchant);

        // Audit
        String auditAction = "ADMIN_UPDATE_MERCHANT_STATUS_" + cmd.targetStatus();
        Instant now = timeProviderPort.now();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("merchantCode", cmd.merchantCode());
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
        return new UpdateMerchantStatusResult(cmd.merchantCode(), before, cmd.targetStatus());
    }

    private void requireAdmin(ActorContext actor) {
        if (actor == null || actor.actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can update merchant status");
        }
    }
}

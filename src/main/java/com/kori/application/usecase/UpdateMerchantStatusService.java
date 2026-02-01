package com.kori.application.usecase;

import com.kori.application.command.UpdateMerchantStatusCommand;
import com.kori.application.events.MerchantStatusChangedEvent;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.UpdateMerchantStatusUseCase;
import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.DomainEventPublisherPort;
import com.kori.application.port.out.MerchantRepositoryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.application.result.UpdateMerchantStatusResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;

import java.time.Instant;
import java.util.UUID;

public class UpdateMerchantStatusService implements UpdateMerchantStatusUseCase {

    private final MerchantRepositoryPort merchantRepositoryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;
    private final DomainEventPublisherPort domainEventPublisherPort;

    public UpdateMerchantStatusService(
            MerchantRepositoryPort merchantRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort
    ) {
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
        this.domainEventPublisherPort = domainEventPublisherPort;
    }

    @Override
    public UpdateMerchantStatusResult execute(UpdateMerchantStatusCommand cmd) {
        ActorGuards.requireAdmin(cmd.actorContext(), "update merchant status");

        Merchant merchant = merchantRepositoryPort.findByCode(MerchantCode.of(cmd.merchantCode()))
                .orElseThrow(() -> new NotFoundException("Merchant not found"));

        Status beforeStatus = merchant.status();
        String before = beforeStatus.name();

        Status afterStatus = Status.parseStatus(cmd.targetStatus());

        switch (afterStatus) {
            case ACTIVE -> merchant.activate();
            case SUSPENDED -> merchant.suspend();
            case CLOSED -> merchant.close();
        }

        merchantRepositoryPort.save(merchant);

        String reason = ReasonNormalizer.normalize(cmd.reason());
        Instant now = timeProviderPort.now();

        // Audit
        auditPort.publish(AuditBuilder.buildStatusChangeAudit(
                "ADMIN_UPDATE_MERCHANT_STATUS",
                cmd.actorContext(),
                now,
                "merchantCode",
                cmd.merchantCode(),
                before,
                cmd.targetStatus(),
                reason
        ));

        if (beforeStatus != afterStatus) {
            domainEventPublisherPort.publish(new MerchantStatusChangedEvent(
                    UUID.randomUUID().toString(),
                    now,
                    merchant.id(),
                    beforeStatus,
                    afterStatus,
                    reason
            ));
        }

        return new UpdateMerchantStatusResult(cmd.merchantCode(), before, cmd.targetStatus());
    }
}

package com.kori.application.usecase;

import com.kori.application.command.UpdateMerchantStatusCommand;
import com.kori.application.events.MerchantStatusChangedEvent;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.port.in.UpdateMerchantStatusUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.UpdateMerchantStatusResult;
import com.kori.application.utils.AuditBuilder;
import com.kori.application.utils.ReasonNormalizer;
import com.kori.domain.ledger.LedgerAccountRef;
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
    private final LedgerQueryPort ledgerQueryPort;

    public UpdateMerchantStatusService(
            MerchantRepositoryPort merchantRepositoryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort,
            DomainEventPublisherPort domainEventPublisherPort, LedgerQueryPort ledgerQueryPort
    ) {
        this.merchantRepositoryPort = merchantRepositoryPort;
        this.auditPort = auditPort;
        this.timeProviderPort = timeProviderPort;
        this.domainEventPublisherPort = domainEventPublisherPort;
        this.ledgerQueryPort = ledgerQueryPort;
    }

    @Override
    public UpdateMerchantStatusResult execute(UpdateMerchantStatusCommand cmd) {
        ActorGuards.requireAdmin(cmd.actorContext(), "update merchant status");

        Merchant merchant = merchantRepositoryPort.findByCode(MerchantCode.of(cmd.merchantCode()))
                .orElseThrow(() -> new NotFoundException("Merchant not found"));

        Status beforeStatus = merchant.status();
        String before = beforeStatus.name();

        Status afterStatus = Status.parseStatus(cmd.targetStatus());

        if (afterStatus == Status.CLOSED && beforeStatus != Status.CLOSED) {
            ensureMerchantWalletIsZero(merchant);
        }

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

    private void ensureMerchantWalletIsZero(Merchant merchant) {
        LedgerAccountRef merchantWallet = LedgerAccountRef.merchant(merchant.id().value().toString());
        if (!ledgerQueryPort.netBalance(merchantWallet).isZero()) {
            throw new IllegalStateException("MERCHANT_WALLET_BALANCE_MUST_BE_ZERO_TO_CLOSE");
        }
    }
}

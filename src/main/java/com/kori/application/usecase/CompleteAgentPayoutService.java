package com.kori.application.usecase;

import com.kori.application.command.CompleteAgentPayoutCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.InsufficientFundsException;
import com.kori.application.port.in.CompleteAgentPayoutUseCase;
import com.kori.application.port.out.*;
import com.kori.application.security.ActorType;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.payout.Payout;
import com.kori.domain.model.payout.PayoutId;
import com.kori.domain.model.payout.PayoutStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CompleteAgentPayoutService implements CompleteAgentPayoutUseCase {

    private final TimeProviderPort timeProviderPort;
    private final PayoutRepositoryPort payoutRepositoryPort;
    private final LedgerQueryPort ledgerQueryPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    public CompleteAgentPayoutService(TimeProviderPort timeProviderPort,
                                      PayoutRepositoryPort payoutRepositoryPort,
                                      LedgerQueryPort ledgerQueryPort,
                                      LedgerAppendPort ledgerAppendPort,
                                      AuditPort auditPort) {
        this.timeProviderPort = timeProviderPort;
        this.payoutRepositoryPort = payoutRepositoryPort;
        this.ledgerQueryPort = ledgerQueryPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
    }

    @Override
    public void execute(CompleteAgentPayoutCommand command) {
        if (command.actorContext().actorType() != ActorType.ADMIN) {
            throw new ForbiddenOperationException("Only ADMIN can complete payouts");
        }

        Payout payout = payoutRepositoryPort.findById(new PayoutId(UUID.fromString(command.payoutId())))
                .orElseThrow(() -> new ForbiddenOperationException("Payout not found"));

        if (payout.status() != PayoutStatus.REQUESTED) {
            throw new ForbiddenOperationException("Payout is not in REQUESTED status");
        }

        // Safety check: agent must still have the funds when we actually settle.
        Money available = ledgerQueryPort.agentAvailableBalance(payout.agentId());
        if (payout.amount().isGreaterThan(available)) {
            throw new InsufficientFundsException(
                    "Insufficient agent funds to complete payout: need " + payout.amount() + " but available " + available
            );
        }

        Instant now = timeProviderPort.now();

        // Actual settlement in ledger happens ONLY here.
        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(payout.transactionId(), LedgerAccount.AGENT, payout.amount(), payout.agentId()),
                LedgerEntry.credit(payout.transactionId(), LedgerAccount.PLATFORM_CLEARING, payout.amount(), null)
        ));

        Payout completed = payoutRepositoryPort.save(payout.complete(now));

        Map<String, String> metadata = new HashMap<>();
        metadata.put("payoutId", completed.id().value().toString());
        metadata.put("agentId", completed.agentId());
        metadata.put("transactionId", completed.transactionId().value());
        metadata.put("amount", completed.amount().asBigDecimal().toPlainString());

        auditPort.publish(new AuditEvent(
                "AGENT_PAYOUT_COMPLETED",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        ));
    }
}

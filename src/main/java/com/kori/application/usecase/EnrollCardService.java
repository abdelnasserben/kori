package com.kori.application.usecase;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.security.ActorType;
import com.kori.application.security.PinFormatValidator;
import com.kori.domain.ledger.LedgerAccount;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.account.Account;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnrollCardService implements EnrollCardUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;

    private final ClientRepositoryPort clientRepositoryPort;
    private final AccountRepositoryPort accountRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AgentRepositoryPort agentRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    private final FeePolicyPort feePolicyPort;
    private final CommissionPolicyPort commissionPolicyPort;

    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;

    private final PinHasherPort pinHasherPort;

    public EnrollCardService(TimeProviderPort timeProviderPort,
                             IdempotencyPort idempotencyPort,
                             ClientRepositoryPort clientRepositoryPort,
                             AccountRepositoryPort accountRepositoryPort,
                             CardRepositoryPort cardRepositoryPort,
                             AgentRepositoryPort agentRepositoryPort,
                             TransactionRepositoryPort transactionRepositoryPort,
                             FeePolicyPort feePolicyPort,
                             CommissionPolicyPort commissionPolicyPort,
                             LedgerAppendPort ledgerAppendPort,
                             AuditPort auditPort, PinHasherPort pinHasherPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.accountRepositoryPort = accountRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.feePolicyPort = feePolicyPort;
        this.commissionPolicyPort = commissionPolicyPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
        this.pinHasherPort = pinHasherPort;
    }

    @Override
    public EnrollCardResult execute(EnrollCardCommand command) {
        // Idempotence (Phase 1, use-case scoped)
        var cached = idempotencyPort.find(command.idempotencyKey(), com.kori.application.result.EnrollCardResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // Authorization conceptuelle : enrôlement effectué par un AGENT
        if (command.actorContext().actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Only AGENT can enroll a card");
        }

        // Agent must exist
        if (!agentRepositoryPort.existsById(command.agentId())) {
            throw new ForbiddenOperationException("Agent not found");
        }

        // Card UID must be unique
        if (cardRepositoryPort.findByCardUid(command.cardUid()).isPresent()) {
            throw new ForbiddenOperationException("Card UID already enrolled");
        }

        Instant now = timeProviderPort.now();

        // 1) client (si absent)
        boolean clientCreated = false;
        Client client = clientRepositoryPort.findByPhoneNumber(command.phoneNumber()).orElse(null);

        if (client == null) {
            client = Client.activeNew(command.phoneNumber());
            client = clientRepositoryPort.save(client);
            clientCreated = true;
        }

        // 2) compte (si absent)
        boolean accountCreated = false;
        Account account = accountRepositoryPort.findByClientId(client.id()).orElse(null);

        if (account == null) {
            account = Account.activeNew(client.id());
            account = accountRepositoryPort.save(account);
            accountCreated = true;
        }

        // 3) ajout carte (active)
        PinFormatValidator.validate(command.pin());
        var hashed = pinHasherPort.hash(command.pin());
        Card card = Card.activeNew(account.id(), command.cardUid(), hashed);
        card = cardRepositoryPort.save(card);

        // 4) frais d’enrôlement + 5) commission agent (policies)
        Money cardPrice = feePolicyPort.cardEnrollmentPrice();
        Money agentCommission = commissionPolicyPort.cardEnrollmentAgentCommission();

        BigDecimal price = cardPrice.asBigDecimal();
        BigDecimal commission = agentCommission.asBigDecimal();

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ForbiddenOperationException("Invalid fee policy: cardEnrollmentPrice cannot be negative");
        }
        if (commission.compareTo(BigDecimal.ZERO) < 0) {
            throw new ForbiddenOperationException("Invalid commission policy: cardEnrollmentAgentCommission cannot be negative");
        }
        if (commission.compareTo(price) > 0) {
            throw new ForbiddenOperationException("Invalid commission policy: agentCommission cannot exceed cardPrice");
        }

        Money platformRevenue = cardPrice.minus(agentCommission); // never negative due to guards

        // 6) transaction
        Transaction tx = Transaction.enrollCard(cardPrice, now);
        tx = transactionRepositoryPort.save(tx);

        // 7) ledger (revenu + commission)
        ledgerAppendPort.append(List.of(
                LedgerEntry.credit(tx.id(), LedgerAccount.PLATFORM, platformRevenue, null),
                LedgerEntry.credit(tx.id(), LedgerAccount.AGENT, agentCommission, command.agentId())
        ));

        // 8) audit
        Map<String, String> metadata = new HashMap<>();
        metadata.put("agentId", command.agentId());
        metadata.put("transactionId", tx.id().value());

        AuditEvent event = new AuditEvent(
                "ENROLL_CARD",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        );
        auditPort.publish(event);

        EnrollCardResult result = new EnrollCardResult(
                tx.id().value(),
                client.id().value(),
                account.id().value(),
                card.id().value(),
                cardPrice.asBigDecimal(),
                agentCommission.asBigDecimal(),
                clientCreated,
                accountCreated
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}

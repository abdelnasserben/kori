package com.kori.application.usecase;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.security.ActorType;
import com.kori.application.security.PinFormatValidator;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
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
    private final CardRepositoryPort cardRepositoryPort;

    private final AgentRepositoryPort agentRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    private final AccountProfilePort accountProfilePort;

    private final FeePolicyPort feePolicyPort;
    private final CommissionPolicyPort commissionPolicyPort;

    private final LedgerAppendPort ledgerAppendPort;
    private final AuditPort auditPort;
    private final PinHasherPort pinHasherPort;

    public EnrollCardService(TimeProviderPort timeProviderPort,
                             IdempotencyPort idempotencyPort,
                             ClientRepositoryPort clientRepositoryPort,
                             CardRepositoryPort cardRepositoryPort,
                             AgentRepositoryPort agentRepositoryPort,
                             TransactionRepositoryPort transactionRepositoryPort,
                             AccountProfilePort accountProfilePort,
                             FeePolicyPort feePolicyPort,
                             CommissionPolicyPort commissionPolicyPort,
                             LedgerAppendPort ledgerAppendPort,
                             AuditPort auditPort,
                             PinHasherPort pinHasherPort) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.accountProfilePort = accountProfilePort;
        this.feePolicyPort = feePolicyPort;
        this.commissionPolicyPort = commissionPolicyPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.auditPort = auditPort;
        this.pinHasherPort = pinHasherPort;
    }

    @Override
    public EnrollCardResult execute(EnrollCardCommand command) {

        // 0) Idempotence
        var cached = idempotencyPort.find(command.idempotencyKey(), EnrollCardResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 1) Authorization conceptuelle : enrôlement effectué par un AGENT
        if (command.actorContext().actorType() != ActorType.AGENT) {
            throw new ForbiddenOperationException("Only AGENT can enroll a card");
        }

        // 2) Agent must exist (by code) + should be ACTIVE
        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(command.agentCode()))
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        var agentAccount = LedgerAccountRef.agent(agent.id().value().toString());
        var agentProfile = accountProfilePort.findByAccount(agentAccount)
                .orElseThrow(() -> new NotFoundException("Agent account not found"));

        if (agentProfile.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("Agent is not active");
        }

        // 3) Card UID must be unique
        if (cardRepositoryPort.findByCardUid(command.cardUid()).isPresent()) {
            throw new ForbiddenOperationException("Card UID already enrolled");
        }

        Instant now = timeProviderPort.now();

        // 4) client (find or create by phone)
        boolean clientCreated = false;
        Client client = clientRepositoryPort.findByPhoneNumber(command.phoneNumber()).orElse(null);

        if (client == null) {
            client = Client.activeNew(command.phoneNumber());
            client = clientRepositoryPort.save(client);
            clientCreated = true;
        }

        // 5) client accountRef profile (create if absent)
        boolean clientAccountProfileCreated = false;
        var clientAccount = LedgerAccountRef.client(client.id().toString()); // adapt if value() is not String
        var clientProfileOpt = accountProfilePort.findByAccount(clientAccount);

        if (clientProfileOpt.isEmpty()) {
            AccountProfile profile = AccountProfile.activeNew(clientAccount, now);
            accountProfilePort.save(profile);
            clientAccountProfileCreated = true;
        } else {
            if (clientProfileOpt.get().status() != Status.ACTIVE) {
                throw new ForbiddenOperationException("Client account is not active");
            }
        }

        // 6) Card (active) linked to ClientId (no AccountId anymore)
        PinFormatValidator.validate(command.pin());
        var hashed = pinHasherPort.hash(command.pin());

        Card card = Card.activeNew(client.id(), command.cardUid(), hashed);
        card = cardRepositoryPort.save(card);

        // 7) pricing / commissions policies
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

        Money platformRevenue = cardPrice.minus(agentCommission); // safe due to guards

        // 8) transaction
        Transaction tx = Transaction.enrollCard(cardPrice, now);
        tx = transactionRepositoryPort.save(tx);

        // 9) ledger entries (platform revenue + agent commission)
        ledgerAppendPort.append(List.of(
                LedgerEntry.credit(tx.id(), LedgerAccountRef.platformFeeRevenue(), platformRevenue),
                LedgerEntry.credit(tx.id(), agentAccount, agentCommission)
        ));

        // 10) audit
        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", tx.id().toString());
        metadata.put("agentCode", command.agentCode());
        metadata.put("clientPhoneNumber", client.phoneNumber());
        metadata.put("cardUid", card.cardUid());

        AuditEvent event = new AuditEvent(
                "ENROLL_CARD",
                command.actorContext().actorType().name(),
                command.actorContext().actorId(),
                now,
                metadata
        );
        auditPort.publish(event);

        EnrollCardResult result = new EnrollCardResult(
                tx.id().toString(),
                client.phoneNumber(),
                card.cardUid(),
                cardPrice.asBigDecimal(),
                agentCommission.asBigDecimal(),
                clientCreated,
                clientAccountProfileCreated
        );

        idempotencyPort.save(command.idempotencyKey(), result);
        return result;
    }
}

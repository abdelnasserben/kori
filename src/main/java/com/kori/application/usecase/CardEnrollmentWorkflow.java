package com.kori.application.usecase;

import com.kori.application.guard.AgentCashLimitGuard;
import com.kori.application.guard.PricingGuards;
import com.kori.application.port.out.*;
import com.kori.application.security.PinFormatValidator;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

final class CardEnrollmentWorkflow {

    private final IdGeneratorPort idGeneratorPort;
    private final CardRepositoryPort cardRepositoryPort;
    private final AgentRepositoryPort agentRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;
    private final FeePolicyPort feePolicyPort;
    private final CommissionPolicyPort commissionPolicyPort;
    private final LedgerAppendPort ledgerAppendPort;
    private final AgentCashLimitGuard agentCashLimitGuard;
    private final PinHasherPort pinHasherPort;

    CardEnrollmentWorkflow(
            IdGeneratorPort idGeneratorPort,
            CardRepositoryPort cardRepositoryPort,
            AgentRepositoryPort agentRepositoryPort,
            TransactionRepositoryPort transactionRepositoryPort,
            FeePolicyPort feePolicyPort,
            CommissionPolicyPort commissionPolicyPort,
            LedgerAppendPort ledgerAppendPort,
            LedgerQueryPort ledgerQueryPort,
            PlatformConfigPort platformConfigPort,
            PinHasherPort pinHasherPort
    ) {
        this.idGeneratorPort = Objects.requireNonNull(idGeneratorPort);
        this.cardRepositoryPort = Objects.requireNonNull(cardRepositoryPort);
        this.agentRepositoryPort = Objects.requireNonNull(agentRepositoryPort);
        this.transactionRepositoryPort = Objects.requireNonNull(transactionRepositoryPort);
        this.feePolicyPort = Objects.requireNonNull(feePolicyPort);
        this.commissionPolicyPort = Objects.requireNonNull(commissionPolicyPort);
        this.ledgerAppendPort = Objects.requireNonNull(ledgerAppendPort);
        this.agentCashLimitGuard = new AgentCashLimitGuard(ledgerQueryPort, platformConfigPort);
        this.pinHasherPort = Objects.requireNonNull(pinHasherPort);
    }

    CardEnrollmentOutcome enrollCard(Client client, Agent agent, String cardUid, String pin, Instant now) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(agent, "agent");
        Objects.requireNonNull(cardUid, "cardUid");
        Objects.requireNonNull(pin, "pin");
        Objects.requireNonNull(now, "now");

        PinFormatValidator.validate(pin);
        var hashed = pinHasherPort.hash(pin);

        Card card = Card.activeNew(client.id(), cardUid, hashed, now);
        card = cardRepositoryPort.save(card);

        Money cardPrice = feePolicyPort.cardEnrollmentPrice();
        Money agentCommission = commissionPolicyPort.cardEnrollmentAgentCommission();

        var breakdown = PricingGuards.feeMinusCommission(cardPrice, agentCommission, "ENROLL_CARD");
        Money platformRevenue = breakdown.platformRevenue();

        TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
        Transaction tx = Transaction.enrollCard(txId, cardPrice, now);
        tx = transactionRepositoryPort.save(tx);

        agentRepositoryPort.findByIdForUpdate(agent.id());
        agentCashLimitGuard.ensureProjectedBalanceWithinLimit(agent.id().value().toString(), cardPrice, Money.zero());

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), LedgerAccountRef.agentCashClearing(agent.id().value().toString()), cardPrice),
                LedgerEntry.credit(tx.id(), LedgerAccountRef.agentWallet(agent.id().value().toString()), agentCommission),
                LedgerEntry.credit(tx.id(), LedgerAccountRef.platformFeeRevenue(), platformRevenue)
        ));

        return new CardEnrollmentOutcome(card, tx, cardPrice, agentCommission);
    }

    record CardEnrollmentOutcome(Card card, Transaction transaction, Money cardPrice, Money agentCommission) {}
}

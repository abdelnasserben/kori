package com.kori.application.usecase;

import com.kori.application.command.EnrollCardCommand;
import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.exception.NotFoundException;
import com.kori.application.guard.ActorGuards;
import com.kori.application.guard.AgentCashLimitGuard;
import com.kori.application.guard.OperationStatusGuards;
import com.kori.application.guard.PricingGuards;
import com.kori.application.port.in.EnrollCardUseCase;
import com.kori.application.port.out.*;
import com.kori.application.result.EnrollCardResult;
import com.kori.application.security.PinFormatValidator;
import com.kori.application.utils.AuditBuilder;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.account.AccountProfile;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.agent.AgentCode;
import com.kori.domain.model.card.Card;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.client.ClientId;
import com.kori.domain.model.common.Money;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class EnrollCardService implements EnrollCardUseCase {

    private final TimeProviderPort timeProviderPort;
    private final IdempotencyPort idempotencyPort;
    private final IdGeneratorPort idGeneratorPort;

    private final ClientRepositoryPort clientRepositoryPort;
    private final CardRepositoryPort cardRepositoryPort;

    private final AgentRepositoryPort agentRepositoryPort;
    private final TransactionRepositoryPort transactionRepositoryPort;

    private final AccountProfilePort accountProfilePort;

    private final FeePolicyPort feePolicyPort;
    private final CommissionPolicyPort commissionPolicyPort;

    private final LedgerAppendPort ledgerAppendPort;
    private final AgentCashLimitGuard agentCashLimitGuard;
    private final AuditPort auditPort;
    private final PinHasherPort pinHasherPort;
    private final OperationStatusGuards operationStatusGuards;

    public EnrollCardService(TimeProviderPort timeProviderPort,
                             IdempotencyPort idempotencyPort,
                             IdGeneratorPort idGeneratorPort,
                             ClientRepositoryPort clientRepositoryPort,
                             CardRepositoryPort cardRepositoryPort,
                             AgentRepositoryPort agentRepositoryPort,
                             TransactionRepositoryPort transactionRepositoryPort,
                             AccountProfilePort accountProfilePort,
                             FeePolicyPort feePolicyPort,
                             CommissionPolicyPort commissionPolicyPort,
                             LedgerAppendPort ledgerAppendPort,
                             LedgerQueryPort ledgerQueryPort,
                             PlatformConfigPort platformConfigPort,
                             AuditPort auditPort,
                             PinHasherPort pinHasherPort,
                             OperationStatusGuards operationStatusGuards) {
        this.timeProviderPort = timeProviderPort;
        this.idempotencyPort = idempotencyPort;
        this.idGeneratorPort = idGeneratorPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.cardRepositoryPort = cardRepositoryPort;
        this.agentRepositoryPort = agentRepositoryPort;
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.accountProfilePort = accountProfilePort;
        this.feePolicyPort = feePolicyPort;
        this.commissionPolicyPort = commissionPolicyPort;
        this.ledgerAppendPort = ledgerAppendPort;
        this.agentCashLimitGuard = new AgentCashLimitGuard(ledgerQueryPort, platformConfigPort);
        this.auditPort = auditPort;
        this.pinHasherPort = pinHasherPort;
        this.operationStatusGuards = operationStatusGuards;
    }

    @Override
    public EnrollCardResult execute(EnrollCardCommand command) {

        // 0) Idempotence
        var cached = idempotencyPort.find(command.idempotencyKey(), command.idempotencyRequestHash(), EnrollCardResult.class);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 1) Authorization: enrollment must be initiated by an AGENT
        ActorGuards.requireAgent(command.actorContext(), "enroll a card");

        // 2) Agent must exist (by code) + must be ACTIVE
        Agent agent = agentRepositoryPort.findByCode(AgentCode.of(command.agentCode()))
                .orElseThrow(() -> new NotFoundException("Agent not found"));

        operationStatusGuards.requireActiveAgent(agent);

        var agentWalletAccount = LedgerAccountRef.agentWallet(agent.id().value().toString());

        // 3) Card UID must be unique
        if (cardRepositoryPort.findByCardUid(command.cardUid()).isPresent()) {
            throw new ForbiddenOperationException("Card UID already enrolled");
        }

        Instant now = timeProviderPort.now();

        // 4) client (find or create by phone)
        boolean clientCreated = false;
        Optional<Client> existingClient = clientRepositoryPort.findByPhoneNumber(command.phoneNumber());
        existingClient.ifPresent(operationStatusGuards::requireClientEligibleForEnroll);

        Client client = existingClient.orElse(null);

        if (client == null) {
            client = Client.activeNew(new ClientId(idGeneratorPort.newUuid()), command.phoneNumber(), now);
            client = clientRepositoryPort.save(client);
            clientCreated = true;
        }

        // 5) client AccountProfile (create if absent)
        boolean clientAccountProfileCreated = false;
        var clientAccount = LedgerAccountRef.client(client.id().value().toString());
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

        // 6) Card (active) linked to ClientId
        PinFormatValidator.validate(command.pin());
        var hashed = pinHasherPort.hash(command.pin());

        Card card = Card.activeNew(client.id(), command.cardUid(), hashed, now);
        card = cardRepositoryPort.save(card);

        // 7) pricing / commissions
        Money cardPrice = feePolicyPort.cardEnrollmentPrice();
        Money agentCommission = commissionPolicyPort.cardEnrollmentAgentCommission();

        var breakdown = PricingGuards.feeMinusCommission(cardPrice, agentCommission, "ENROLL_CARD");
        Money platformRevenue = breakdown.platformRevenue();

        // 8) transaction
        TransactionId txId = new TransactionId(idGeneratorPort.newUuid());
        Transaction tx = Transaction.enrollCard(txId, cardPrice, now);
        tx = transactionRepositoryPort.save(tx);

        // 9) ledger entries
        agentRepositoryPort.findByIdForUpdate(agent.id());
        agentCashLimitGuard.ensureProjectedBalanceWithinLimit(agent.id().value().toString(), cardPrice, Money.zero());

        ledgerAppendPort.append(List.of(
                LedgerEntry.debit(tx.id(), LedgerAccountRef.agentCashClearing(agent.id().value().toString()), cardPrice),
                LedgerEntry.credit(tx.id(), agentWalletAccount, agentCommission),
                LedgerEntry.credit(tx.id(), LedgerAccountRef.platformFeeRevenue(), platformRevenue)
        ));

        // 10) audit
        Map<String, String> metadata = new HashMap<>();
        metadata.put("transactionId", tx.id().value().toString());
        metadata.put("agentCode", command.agentCode());
        metadata.put("clientPhoneNumber", client.phoneNumber());
        metadata.put("cardUid", card.cardUid());

        auditPort.publish(AuditBuilder.buildBasicAudit(
                "ENROLL_CARD",
                command.actorContext(),
                now,
                metadata
        ));

        EnrollCardResult result = new EnrollCardResult(
                tx.id().value().toString(),
                client.phoneNumber(),
                card.cardUid(),
                cardPrice.asBigDecimal(),
                agentCommission.asBigDecimal(),
                clientCreated,
                clientAccountProfileCreated
        );

        idempotencyPort.save(command.idempotencyKey(), command.idempotencyRequestHash(), result);
        return result;
    }
}

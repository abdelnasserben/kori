package com.kori.application.usecase;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.guard.ActorStatusGuards;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;

import java.util.Objects;

/**
 * Centralise toutes les règles de statut pour autoriser ou non une opération métier.
 * Aucun effet de bord : uniquement des guards.
 */
public class OperationAuthorizationService {

    private final AccountProfilePort accountProfilePort;

    public OperationAuthorizationService(AccountProfilePort accountProfilePort) {
        this.accountProfilePort = Objects.requireNonNull(accountProfilePort);
    }

    public void authorizeClientPayment(Client client) {
        ActorStatusGuards.requireActiveClient(client);

        LedgerAccountRef ref =
                LedgerAccountRef.client(client.id().value().toString());

        accountProfilePort.findByAccount(ref)
                .filter(p -> p.status() == Status.ACTIVE)
                .orElseThrow(() -> new ForbiddenOperationException("CLIENT_ACCOUNT_INACTIVE_OR_MISSING"));

    }

    public void authorizeMerchantPayment(Merchant merchant) {
        ActorStatusGuards.requireActiveMerchant(merchant);

        LedgerAccountRef ref =
                LedgerAccountRef.merchant(merchant.id().value().toString());

        accountProfilePort.findByAccount(ref)
                .filter(p -> p.status() == Status.ACTIVE)
                .orElseThrow(() -> new ForbiddenOperationException("MERCHANT_ACCOUNT_INACTIVE_OR_MISSING"));

    }

    public void authorizeAgentOperation(Agent agent) {
        ActorStatusGuards.requireActiveAgent(agent);

        String agentId = agent.id().value().toString();

        LedgerAccountRef walletRef = LedgerAccountRef.agentWallet(agentId);
        LedgerAccountRef clearingRef = LedgerAccountRef.agentCashClearing(agentId);

        accountProfilePort.findByAccount(walletRef)
                .filter(p -> p.status() == Status.ACTIVE)
                .orElseThrow(() -> new ForbiddenOperationException("AGENT_ACCOUNT_INACTIVE_OR_MISSING"));

        accountProfilePort.findByAccount(clearingRef)
                .filter(p -> p.status() == Status.ACTIVE)
                .orElseThrow(() -> new ForbiddenOperationException("AGENT_ACCOUNT_INACTIVE_OR_MISSING"));

    }
}

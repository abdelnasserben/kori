package com.kori.application.guard;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.application.port.out.AccountProfilePort;
import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.agent.Agent;
import com.kori.domain.model.client.Client;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;

import java.util.Objects;

/**
 * Centralise toutes les règles de statut pour autoriser ou non
 * une opération métier.
 * Aucun effet de bord : uniquement des guards.
 */
public class OperationStatusGuards {

    private final AccountProfilePort accountProfilePort;

    public OperationStatusGuards(AccountProfilePort accountProfilePort) {
        this.accountProfilePort = Objects.requireNonNull(accountProfilePort);
    }

    /* =========================
       PAY BY CARD
       ========================= */

    public void requireActiveClient(Client client) {
        if (client.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("CLIENT_NOT_ACTIVE");
        }

        LedgerAccountRef ref =
                LedgerAccountRef.client(client.id().value().toString());

        accountProfilePort.findByAccount(ref)
                .filter(p -> p.status() == Status.ACTIVE)
                .orElseThrow(() -> new ForbiddenOperationException("CLIENT_ACCOUNT_INACTIVE_OR_MISSING"));

    }

    public void requireActiveMerchant(Merchant merchant) {
        if (merchant.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("MERCHANT_NOT_ACTIVE");
        }

        LedgerAccountRef ref =
                LedgerAccountRef.merchant(merchant.id().value().toString());

        accountProfilePort.findByAccount(ref)
                .filter(p -> p.status() == Status.ACTIVE)
                .orElseThrow(() -> new ForbiddenOperationException("MERCHANT_ACCOUNT_INACTIVE_OR_MISSING"));

    }

    /* =========================
       AGENT OPERATIONS
       ========================= */

    public void requireActiveAgent(Agent agent) {
        if (agent.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("AGENT_NOT_ACTIVE");
        }

        LedgerAccountRef ref =
                LedgerAccountRef.agent(agent.id().value().toString());

        accountProfilePort.findByAccount(ref)
                .filter(p -> p.status() == Status.ACTIVE)
                .orElseThrow(() -> new ForbiddenOperationException("AGENT_ACCOUNT_INACTIVE_OR_MISSING"));

    }

    /* =========================
       ENROLL CARD
       ========================= */

    public void requireClientEligibleForEnroll(Client client) {
        if (client.status() != Status.ACTIVE) {
            throw new ForbiddenOperationException("CLIENT_NOT_ACTIVE");
        }
    }
}

package com.kori.application.security;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.domain.ledger.LedgerAccount;

import java.util.Objects;

/**
 * Application-layer authorization for read-only ledger consultation.
 * Domain stays actor-agnostic; this policy uses {@link ActorContext}.
 */
public final class LedgerAccessPolicy {

    public void assertCanReadLedger(ActorContext actorContext, LedgerAccount ledgerAccount, String referenceId) {
        Objects.requireNonNull(actorContext);
        Objects.requireNonNull(ledgerAccount);
        Objects.requireNonNull(referenceId);

        switch (actorContext.actorType()) {
            case CLIENT -> {
                if (ledgerAccount != LedgerAccount.CLIENT || !actorContext.actorId().equals(referenceId)) {
                    throw new ForbiddenOperationException("Client can only consult their own ledger");
                }
            }
            case MERCHANT -> {
                if (ledgerAccount != LedgerAccount.MERCHANT || !actorContext.actorId().equals(referenceId)) {
                    throw new ForbiddenOperationException("Merchant can only consult their own ledger");
                }
            }
            case AGENT -> {
                if (ledgerAccount != LedgerAccount.AGENT || !actorContext.actorId().equals(referenceId)) {
                    throw new ForbiddenOperationException("Agent can only consult their own ledger");
                }
            }
            case ADMIN -> {} // Admin can consult any ledger scope.
            default -> throw new ForbiddenOperationException("Actor type cannot consult ledger");
        }
    }
}

package com.kori.application.security;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.domain.ledger.LedgerAccountRef;

import java.util.Objects;

/**
 * Application-layer authorization for read-only ledger consultation.
 * Domain stays actor-agnostic; this policy uses {@link ActorContext}.
 */
public final class LedgerAccessPolicy {

    public void assertCanReadLedger(ActorContext actorContext, LedgerAccountRef scope) {
        Objects.requireNonNull(actorContext, "actorContext");
        Objects.requireNonNull(scope, "scope");

        switch (actorContext.actorType()) {
            case CLIENT -> {
                if (!scope.isForClient() || !actorContext.actorRef().equals(scope.ownerRef())) {
                    throw new ForbiddenOperationException("Client can only consult their own ledger");
                }
            }
            case MERCHANT -> {
                if (!scope.isForMerchant() || !actorContext.actorRef().equals(scope.ownerRef())) {
                    throw new ForbiddenOperationException("Merchant can only consult their own ledger");
                }
            }
            case AGENT -> {
                if (!scope.isForAgent() || !actorContext.actorRef().equals(scope.ownerRef())) {
                    throw new ForbiddenOperationException("Agent can only consult their own ledger");
                }
            }
            case ADMIN -> {
                // Admin can consult any ledger scope.
            }
            default -> throw new ForbiddenOperationException("Actor type cannot consult ledger");
        }
    }
}

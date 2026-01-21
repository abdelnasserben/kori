package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record EnrollCardResult(String transactionId, String clientId, String accountId, String cardId,
                               BigDecimal cardPrice, BigDecimal agentCommission, boolean clientCreated,
                               boolean accountCreated) {
    public EnrollCardResult(String transactionId,
                            String clientId,
                            String accountId,
                            String cardId,
                            BigDecimal cardPrice,
                            BigDecimal agentCommission,
                            boolean clientCreated,
                            boolean accountCreated) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.clientId = Objects.requireNonNull(clientId);
        this.accountId = Objects.requireNonNull(accountId);
        this.cardId = Objects.requireNonNull(cardId);
        this.cardPrice = Objects.requireNonNull(cardPrice);
        this.agentCommission = Objects.requireNonNull(agentCommission);
        this.clientCreated = clientCreated;
        this.accountCreated = accountCreated;
    }
}

package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record EnrollCardResult(String transactionId, String clientPhoneNumber, String cardUid,
                               BigDecimal cardPrice, BigDecimal agentCommission, boolean clientCreated,
                               boolean accountCreated) {
    public EnrollCardResult(String transactionId,
                            String clientPhoneNumber,
                            String cardUid,
                            BigDecimal cardPrice,
                            BigDecimal agentCommission,
                            boolean clientCreated,
                            boolean accountCreated) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.clientPhoneNumber = Objects.requireNonNull(clientPhoneNumber);
        this.cardUid = Objects.requireNonNull(cardUid);
        this.cardPrice = Objects.requireNonNull(cardPrice);
        this.agentCommission = Objects.requireNonNull(agentCommission);
        this.clientCreated = clientCreated;
        this.accountCreated = accountCreated;
    }
}

package com.kori.application.result;

import java.math.BigDecimal;

public record AddCardToExistingClientResult(
        String transactionId,
        String clientId,
        String cardUid,
        BigDecimal cardPrice,
        BigDecimal agentCommission
) {}

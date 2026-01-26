package com.kori.application.result;

import java.math.BigDecimal;
import java.util.Objects;

public record MerchantWithdrawAtAgentResult(String transactionId, String merchantCode, String agentCode, BigDecimal amount,
                                            BigDecimal fee, BigDecimal commission, BigDecimal totalDebitedMerchant) {
    public MerchantWithdrawAtAgentResult(String transactionId,
                                         String merchantCode,
                                         String agentCode,
                                         BigDecimal amount,
                                         BigDecimal fee,
                                         BigDecimal commission,
                                         BigDecimal totalDebitedMerchant) {
        this.transactionId = Objects.requireNonNull(transactionId);
        this.merchantCode = Objects.requireNonNull(merchantCode);
        this.agentCode = Objects.requireNonNull(agentCode);
        this.amount = Objects.requireNonNull(amount);
        this.fee = Objects.requireNonNull(fee);
        this.commission = Objects.requireNonNull(commission);
        this.totalDebitedMerchant = Objects.requireNonNull(totalDebitedMerchant);
    }
}

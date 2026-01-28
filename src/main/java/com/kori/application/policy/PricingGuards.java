package com.kori.application.policy;

import com.kori.application.exception.ForbiddenOperationException;
import com.kori.domain.model.common.Money;

import java.math.BigDecimal;
import java.util.Objects;

public final class PricingGuards {

    private PricingGuards() {}

    public static PricingBreakdown feeMinusCommission(Money fee, Money commission, String context) {
        Objects.requireNonNull(fee, "fee");
        Objects.requireNonNull(commission, "commission");

        BigDecimal f = fee.asBigDecimal();
        BigDecimal c = commission.asBigDecimal();

        if (f.compareTo(BigDecimal.ZERO) < 0) {
            throw new ForbiddenOperationException(context + ": fee cannot be negative");
        }
        if (c.compareTo(BigDecimal.ZERO) < 0) {
            throw new ForbiddenOperationException(context + ": commission cannot be negative");
        }
        if (c.compareTo(f) > 0) {
            throw new ForbiddenOperationException(context + ": commission cannot exceed fee");
        }

        return new PricingBreakdown(fee, commission, fee.minus(commission));
    }

    public record PricingBreakdown(Money fee, Money commission, Money platformRevenue) {}
}

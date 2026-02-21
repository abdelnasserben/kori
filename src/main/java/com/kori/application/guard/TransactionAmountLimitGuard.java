package com.kori.application.guard;

import com.kori.application.exception.ValidationException;
import com.kori.domain.model.common.Money;

import java.util.Map;
import java.util.Objects;

public final class TransactionAmountLimitGuard {

    private TransactionAmountLimitGuard() {}

    public static void ensureMinPerTransaction(Money amount, Money minPerTransaction, String operationType) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(minPerTransaction, "minPerTransaction");
        Objects.requireNonNull(operationType, "operationType");

        if (amount.asBigDecimal().compareTo(minPerTransaction.asBigDecimal()) < 0) {
            throw new ValidationException(operationType + " min per transaction not reached", Map.of(
                    "amount", amount.asBigDecimal(),
                    "minPerTransaction", minPerTransaction.asBigDecimal()
            ));
        }
    }

    public static void ensureMaxPerTransaction(Money amount, Money maxPerTransaction, String operationType) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(maxPerTransaction, "maxPerTransaction");
        Objects.requireNonNull(operationType, "operationType");

        if (amount.isGreaterThan(maxPerTransaction)) {
            throw new ValidationException(operationType + " max per transaction exceeded", Map.of(
                    "amount", amount.asBigDecimal(),
                    "maxPerTransaction", maxPerTransaction.asBigDecimal()
            ));
        }
    }
}

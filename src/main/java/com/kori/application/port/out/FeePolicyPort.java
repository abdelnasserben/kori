package com.kori.application.port.out;

import com.kori.domain.model.common.Money;

public interface FeePolicyPort {
    Money cardEnrollmentPrice();

    Money cardPaymentFee(Money amount);

    Money merchantWithdrawFee(Money amount);

    Money clientTransferFee(Money amount);

    Money merchantTransferFee(Money amount);
}

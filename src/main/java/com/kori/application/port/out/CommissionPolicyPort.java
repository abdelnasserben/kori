package com.kori.application.port.out;

import com.kori.domain.model.common.Money;

public interface CommissionPolicyPort {
    Money cardEnrollmentAgentCommission();

    Money merchantWithdrawAgentCommission(Money fee);
}

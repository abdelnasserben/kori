package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.repo.CommissionConfigJpaRepository;
import com.kori.application.port.out.CommissionPolicyPort;
import com.kori.domain.model.common.Money;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
public class JpaCommissionPolicyAdapter implements CommissionPolicyPort {

    private final CommissionConfigJpaRepository repo;

    public JpaCommissionPolicyAdapter(CommissionConfigJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Money cardEnrollmentAgentCommission() {
        var cfg = repo.findById(1).orElseThrow(() -> new IllegalStateException("commission_config id=1 missing"));
        return Money.of(cfg.getCardEnrollmentAgentCommission());
    }

    @Override
    @Transactional(readOnly = true)
    public Money merchantWithdrawAgentCommission(Money fee) {
        var cfg = repo.findById(1).orElseThrow(() -> new IllegalStateException("commission_config id=1 missing"));

        BigDecimal rate = cfg.getMerchantWithdrawCommissionRate();
        BigDecimal v = fee.asBigDecimal().multiply(rate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal min = cfg.getMerchantWithdrawCommissionMin();
        BigDecimal max = cfg.getMerchantWithdrawCommissionMax();

        if (min != null && v.compareTo(min) < 0) v = min;
        if (max != null && v.compareTo(max) > 0) v = max;

        // guard: commission <= fee
        if (v.compareTo(fee.asBigDecimal()) > 0) v = fee.asBigDecimal();

        return Money.of(v);
    }
}

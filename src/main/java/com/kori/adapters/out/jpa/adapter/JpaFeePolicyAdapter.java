package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.repo.FeeConfigJpaRepository;
import com.kori.application.port.out.FeePolicyPort;
import com.kori.domain.model.common.Money;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Component
public class JpaFeePolicyAdapter implements FeePolicyPort {

    private final FeeConfigJpaRepository repo;

    public JpaFeePolicyAdapter(FeeConfigJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Money cardEnrollmentPrice() {
        var cfg = repo.findById(1).orElseThrow(() -> new IllegalStateException("fee_config id=1 missing"));
        return Money.of(cfg.getCardEnrollmentPrice());
    }

    @Override
    @Transactional(readOnly = true)
    public Money cardPaymentFee(Money amount) {
        var cfg = repo.findById(1).orElseThrow(() -> new IllegalStateException("fee_config id=1 missing"));
        return percentMinMax(amount, cfg.getCardPaymentFeeRate(), cfg.getCardPaymentFeeMin(), cfg.getCardPaymentFeeMax());
    }

    @Override
    @Transactional(readOnly = true)
    public Money merchantWithdrawFee(Money amount) {
        var cfg = repo.findById(1).orElseThrow(() -> new IllegalStateException("fee_config id=1 missing"));
        return percentMinMax(amount, cfg.getMerchantWithdrawFeeRate(), cfg.getMerchantWithdrawFeeMin(), cfg.getMerchantWithdrawFeeMax());
    }

    private static Money percentMinMax(Money amount, BigDecimal rate, BigDecimal min, BigDecimal max) {
        BigDecimal v = amount.asBigDecimal().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        if (v.compareTo(min) < 0) v = min;
        if (v.compareTo(max) > 0) v = max;
        return Money.of(v);
    }
}

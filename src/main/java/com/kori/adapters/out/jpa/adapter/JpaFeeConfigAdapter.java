package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.FeeConfigEntity;
import com.kori.adapters.out.jpa.repo.FeeConfigJpaRepository;
import com.kori.application.port.out.FeeConfigPort;
import com.kori.domain.model.config.FeeConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaFeeConfigAdapter implements FeeConfigPort {

    private static final int CONFIG_ID = 1;

    private final FeeConfigJpaRepository repo;

    public JpaFeeConfigAdapter(FeeConfigJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FeeConfig> get() {
        return repo.findById(CONFIG_ID).map(cfg ->
                new FeeConfig(
                        cfg.getCardEnrollmentPrice(),
                        cfg.getCardPaymentFeeRate(),
                        cfg.getCardPaymentFeeMin(),
                        cfg.getCardPaymentFeeMax(),
                        cfg.getMerchantWithdrawFeeRate(),
                        cfg.getMerchantWithdrawFeeMin(),
                        cfg.getMerchantWithdrawFeeMax()
                )
        );
    }

    @Override
    @Transactional
    public void upsert(FeeConfig config) {
        repo.save(new FeeConfigEntity(
                CONFIG_ID,
                config.cardEnrollmentPrice(),
                config.cardPaymentFeeRate(),
                config.cardPaymentFeeMin(),
                config.cardPaymentFeeMax(),
                config.merchantWithdrawFeeRate(),
                config.merchantWithdrawFeeMin(),
                config.merchantWithdrawFeeMax()
        ));
    }
}
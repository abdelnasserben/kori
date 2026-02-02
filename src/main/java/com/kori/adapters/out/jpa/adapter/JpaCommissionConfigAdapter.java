package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.CommissionConfigEntity;
import com.kori.adapters.out.jpa.repo.CommissionConfigJpaRepository;
import com.kori.application.port.out.CommissionConfigPort;
import com.kori.domain.model.config.CommissionConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaCommissionConfigAdapter implements CommissionConfigPort {

    private static final int CONFIG_ID = 1;

    private final CommissionConfigJpaRepository repo;

    public JpaCommissionConfigAdapter(CommissionConfigJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommissionConfig> get() {
        return repo.findById(CONFIG_ID).map(cfg ->
                new CommissionConfig(
                        cfg.getCardEnrollmentAgentCommission(),
                        cfg.getMerchantWithdrawCommissionRate(),
                        cfg.getMerchantWithdrawCommissionMin(),
                        cfg.getMerchantWithdrawCommissionMax()
                )
        );
    }

    @Override
    @Transactional
    public void upsert(CommissionConfig config) {
        repo.save(new CommissionConfigEntity(
                CONFIG_ID,
                config.cardEnrollmentAgentCommission(),
                config.merchantWithdrawCommissionRate(),
                config.merchantWithdrawCommissionMin(),
                config.merchantWithdrawCommissionMax()
        ));
    }
}
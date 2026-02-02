package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.repo.CommissionConfigJpaRepository;
import com.kori.application.port.out.CommissionConfigPort;
import com.kori.domain.model.config.CommissionConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaCommissionConfigAdapter implements CommissionConfigPort {

    private static final int CONFIG_ID = 1;

    private final CommissionConfigJpaRepository repo;

    @PersistenceContext
    private EntityManager em;

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
        em.createNativeQuery("""
                INSERT INTO commission_config (
                    id,
                    card_enrollment_agent_commission,
                    merchant_withdraw_commission_rate,
                    merchant_withdraw_commission_min,
                    merchant_withdraw_commission_max
                )
                VALUES (
                    ?1, ?2, ?3, ?4, ?5
                )
                ON CONFLICT (id) DO UPDATE SET
                    card_enrollment_agent_commission = EXCLUDED.card_enrollment_agent_commission,
                    merchant_withdraw_commission_rate = EXCLUDED.merchant_withdraw_commission_rate,
                    merchant_withdraw_commission_min = EXCLUDED.merchant_withdraw_commission_min,
                    merchant_withdraw_commission_max = EXCLUDED.merchant_withdraw_commission_max
                """)
                .setParameter(1, CONFIG_ID)
                .setParameter(2, config.cardEnrollmentAgentCommission())
                .setParameter(3, config.merchantWithdrawCommissionRate())
                .setParameter(4, config.merchantWithdrawCommissionMin())
                .setParameter(5, config.merchantWithdrawCommissionMax())
                .executeUpdate();
    }
}

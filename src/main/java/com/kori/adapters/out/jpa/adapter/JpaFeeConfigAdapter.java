package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.repo.FeeConfigJpaRepository;
import com.kori.application.port.out.FeeConfigPort;
import com.kori.domain.model.config.FeeConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaFeeConfigAdapter implements FeeConfigPort {

    private static final int CONFIG_ID = 1;

    private final FeeConfigJpaRepository repo;

    @PersistenceContext
    private EntityManager em;

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
        em.createNativeQuery("""
                INSERT INTO fee_config (
                    id,
                    card_enrollment_price,
                    card_payment_fee_rate,
                    card_payment_fee_min,
                    card_payment_fee_max,
                    merchant_withdraw_fee_rate,
                    merchant_withdraw_fee_min,
                    merchant_withdraw_fee_max
                )
                VALUES (
                    ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8
                )
                ON CONFLICT (id) DO UPDATE SET
                    card_enrollment_price = EXCLUDED.card_enrollment_price,
                    card_payment_fee_rate = EXCLUDED.card_payment_fee_rate,
                    card_payment_fee_min = EXCLUDED.card_payment_fee_min,
                    card_payment_fee_max = EXCLUDED.card_payment_fee_max,
                    merchant_withdraw_fee_rate = EXCLUDED.merchant_withdraw_fee_rate,
                    merchant_withdraw_fee_min = EXCLUDED.merchant_withdraw_fee_min,
                    merchant_withdraw_fee_max = EXCLUDED.merchant_withdraw_fee_max
                """)
                .setParameter(1, CONFIG_ID)
                .setParameter(2, config.cardEnrollmentPrice())
                .setParameter(3, config.cardPaymentFeeRate())
                .setParameter(4, config.cardPaymentFeeMin())
                .setParameter(5, config.cardPaymentFeeMax())
                .setParameter(6, config.merchantWithdrawFeeRate())
                .setParameter(7, config.merchantWithdrawFeeMin())
                .setParameter(8, config.merchantWithdrawFeeMax())
                .executeUpdate();
    }
}

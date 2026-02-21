package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.PlatformConfigEntity;
import com.kori.adapters.out.jpa.repo.PlatformConfigJpaRepository;
import com.kori.application.port.out.PlatformConfigPort;
import com.kori.domain.model.config.PlatformConfig;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaPlatformConfigAdapter implements PlatformConfigPort {

    private static final int CONFIG_ID = 1;

    private final PlatformConfigJpaRepository repo;

    public JpaPlatformConfigAdapter(PlatformConfigJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlatformConfig> get() {
        return repo.findById(CONFIG_ID)
                .map(cfg -> new PlatformConfig(
                        cfg.getAgentCashLimitGlobal(),
                        cfg.getClientTransferMaxPerTransaction(),
                        cfg.getClientTransferDailyMax(),
                        cfg.getMerchantTransferMaxPerTransaction(),
                        cfg.getMerchantTransferDailyMax()
                ));
    }

    @Override
    @Transactional
    public void upsert(PlatformConfig config) {
        repo.save(new PlatformConfigEntity(
                CONFIG_ID,
                config.agentCashLimitGlobal(),
                config.clientTransferMaxPerTransaction(),
                config.clientTransferDailyMax(),
                config.merchantTransferMaxPerTransaction(),
                config.merchantTransferDailyMax()
        ));
    }
}

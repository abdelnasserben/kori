package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.repo.MerchantJpaRepository;
import com.kori.application.port.out.MerchantRepositoryPort;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Component
public class JpaMerchantRepositoryAdapter implements MerchantRepositoryPort {

    private final MerchantJpaRepository repo;

    public JpaMerchantRepositoryAdapter(MerchantJpaRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Merchant> findById(String merchantId) {
        Objects.requireNonNull(merchantId, "merchantId must not be null");
        return repo.findById(merchantId).map(e ->
                new Merchant(
                        MerchantId.of(e.getId()),
                        Status.valueOf(e.getStatus())
                )
        );
    }
}

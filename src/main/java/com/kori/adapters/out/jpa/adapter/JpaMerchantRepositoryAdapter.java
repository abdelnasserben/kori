package com.kori.adapters.out.jpa.adapter;

import com.kori.adapters.out.jpa.entity.MerchantEntity;
import com.kori.adapters.out.jpa.repo.MerchantJpaRepository;
import com.kori.application.port.out.MerchantRepositoryPort;
import com.kori.domain.model.common.Status;
import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
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
    public boolean existsByCode(MerchantCode code) {
        return repo.existsByCode(code.value());
    }

    @Override
    @Transactional
    public void save(Merchant merchant) {
        repo.save(new MerchantEntity(
                merchant.id().value(),
                merchant.code().value(),
                merchant.status().name(),
                merchant.createdAt()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Merchant> findById(MerchantId id) {
        return repo.findById(id.value()).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Merchant> findByCode(MerchantCode code) {
        return repo.findByCode(code.value()).map(this::toDomain);
    }

    private Merchant toDomain(MerchantEntity e) {
        return new Merchant(
                new MerchantId(e.getId()),
                MerchantCode.of(e.getCode()),
                Status.valueOf(e.getStatus()),
                e.getCreatedAt()
        );
    }
}

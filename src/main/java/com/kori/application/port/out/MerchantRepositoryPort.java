package com.kori.application.port.out;

import com.kori.domain.model.merchant.Merchant;
import com.kori.domain.model.merchant.MerchantCode;
import com.kori.domain.model.merchant.MerchantId;

import java.util.Optional;

public interface MerchantRepositoryPort {

    boolean existsByCode(MerchantCode code);

    void save(Merchant merchant);

    Optional<Merchant> findById(MerchantId id);

    Optional<Merchant> findByCode(MerchantCode code);
}

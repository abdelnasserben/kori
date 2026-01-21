package com.kori.application.port.out;

import com.kori.domain.model.merchant.Merchant;

import java.util.Optional;

public interface MerchantRepositoryPort {
    Optional<Merchant> findById(String merchantId);
}

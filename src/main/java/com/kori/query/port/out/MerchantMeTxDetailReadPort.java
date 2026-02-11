package com.kori.query.port.out;

import com.kori.query.model.me.MeQueryModels;

import java.util.Optional;

public interface MerchantMeTxDetailReadPort {
    Optional<MeQueryModels.MerchantTransactionDetails> findOwnedByMerchant(String merchantId, String transactionId);

    boolean existsTransaction(String transactionId);
}

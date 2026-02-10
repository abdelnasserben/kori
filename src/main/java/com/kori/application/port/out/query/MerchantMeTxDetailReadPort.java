package com.kori.application.port.out.query;

import com.kori.application.query.model.MeQueryModels;

import java.util.Optional;

public interface MerchantMeTxDetailReadPort {
    Optional<MeQueryModels.MerchantTransactionDetails> findOwnedByMerchant(String merchantId, String transactionId);

    boolean existsTransaction(String transactionId);
}

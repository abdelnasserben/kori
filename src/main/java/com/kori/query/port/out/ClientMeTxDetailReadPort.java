package com.kori.query.port.out;

import com.kori.query.model.me.MeQueryModels;

import java.util.Optional;

public interface ClientMeTxDetailReadPort {
    Optional<MeQueryModels.ClientTransactionDetails> findOwnedByClient(String clientId, String transactionId);

    boolean existsTransaction(String transactionId);
}

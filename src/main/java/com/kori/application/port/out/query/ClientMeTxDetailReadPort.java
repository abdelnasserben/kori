package com.kori.application.port.out.query;

import com.kori.application.query.model.MeQueryModels;

import java.util.Optional;

public interface ClientMeTxDetailReadPort {
    Optional<MeQueryModels.ClientTransactionDetails> findOwnedByClient(String clientId, String transactionId);

    boolean existsTransaction(String transactionId);
}

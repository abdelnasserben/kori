package com.kori.adapters.out.jpa.query;

import java.util.Optional;

public interface BackofficeTransactionReadPort {
    QueryPage<BackofficeTransactionItem> list(BackofficeTransactionQuery query);
    Optional<BackofficeTransactionDetails> findById(String transactionId);
}

package com.kori.query.port.out;

import com.kori.query.model.BackofficeTransactionDetails;
import com.kori.query.model.BackofficeTransactionItem;
import com.kori.query.model.BackofficeTransactionQuery;
import com.kori.query.model.QueryPage;

import java.util.Optional;

public interface BackofficeTransactionReadPort {
    QueryPage<BackofficeTransactionItem> list(BackofficeTransactionQuery query);
    Optional<BackofficeTransactionDetails> findById(String transactionId);
}

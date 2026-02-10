package com.kori.application.port.out.query;

import com.kori.application.query.BackofficeTransactionDetails;
import com.kori.application.query.BackofficeTransactionItem;
import com.kori.application.query.BackofficeTransactionQuery;
import com.kori.application.query.QueryPage;

import java.util.Optional;

public interface BackofficeTransactionReadPort {
    QueryPage<BackofficeTransactionItem> list(BackofficeTransactionQuery query);
    Optional<BackofficeTransactionDetails> findById(String transactionId);
}

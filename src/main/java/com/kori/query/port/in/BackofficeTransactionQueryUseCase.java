package com.kori.query.port.in;

import com.kori.query.model.BackofficeTransactionDetails;
import com.kori.query.model.BackofficeTransactionItem;
import com.kori.query.model.BackofficeTransactionQuery;
import com.kori.query.model.QueryPage;

public interface BackofficeTransactionQueryUseCase {
    QueryPage<BackofficeTransactionItem> list(BackofficeTransactionQuery query);
    BackofficeTransactionDetails getById(String transactionId);
}

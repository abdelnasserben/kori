package com.kori.application.port.in.query;

import com.kori.application.query.BackofficeTransactionDetails;
import com.kori.application.query.BackofficeTransactionItem;
import com.kori.application.query.BackofficeTransactionQuery;
import com.kori.application.query.QueryPage;

public interface BackofficeTransactionQueryUseCase {
    QueryPage<BackofficeTransactionItem> list(BackofficeTransactionQuery query);
    BackofficeTransactionDetails getById(String transactionId);
}

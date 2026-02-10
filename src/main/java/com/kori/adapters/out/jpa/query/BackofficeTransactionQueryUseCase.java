package com.kori.adapters.out.jpa.query;

public interface BackofficeTransactionQueryUseCase {
    QueryPage<BackofficeTransactionItem> list(BackofficeTransactionQuery query);
    BackofficeTransactionDetails getById(String transactionId);
}

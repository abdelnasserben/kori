package com.kori.application.port.out;

import com.kori.domain.model.transaction.Transaction;
import com.kori.domain.model.transaction.TransactionId;

import java.util.Optional;

public interface TransactionRepositoryPort {
    Transaction save(Transaction transaction);

    Optional<Transaction> findById(TransactionId transactionId);

    boolean existsReversalFor(TransactionId originalTransactionId);

}

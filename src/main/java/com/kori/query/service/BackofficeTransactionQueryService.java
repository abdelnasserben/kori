package com.kori.query.service;

import com.kori.application.exception.NotFoundException;
import com.kori.query.model.BackofficeTransactionDetails;
import com.kori.query.model.BackofficeTransactionItem;
import com.kori.query.model.BackofficeTransactionQuery;
import com.kori.query.model.QueryPage;
import com.kori.query.port.in.BackofficeTransactionQueryUseCase;
import com.kori.query.port.out.BackofficeTransactionReadPort;

import java.util.Objects;

public class BackofficeTransactionQueryService implements BackofficeTransactionQueryUseCase {

    private final BackofficeTransactionReadPort readPort;

    public BackofficeTransactionQueryService(BackofficeTransactionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public QueryPage<BackofficeTransactionItem> list(BackofficeTransactionQuery query) {
        return readPort.list(query);
    }

    @Override
    public BackofficeTransactionDetails getByRef(String transactionRef) {
        return readPort.findByRef(transactionRef)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }
}

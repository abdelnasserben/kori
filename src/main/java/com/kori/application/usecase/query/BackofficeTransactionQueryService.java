package com.kori.application.usecase.query;

import com.kori.application.exception.NotFoundException;
import com.kori.application.port.in.query.BackofficeTransactionQueryUseCase;
import com.kori.application.port.out.query.BackofficeTransactionReadPort;
import com.kori.application.query.BackofficeTransactionDetails;
import com.kori.application.query.BackofficeTransactionItem;
import com.kori.application.query.BackofficeTransactionQuery;
import com.kori.application.query.QueryPage;

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
    public BackofficeTransactionDetails getById(String transactionId) {
        return readPort.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
    }
}

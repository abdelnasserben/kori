package com.kori.adapters.out.jpa.query;

import com.kori.application.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
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

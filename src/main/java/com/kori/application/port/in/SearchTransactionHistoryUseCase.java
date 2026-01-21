package com.kori.application.port.in;

import com.kori.application.command.SearchTransactionHistoryCommand;
import com.kori.application.result.TransactionHistoryResult;

public interface SearchTransactionHistoryUseCase {
    TransactionHistoryResult execute(SearchTransactionHistoryCommand command);
}

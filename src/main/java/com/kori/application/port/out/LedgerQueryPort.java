package com.kori.application.port.out;

import com.kori.domain.ledger.LedgerEntry;
import com.kori.domain.model.common.Money;

import java.util.List;

public interface LedgerQueryPort {
    Money agentAvailableBalance(String agentId);

    // Needed for generic reversal
    List<LedgerEntry> findByTransactionId(String transactionId);
}

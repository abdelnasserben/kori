package com.kori.application.port.out;

import com.kori.domain.ledger.LedgerEntry;

import java.util.List;

public interface LedgerAppendPort {
    void append(List<LedgerEntry> entries);
}

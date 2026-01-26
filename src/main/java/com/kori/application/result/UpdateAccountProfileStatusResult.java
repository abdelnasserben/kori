package com.kori.application.result;

import com.kori.domain.ledger.LedgerAccountRef;
import com.kori.domain.model.common.Status;

import java.util.UUID;

public record UpdateAccountProfileStatusResult(
        LedgerAccountRef accountId,
        Status previousStatus,
        Status newStatus
) {}

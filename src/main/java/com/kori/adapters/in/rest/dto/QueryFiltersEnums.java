package com.kori.adapters.in.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public final class QueryFiltersEnums {

    private QueryFiltersEnums() {
    }

    @Schema(description = "Backoffice/Me transaction status filter")
    public enum TransactionStatusFilter {
        COMPLETED,
        REQUESTED,
        FAILED
    }

    @Schema(description = "Backoffice actor type filter")
    public enum BackofficeActorTypeFilter {
        AGENT,
        CLIENT,
        MERCHANT,
        TERMINAL,
        ADMIN
    }

    @Schema(description = "Lookup type filter")
    public enum LookupType {
        CLIENT_CODE,
        CARD_UID,
        TERMINAL_UID,
        TRANSACTION_REF,
        MERCHANT_CODE,
        AGENT_CODE,
        ADMIN_USERNAME
    }
}

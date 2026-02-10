package com.kori.adapters.out.jpa.query;

public interface BackofficeAuditEventQueryUseCase {
    QueryPage<BackofficeAuditEventItem> list(BackofficeAuditEventQuery query);
}

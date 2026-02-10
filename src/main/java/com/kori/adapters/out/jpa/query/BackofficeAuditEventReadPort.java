package com.kori.adapters.out.jpa.query;

public interface BackofficeAuditEventReadPort {
    QueryPage<BackofficeAuditEventItem> list(BackofficeAuditEventQuery query);
}

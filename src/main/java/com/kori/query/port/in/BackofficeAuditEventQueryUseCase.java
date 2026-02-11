package com.kori.query.port.in;

import com.kori.query.model.BackofficeAuditEventItem;
import com.kori.query.model.BackofficeAuditEventQuery;
import com.kori.query.model.QueryPage;

public interface BackofficeAuditEventQueryUseCase {
    QueryPage<BackofficeAuditEventItem> list(BackofficeAuditEventQuery query);
}

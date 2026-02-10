package com.kori.application.port.in.query;

import com.kori.application.query.BackofficeAuditEventItem;
import com.kori.application.query.BackofficeAuditEventQuery;
import com.kori.application.query.QueryPage;

public interface BackofficeAuditEventQueryUseCase {
    QueryPage<BackofficeAuditEventItem> list(BackofficeAuditEventQuery query);
}

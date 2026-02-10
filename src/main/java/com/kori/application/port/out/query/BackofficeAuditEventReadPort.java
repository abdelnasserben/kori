package com.kori.application.port.out.query;

import com.kori.application.query.BackofficeAuditEventItem;
import com.kori.application.query.BackofficeAuditEventQuery;
import com.kori.application.query.QueryPage;

public interface BackofficeAuditEventReadPort {
    QueryPage<BackofficeAuditEventItem> list(BackofficeAuditEventQuery query);
}

package com.kori.application.usecase.query;

import com.kori.application.port.in.query.BackofficeAuditEventQueryUseCase;
import com.kori.application.port.out.query.BackofficeAuditEventReadPort;
import com.kori.application.query.BackofficeAuditEventItem;
import com.kori.application.query.BackofficeAuditEventQuery;
import com.kori.application.query.QueryPage;

import java.util.Objects;

public class BackofficeAuditEventQueryService implements BackofficeAuditEventQueryUseCase {

    private final BackofficeAuditEventReadPort readPort;

    public BackofficeAuditEventQueryService(BackofficeAuditEventReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort);
    }

    @Override
    public QueryPage<BackofficeAuditEventItem> list(BackofficeAuditEventQuery query) {
        return readPort.list(query);
    }
}

package com.kori.query.service;

import com.kori.query.model.BackofficeAuditEventItem;
import com.kori.query.model.BackofficeAuditEventQuery;
import com.kori.query.model.QueryPage;
import com.kori.query.port.in.BackofficeAuditEventQueryUseCase;
import com.kori.query.port.out.BackofficeAuditEventReadPort;

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

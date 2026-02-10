package com.kori.adapters.out.jpa.query;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
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

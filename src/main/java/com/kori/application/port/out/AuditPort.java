package com.kori.application.port.out;

import com.kori.domain.model.audit.AuditEvent;

public interface AuditPort {
    void publish(AuditEvent event);
}

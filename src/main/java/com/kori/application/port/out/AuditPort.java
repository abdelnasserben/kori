package com.kori.application.port.out;

public interface AuditPort {
    void publish(AuditEvent event);
}

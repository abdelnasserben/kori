package com.kori.application.port.out;

import java.time.Instant;

public interface DataRetentionPort {
    long purgeExpiredIdempotencyRecords(Instant now);

    long purgeAuditEventsBefore(Instant cutoff);
}

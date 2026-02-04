package com.kori.adapters.in.scheduler;

import com.kori.application.port.out.DataRetentionPort;
import com.kori.application.port.out.TimeProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
public class DataRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionScheduler.class);

    private final DataRetentionPort dataRetentionPort;
    private final TimeProviderPort timeProviderPort;
    private final Duration auditRetention;

    public DataRetentionScheduler(
            DataRetentionPort dataRetentionPort,
            TimeProviderPort timeProviderPort,
            @Value("${kori.retention.audit-events:P90D}") Duration auditRetention
    ) {
        this.dataRetentionPort = Objects.requireNonNull(dataRetentionPort, "dataRetentionPort");
        this.timeProviderPort = Objects.requireNonNull(timeProviderPort, "timeProviderPort");
        this.auditRetention = Objects.requireNonNull(auditRetention, "auditRetention");
    }

    @Scheduled(
            fixedDelayString = "${kori.retention.purge.fixed-delay-ms:3600000}",
            initialDelayString = "${kori.retention.purge.initial-delay-ms:60000}"
    )
    public void purge() {
        Instant now = timeProviderPort.now();
        long idempotencyDeleted = dataRetentionPort.purgeExpiredIdempotencyRecords(now);
        Instant auditCutoff = now.minus(auditRetention);
        long auditDeleted = dataRetentionPort.purgeAuditEventsBefore(auditCutoff);

        log.info(
                "Data retention purge: deleted {} expired idempotency record(s), {} audit event(s) older than {}.",
                idempotencyDeleted,
                auditDeleted,
                auditCutoff
        );
    }
}

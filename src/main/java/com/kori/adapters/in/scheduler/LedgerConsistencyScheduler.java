package com.kori.adapters.in.scheduler;

import com.kori.application.port.out.AuditPort;
import com.kori.application.port.out.LedgerQueryPort;
import com.kori.application.port.out.TimeProviderPort;
import com.kori.domain.model.audit.AuditEvent;
import com.kori.domain.model.transaction.TransactionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class LedgerConsistencyScheduler {

    private static final Logger log = LoggerFactory.getLogger(LedgerConsistencyScheduler.class);
    private static final int SAMPLE_SIZE = 10;

    private final LedgerQueryPort ledgerQueryPort;
    private final AuditPort auditPort;
    private final TimeProviderPort timeProviderPort;

    public LedgerConsistencyScheduler(
            LedgerQueryPort ledgerQueryPort,
            AuditPort auditPort,
            TimeProviderPort timeProviderPort
    ) {
        this.ledgerQueryPort = Objects.requireNonNull(ledgerQueryPort, "ledgerQueryPort");
        this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
        this.timeProviderPort = Objects.requireNonNull(timeProviderPort, "timeProviderPort");
    }

    @Scheduled(
            fixedDelayString = "${kori.ledger.consistency-check.fixed-delay-ms:900000}",
            initialDelayString = "${kori.ledger.consistency-check.initial-delay-ms:60000}"
    )
    public void run() {
        List<TransactionId> inconsistent = ledgerQueryPort.findInconsistentTransactionIds();
        if (inconsistent.isEmpty()) {
            log.info("Ledger consistency check: no anomalies detected.");
            return;
        }

        List<String> sample = inconsistent.stream()
                .limit(SAMPLE_SIZE)
                .map(id -> id.value().toString())
                .toList();

        log.warn(
                "Ledger consistency check: {} transaction(s) with anomalies. Sample transaction ids: {}",
                inconsistent.size(),
                sample
        );

        auditPort.publish(new AuditEvent(
                "LEDGER_CONSISTENCY_ALERT",
                "SYSTEM",
                "LEDGER",
                timeProviderPort.now(),
                Map.of(
                        "count", Integer.toString(inconsistent.size()),
                        "sampleTransactionIds", String.join(",", sample)
                )
        ));
    }
}

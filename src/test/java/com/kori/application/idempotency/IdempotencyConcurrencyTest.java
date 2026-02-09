package com.kori.application.idempotency;

import com.kori.application.exception.IdempotencyConflictException;
import com.kori.application.port.out.IdempotencyPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IdempotencyConcurrencyTest {

    @Test
    void concurrent_requests_do_not_double_write() throws Exception {
        InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
        AtomicInteger ledgerWrites = new AtomicInteger();
        DemoService service = new DemoService(store, ledgerWrites);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<String> task = () -> {
            ready.countDown();
            start.await(2, TimeUnit.SECONDS);
            return service.execute("idem-1", "hash-1");
        };

        Future<String> first = executor.submit(task);
        Future<String> second = executor.submit(task);

        ready.await(2, TimeUnit.SECONDS);
        start.countDown();

        List<String> successes = new ArrayList<>();
        int conflicts = 0;

        for (Future<String> future : List.of(first, second)) {
            try {
                successes.add(future.get(2, TimeUnit.SECONDS));
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IdempotencyConflictException) {
                    conflicts++;
                } else {
                    throw e;
                }
            }
        }

        executor.shutdownNow();

        assertEquals(1, ledgerWrites.get(), "Ledger write must run once.");
        assertEquals(2, successes.size() + conflicts);
        assertTrue(conflicts <= 1);
        if (successes.size() == 2) {
            assertEquals(successes.get(0), successes.get(1));
        }
    }

    private static final class DemoService {
        private final IdempotencyPort idempotencyPort;
        private final AtomicInteger ledgerWrites;

        private DemoService(IdempotencyPort idempotencyPort, AtomicInteger ledgerWrites) {
            this.idempotencyPort = idempotencyPort;
            this.ledgerWrites = ledgerWrites;
        }

        String execute(String idempotencyKey, String requestHash) {
            IdempotencyClaim<String> claim = idempotencyPort.claimOrLoad(idempotencyKey, requestHash, String.class);

            if (claim.status() == IdempotencyClaimStatus.ALREADY_COMPLETED) {
                return claim.result().orElseThrow();
            }
            if (claim.status() == IdempotencyClaimStatus.IN_PROGRESS) {
                throw new IdempotencyConflictException("Idempotency key is already being processed.",
                        Map.of("idempotencyKey", idempotencyKey));
            }
            if (claim.status() == IdempotencyClaimStatus.CONFLICT) {
                throw new IdempotencyConflictException("Idempotency key reuse with different payload.",
                        Map.of("idempotencyKey", idempotencyKey));
            }

            try {
                ledgerWrites.incrementAndGet();
                String result = "ledger-result";
                idempotencyPort.complete(idempotencyKey, requestHash, result);
                return result;
            } catch (RuntimeException e) {
                idempotencyPort.fail(idempotencyKey, requestHash);
                throw e;
            }
        }
    }

    private static final class InMemoryIdempotencyStore implements IdempotencyPort {
        private final ConcurrentHashMap<String, Record> records = new ConcurrentHashMap<>();

        private record Record(String requestHash, String resultType, Object result, IdempotencyStatus status) { }

        @Override
        public <T> IdempotencyClaim<T> claimOrLoad(String idempotencyKey, String requestHash, Class<T> type) {
            AtomicReference<IdempotencyClaim<T>> response = new AtomicReference<>();
            records.compute(idempotencyKey, (key, existing) -> {
                if (existing == null) {
                    response.set(IdempotencyClaim.claimed());
                    return new Record(requestHash, type.getName(), null, IdempotencyStatus.IN_PROGRESS);
                }
                if (!existing.requestHash().equals(requestHash)) {
                    response.set(IdempotencyClaim.conflict());
                    return existing;
                }
                if (!existing.resultType().equals(type.getName())) {
                    throw new IllegalStateException("Idempotency type mismatch for key=" + idempotencyKey);
                }
                if (existing.status() == IdempotencyStatus.COMPLETED) {
                    response.set(IdempotencyClaim.completed(type.cast(existing.result())));
                } else if (existing.status() == IdempotencyStatus.IN_PROGRESS) {
                    response.set(IdempotencyClaim.inProgress());
                } else {
                    response.set(IdempotencyClaim.conflict());
                }
                return existing;
            });
            return response.get();
        }

        @Override
        public void complete(String idempotencyKey, String requestHash, Object result) {
            records.computeIfPresent(idempotencyKey, (key, existing) -> {
                if (!existing.requestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException("Idempotency key reuse with different payload.",
                            Map.of("idempotencyKey", idempotencyKey));
                }
                return new Record(requestHash, existing.resultType(), result, IdempotencyStatus.COMPLETED);
            });
        }

        @Override
        public void fail(String idempotencyKey, String requestHash) {
            records.computeIfPresent(idempotencyKey, (key, existing) -> {
                if (!existing.requestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException("Idempotency key reuse with different payload.",
                            Map.of("idempotencyKey", idempotencyKey));
                }
                return new Record(requestHash, existing.resultType(), existing.result(), IdempotencyStatus.FAILED);
            });
        }
    }
}

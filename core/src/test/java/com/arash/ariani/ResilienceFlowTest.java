package com.arash.ariani;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ResilienceFlowTest extends BaseFlowTest {

    @Test
    void testRetryWithSuccess() {
        String result = Flow.of(failNTimes(2, "Success"))
            .withRetry(3)
            .execute();

        assertEquals("Success", result);
        assertEquals(3, counter.get());
    }

    @Test
    void testRetryWithFailure() {
        assertThrows(FlowExecutionException.class, () ->
            Flow.of(failNTimes(4, "Success"))
                .withRetry(3)
                .execute()
        );
        assertEquals(4, counter.get());
    }

    @Test
    void testRetryWithBackoff() {
        long startTime = System.currentTimeMillis();
        
        String result = Flow.of(failNTimes(2, "Success"))
            .withRetry(3)
            .withBackoff(Duration.ofMillis(100))
            .execute();

        long duration = System.currentTimeMillis() - startTime;
        assertEquals("Success", result);
        assertTrue(duration >= 300); // First attempt + 100ms + 200ms backoff
    }

    @Test
    void testTimeoutSuccess() {
        String result = Flow.of(delayedSupplier(50, "Quick"))
            .withTimeout(Duration.ofMillis(100))
            .execute();

        assertEquals("Quick", result);
    }

    @Test
    void testTimeoutFailure() {
        assertThrows(FlowTimeoutException.class, () ->
            Flow.of(delayedSupplier(200, "Slow"))
                .withTimeout(Duration.ofMillis(100))
                .execute()
        );
    }

    @Test
    void testCircuitBreakerTrip() {
        Flow<String> flow = Flow.of(() -> {
            throw new RuntimeException("Service failure");
        });

        // Execute 5 times to trip the circuit breaker
        for (int i = 0; i < 5; i++) {
            assertThrows(FlowExecutionException.class, flow::execute);
        }

        // Next execution should fail fast with circuit breaker open
        long startTime = System.currentTimeMillis();
        assertThrows(FlowExecutionException.class, flow::execute);
        assertTrue(System.currentTimeMillis() - startTime < 50); // Should fail fast
    }

    @Test
    void testErrorHandling() {
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        assertThrows(RuntimeException.class, () ->
            Flow.of(() -> { throw new RuntimeException("Test error"); })
                .onError(e -> errorCount.incrementAndGet())
                .onComplete(r -> successCount.incrementAndGet())
                .execute()
        );

        assertEquals(1, errorCount.get());
        assertEquals(0, successCount.get());
    }

    @Test
    void testCompensationActions() {
        AtomicInteger compensationCalled = new AtomicInteger(0);
        
        assertThrows(RuntimeException.class, () ->
            Flow.of(() -> {
                throw new RuntimeException("Failure requiring compensation");
            })
            .withCompensation(data -> compensationCalled.incrementAndGet())
            .execute()
        );

        assertEquals(1, compensationCalled.get());
    }

    @Test
    void testCombinedResilience() {
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicInteger compensations = new AtomicInteger(0);

        String result = Flow.of(() -> {
                if (attempts.incrementAndGet() <= 2) {
                    throw new RuntimeException("Temporary failure");
                }
                return "Success";
            })
            .withRetry(3)
            .withBackoff(Duration.ofMillis(50))
            .withTimeout(Duration.ofSeconds(1))
            .withCompensation(data -> compensations.incrementAndGet())
            .onError(e -> {})
            .execute();

        assertEquals("Success", result);
        assertEquals(3, attempts.get());
        assertEquals(2, compensations.get()); // Compensation for first two failures
    }

    @Test
    void testCircuitBreakerWithConfig() {
        Flow<String> flow = Flow.<String>of(() -> {
            throw new RuntimeException("Service failure");
        })
        .withCircuitBreaker(CircuitBreakerConfig.builder()
            .failureThreshold(3)  // Lower threshold for testing
            .resetTimeout(Duration.ofMillis(100))  // Shorter timeout for testing
            .build())
        .withFallback(() -> "fallback-value");

        // First 3 calls should attempt execution and fail
        for (int i = 0; i < 3; i++) {
            String result = flow.execute();
            assertEquals("fallback-value", result);
        }

        // Next call should immediately return fallback due to open circuit
        long startTime = System.currentTimeMillis();
        String result = flow.execute();
        assertTrue(System.currentTimeMillis() - startTime < 50); // Should fail fast
        assertEquals("fallback-value", result);

        // Wait for reset timeout
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Circuit should be half-open now, allowing one attempt
        result = flow.execute();
        assertEquals("fallback-value", result);
    }
} 
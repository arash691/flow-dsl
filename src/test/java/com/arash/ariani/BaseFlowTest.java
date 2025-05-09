package com.arash.ariani;

import org.junit.jupiter.api.BeforeEach;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Base test class providing common utilities for Flow DSL tests.
 */
public class BaseFlowTest {
    
    protected AtomicInteger counter;

    @BeforeEach
    void setUp() {
        counter = new AtomicInteger(0);
    }

    /**
     * Creates a supplier that fails N times before succeeding
     *
     * @param failCount Number of times to fail
     * @param result    The successful result
     * @return A supplier that implements the failure pattern
     */
    protected <T> Supplier<T> failNTimes(int failCount, T result) {
        return () -> {
            int attempts = counter.incrementAndGet();
            if (attempts <= failCount) {
                throw new RuntimeException("Attempt " + attempts + " failed");
            }
            return result;
        };
    }

    /**
     * Creates a supplier that delays for a specified time before returning
     *
     * @param delayMillis Delay duration in milliseconds
     * @param result      The result to return
     * @return A supplier that implements the delay
     */
    protected <T> Supplier<T> delayedSupplier(int delayMillis, T result) {
        return () -> {
            try {
                Thread.sleep(delayMillis);
                return result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Delayed supplier interrupted", e);
            }
        };
    }
} 
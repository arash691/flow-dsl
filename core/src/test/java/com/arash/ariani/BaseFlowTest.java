package com.arash.ariani;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Base test class with common test utilities for Flow tests.
 */
public abstract class BaseFlowTest {
    protected final AtomicInteger counter = new AtomicInteger(0);

    protected <T> Supplier<T> failNTimes(int n, T successValue) {
        return () -> {
            int attempts = counter.incrementAndGet();
            if (attempts <= n) {
                throw new RuntimeException("Attempt " + attempts + " failed");
            }
            return successValue;
        };
    }

    protected <T> Supplier<T> delayedSupplier(long delayMillis, T value) {
        return () -> {
            try {
                Thread.sleep(delayMillis);
                return value;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during delay", e);
            }
        };
    }
} 
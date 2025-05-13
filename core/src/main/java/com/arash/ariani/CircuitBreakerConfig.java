package com.arash.ariani;

import java.time.Duration;

/**
 * Configuration class for circuit breaker settings.
 */
public class CircuitBreakerConfig {
    private final int failureThreshold;
    private final Duration resetTimeout;

    private CircuitBreakerConfig(Builder builder) {
        this.failureThreshold = builder.failureThreshold;
        this.resetTimeout = builder.resetTimeout;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public Duration getResetTimeout() {
        return resetTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int failureThreshold = 5; // default value
        private Duration resetTimeout = Duration.ofSeconds(60); // default value

        public Builder failureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
            return this;
        }

        public Builder resetTimeout(Duration resetTimeout) {
            this.resetTimeout = resetTimeout;
            return this;
        }

        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(this);
        }
    }
} 
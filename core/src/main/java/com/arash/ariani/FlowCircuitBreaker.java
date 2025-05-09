package com.arash.ariani;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker implementation for Flow DSL.
 */
public class FlowCircuitBreaker {
    private final int failureThreshold;
    private final Duration resetTimeout;
    private final AtomicInteger failureCount;
    private final AtomicReference<Instant> lastFailure;
    private final AtomicReference<State> state;

    public enum State {
        CLOSED,     // Normal operation
        OPEN,       // Circuit breaker tripped
        HALF_OPEN   // Testing if service is back
    }

    public FlowCircuitBreaker(int failureThreshold, Duration resetTimeout) {
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
        this.failureCount = new AtomicInteger(0);
        this.lastFailure = new AtomicReference<>(Instant.now());
        this.state = new AtomicReference<>(State.CLOSED);
    }

    /**
     * Checks if the circuit breaker allows execution
     *
     * @return true if execution is allowed
     */
    public boolean allowExecution() {
        State currentState = state.get();
        if (currentState == State.CLOSED) {
            return true;
        }
        if (currentState == State.OPEN) {
            if (Duration.between(lastFailure.get(), Instant.now()).compareTo(resetTimeout) > 0) {
                state.compareAndSet(State.OPEN, State.HALF_OPEN);
                return true;
            }
            return false;
        }
        return true; // HALF_OPEN state allows one execution
    }

    /**
     * Records a successful execution
     */
    public void recordSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    /**
     * Records a failed execution
     */
    public void recordFailure() {
        lastFailure.set(Instant.now());
        if (state.get() == State.HALF_OPEN || 
            failureCount.incrementAndGet() >= failureThreshold) {
            state.set(State.OPEN);
        }
    }

    /**
     * Gets the current state of the circuit breaker
     *
     * @return the current state
     */
    public State getState() {
        return state.get();
    }

    /**
     * Gets the current failure count
     *
     * @return the number of consecutive failures
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Gets the time since the last failure
     *
     * @return the duration since the last failure
     */
    public Duration getTimeSinceLastFailure() {
        return Duration.between(lastFailure.get(), Instant.now());
    }
} 
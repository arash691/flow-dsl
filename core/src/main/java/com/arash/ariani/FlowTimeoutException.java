package com.arash.ariani;

/**
 * Exception thrown when a flow execution times out.
 */
public class FlowTimeoutException extends FlowExecutionException {
    public FlowTimeoutException(String message) {
        super(message);
    }

    public FlowTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
} 
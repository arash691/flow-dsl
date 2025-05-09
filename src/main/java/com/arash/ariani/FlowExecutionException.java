package com.arash.ariani;

/**
 * Exception thrown when a flow execution fails.
 */
public class FlowExecutionException extends RuntimeException {
    public FlowExecutionException(String message) {
        super(message);
    }

    public FlowExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
} 
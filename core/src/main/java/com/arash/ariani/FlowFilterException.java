package com.arash.ariani;

/**
 * Exception thrown when a value is filtered out in a flow.
 */
public class FlowFilterException extends FlowExecutionException {
    public FlowFilterException(String message) {
        super(message);
    }

    public FlowFilterException(String message, Throwable cause) {
        super(message, cause);
    }
} 
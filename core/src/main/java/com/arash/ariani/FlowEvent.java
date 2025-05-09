package com.arash.ariani;

import java.time.Instant;

/**
 * Represents events that occur during flow execution.
 */
public class FlowEvent {
    private final String flowId;
    private final EventType type;
    private final Instant timestamp;
    private final Object payload;

    public enum EventType {
        FLOW_STARTED,
        FLOW_COMPLETED,
        FLOW_ERROR,
        STEP_STARTED,
        STEP_COMPLETED,
        STEP_ERROR,
        RETRY_ATTEMPT,
        TIMEOUT_OCCURRED,
        CIRCUIT_BREAKER_STATE_CHANGED
    }

    public FlowEvent(String flowId, EventType type, Object payload) {
        this.flowId = flowId;
        this.type = type;
        this.timestamp = Instant.now();
        this.payload = payload;
    }

    public String getFlowId() {
        return flowId;
    }

    public EventType getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Object getPayload() {
        return payload;
    }
} 
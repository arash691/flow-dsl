package com.arash.ariani;

import java.util.function.Consumer;

/**
 * Interface for emitting and subscribing to flow events.
 */
public interface FlowEventEmitter {
    /**
     * Subscribes to all flow events
     *
     * @param listener The event listener
     * @return A subscription ID that can be used to unsubscribe
     */
    String subscribe(Consumer<FlowEvent> listener);

    /**
     * Subscribes to specific event types
     *
     * @param listener The event listener
     * @param types    The event types to subscribe to
     * @return A subscription ID that can be used to unsubscribe
     */
    String subscribeToTypes(Consumer<FlowEvent> listener, FlowEvent.EventType... types);

    /**
     * Unsubscribes a listener
     *
     * @param subscriptionId The subscription ID to unsubscribe
     */
    void unsubscribe(String subscriptionId);

    /**
     * Emits an event to all subscribers
     *
     * @param event The event to emit
     */
    void emit(FlowEvent event);
} 
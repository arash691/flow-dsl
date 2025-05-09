package com.arash.ariani;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Default implementation of FlowEventEmitter that supports concurrent event handling.
 */
public class DefaultFlowEventEmitter implements FlowEventEmitter {
    private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

    private static class Subscription {
        final Consumer<FlowEvent> listener;
        final Set<FlowEvent.EventType> eventTypes;

        Subscription(Consumer<FlowEvent> listener, Set<FlowEvent.EventType> eventTypes) {
            this.listener = listener;
            this.eventTypes = eventTypes;
        }

        boolean isInterestedIn(FlowEvent.EventType type) {
            return eventTypes == null || eventTypes.isEmpty() || eventTypes.contains(type);
        }
    }

    @Override
    public String subscribe(Consumer<FlowEvent> listener) {
        return subscribeToTypes(listener);
    }

    @Override
    public String subscribeToTypes(Consumer<FlowEvent> listener, FlowEvent.EventType... types) {
        String subscriptionId = UUID.randomUUID().toString();
        Set<FlowEvent.EventType> eventTypes = types.length > 0 
            ? EnumSet.copyOf(Arrays.asList(types))
            : Collections.emptySet();
        subscriptions.put(subscriptionId, new Subscription(listener, eventTypes));
        return subscriptionId;
    }

    @Override
    public void unsubscribe(String subscriptionId) {
        subscriptions.remove(subscriptionId);
    }

    @Override
    public void emit(FlowEvent event) {
        subscriptions.values().stream()
            .filter(sub -> sub.isInterestedIn(event.getType()))
            .forEach(sub -> {
                try {
                    sub.listener.accept(event);
                } catch (Exception e) {
                    // Log error but continue processing other subscribers
                }
            });
    }
} 
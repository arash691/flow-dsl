package com.arash.ariani;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.util.stream.Collectors;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class FlowEventTest extends BaseFlowTest {

    private List<FlowEvent.EventType> getEventTypes(List<FlowEvent> events) {
        return events.stream()
            .map(FlowEvent::getType)
            .collect(Collectors.toList());
    }

    @Test
    void testBasicEventEmission() {
        List<FlowEvent> events = new ArrayList<>();
        
        Flow.of(() -> "test")
            .onEvent(events::add)
            .execute();

        var eventTypes = getEventTypes(events);
        assertTrue(eventTypes.contains(FlowEvent.EventType.FLOW_STARTED));
        assertTrue(eventTypes.contains(FlowEvent.EventType.FLOW_COMPLETED));
    }

    @Test
    void testEventTypesFiltering() {
        List<FlowEvent> errorEvents = new ArrayList<>();
        
        assertThrows(RuntimeException.class, () ->
            Flow.of(() -> { throw new RuntimeException("Test error"); })
                .onEventTypes(errorEvents::add, FlowEvent.EventType.FLOW_ERROR)
                .execute()
        );

        assertEquals(1, errorEvents.size());
        assertEquals(FlowEvent.EventType.FLOW_ERROR, errorEvents.get(0).getType());
        assertTrue(errorEvents.get(0).getPayload() instanceof RuntimeException);
    }

    @Test
    void testRetryEvents() {
        List<FlowEvent> retryEvents = new ArrayList<>();
        
        Flow.of(failNTimes(2, "success"))
            .withRetry(3)
            .onEventTypes(retryEvents::add, FlowEvent.EventType.RETRY_ATTEMPT)
            .execute();

        assertEquals(2, retryEvents.size()); // 2 retries after initial failure
        retryEvents.forEach(event -> 
            assertEquals(FlowEvent.EventType.RETRY_ATTEMPT, event.getType())
        );
    }

    @Test
    void testTimeoutEvents() {
        List<FlowEvent> timeoutEvents = new ArrayList<>();
        
        assertThrows(FlowTimeoutException.class, () ->
            Flow.of(delayedSupplier(200, "test"))
                .withTimeout(Duration.ofMillis(100))
                .onEventTypes(timeoutEvents::add, FlowEvent.EventType.TIMEOUT_OCCURRED)
                .execute()
        );

        assertEquals(1, timeoutEvents.size());
        assertEquals(FlowEvent.EventType.TIMEOUT_OCCURRED, timeoutEvents.get(0).getType());
    }

    @Test
    void testAsyncEventEmission() throws Exception {
        List<FlowEvent> events = new ArrayList<>();
        
        CompletableFuture<String> future = Flow.of(() -> "async test")
            .onEvent(events::add)
            .executeAsync();

        future.get(1, TimeUnit.SECONDS);

        var eventTypes = getEventTypes(events);
        assertTrue(eventTypes.contains(FlowEvent.EventType.FLOW_STARTED));
        assertTrue(eventTypes.contains(FlowEvent.EventType.FLOW_COMPLETED));
    }

    @Test
    void testEventPayloads() {
        List<FlowEvent> events = new ArrayList<>();
        String testData = "test data";
        
        Flow.of(() -> testData)
            .onEvent(events::add)
            .execute();

        FlowEvent completedEvent = events.stream()
            .filter(e -> e.getType() == FlowEvent.EventType.FLOW_COMPLETED)
            .findFirst()
            .orElseThrow();

        assertEquals(testData, completedEvent.getPayload());
        assertNotNull(completedEvent.getTimestamp());
        assertNotNull(completedEvent.getFlowId());
    }

    @Test
    void testMultipleSubscribers() {
        List<FlowEvent> subscriber1Events = new ArrayList<>();
        List<FlowEvent> subscriber2Events = new ArrayList<>();
        
        Flow.of(() -> "test")
            .onEvent(subscriber1Events::add)
            .onEvent(subscriber2Events::add)
            .execute();

        assertEquals(subscriber1Events.size(), subscriber2Events.size());
        for (int i = 0; i < subscriber1Events.size(); i++) {
            assertEquals(subscriber1Events.get(i).getType(), subscriber2Events.get(i).getType());
        }
    }

    @Test
    void testStepEvents() {
        List<FlowEvent> events = new ArrayList<>();
        
        Flow.of(() -> "test")
            .map(s -> s.toUpperCase())
            .filter(s -> s.length() > 0)
            .onEvent(events::add)
            .execute();

        var eventTypes = getEventTypes(events);
        assertTrue(eventTypes.contains(FlowEvent.EventType.STEP_STARTED));
        assertTrue(eventTypes.contains(FlowEvent.EventType.STEP_COMPLETED));
        
        // Verify step order
        List<String> stepOrder = events.stream()
            .filter(e -> e.getType() == FlowEvent.EventType.STEP_STARTED)
            .map(e -> (String) e.getPayload())
            .collect(Collectors.toList());
            
        assertEquals("map", stepOrder.get(0));
        assertEquals("filter", stepOrder.get(1));
    }

    @Test
    void testComplexFlowEvents() {
        List<FlowEvent> events = new ArrayList<>();
        
        Flow.of(() -> 42)
            .map(n -> n * 2)
            .filter(n -> n > 50)
            .thenIf(
                n -> n > 80,
                n -> "High: " + n
            )
            .otherwise(n -> "Low: " + n)
            .onEvent(events::add)
            .execute();

        var eventTypes = getEventTypes(events);
        assertTrue(eventTypes.contains(FlowEvent.EventType.FLOW_STARTED));
        assertTrue(eventTypes.contains(FlowEvent.EventType.STEP_STARTED));
        assertTrue(eventTypes.contains(FlowEvent.EventType.STEP_COMPLETED));
        assertTrue(eventTypes.contains(FlowEvent.EventType.FLOW_COMPLETED));
        
        // Verify the sequence of steps
        List<String> stepOrder = events.stream()
            .filter(e -> e.getType() == FlowEvent.EventType.STEP_STARTED)
            .map(e -> (String) e.getPayload())
            .collect(Collectors.toList());
            
        assertEquals(Arrays.asList("map", "filter", "thenIf", "otherwise"), stepOrder);
    }
} 
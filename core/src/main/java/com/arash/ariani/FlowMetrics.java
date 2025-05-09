package com.arash.ariani;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics and telemetry collection for Flow DSL.
 */
public class FlowMetrics {
    private final String flowId;
    private final Instant startTime;
    private final Map<String, AtomicLong> counters;
    private final Map<String, Duration> timings;
    private final Map<String, String> tags;

    public FlowMetrics(String flowId) {
        this.flowId = flowId;
        this.startTime = Instant.now();
        this.counters = new ConcurrentHashMap<>();
        this.timings = new ConcurrentHashMap<>();
        this.tags = new ConcurrentHashMap<>();
    }

    /**
     * Increments a counter metric
     *
     * @param name The name of the counter
     */
    public void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Records a timing metric
     *
     * @param name     The name of the timing
     * @param duration The duration to record
     */
    public void recordTiming(String name, Duration duration) {
        timings.put(name, duration);
    }

    /**
     * Adds a tag to the metrics
     *
     * @param key   The tag key
     * @param value The tag value
     */
    public void addTag(String key, String value) {
        tags.put(key, value);
    }

    /**
     * Gets the flow execution duration
     *
     * @return The duration since flow start
     */
    public Duration getDuration() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * Gets the value of a counter
     *
     * @param name The counter name
     * @return The counter value
     */
    public long getCounter(String name) {
        return counters.getOrDefault(name, new AtomicLong(0)).get();
    }

    /**
     * Gets a timing value
     *
     * @param name The timing name
     * @return The timing duration
     */
    public Duration getTiming(String name) {
        return timings.get(name);
    }

    /**
     * Gets all metrics as a map
     *
     * @return A map of all metrics
     */
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new ConcurrentHashMap<>();
        metrics.put("flowId", flowId);
        metrics.put("duration", getDuration());
        metrics.put("counters", counters);
        metrics.put("timings", timings);
        metrics.put("tags", tags);
        return metrics;
    }

    /**
     * Gets the flow ID
     *
     * @return The flow ID
     */
    public String getFlowId() {
        return flowId;
    }

    /**
     * Gets the start time
     *
     * @return The start time
     */
    public Instant getStartTime() {
        return startTime;
    }
} 
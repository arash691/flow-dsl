package com.arash.ariani;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Context object for sharing data and metadata across flow steps.
 */
public class FlowContext {
    private final Map<String, Object> data;
    private final Map<String, Object> metadata;

    public FlowContext() {
        this.data = new HashMap<>();
        this.metadata = new HashMap<>();
    }

    /**
     * Stores a value in the context
     *
     * @param key   The key to store the value under
     * @param value The value to store
     * @return The same context instance
     */
    public FlowContext put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    /**
     * Retrieves a value from the context
     *
     * @param key  The key to retrieve
     * @param type The expected type of the value
     * @param <T>  The type parameter
     * @return An Optional containing the value if present
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        return Optional.ofNullable(data.get(key))
                .filter(type::isInstance)
                .map(type::cast);
    }

    /**
     * Adds metadata to the context
     *
     * @param key   The metadata key
     * @param value The metadata value
     * @return The same context instance
     */
    public FlowContext withMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * Retrieves metadata from the context
     *
     * @param key  The metadata key
     * @param type The expected type of the metadata value
     * @param <T>  The type parameter
     * @return An Optional containing the metadata value if present
     */
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        return Optional.ofNullable(metadata.get(key))
                .filter(type::isInstance)
                .map(type::cast);
    }

    /**
     * Checks if a key exists in the context
     *
     * @param key The key to check
     * @return true if the key exists
     */
    public boolean hasKey(String key) {
        return data.containsKey(key);
    }

    /**
     * Checks if a metadata key exists
     *
     * @param key The metadata key to check
     * @return true if the metadata key exists
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    /**
     * Clears all data from the context
     */
    public void clear() {
        data.clear();
        metadata.clear();
    }
} 
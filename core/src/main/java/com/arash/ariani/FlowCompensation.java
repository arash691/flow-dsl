package com.arash.ariani;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles compensation actions for flow steps in case of failures.
 *
 * @param <T> The type of data flowing through the pipeline
 */
public class FlowCompensation<T> {
    private final List<Consumer<T>> compensationActions;
    private boolean compensating;

    public FlowCompensation() {
        this.compensationActions = new ArrayList<>();
        this.compensating = false;
    }

    /**
     * Adds a compensation action to be executed in case of failure
     *
     * @param action The compensation action
     */
    public void addCompensation(Consumer<T> action) {
        compensationActions.add(action);
    }

    /**
     * Executes all compensation actions in reverse order
     *
     * @param data The data to pass to compensation actions
     */
    public void compensate(T data) {
        if (compensating) {
            return; // Prevent recursive compensation
        }

        compensating = true;
        try {
            for (int i = compensationActions.size() - 1; i >= 0; i--) {
                try {
                    compensationActions.get(i).accept(data);
                } catch (Exception e) {
                    // Log compensation action failure but continue with others
                    // We don't want compensation failures to prevent other compensations
                }
            }
        } finally {
            compensating = false;
        }
    }

    /**
     * Checks if compensation is currently in progress
     *
     * @return true if compensating
     */
    public boolean isCompensating() {
        return compensating;
    }

    /**
     * Gets the number of registered compensation actions
     *
     * @return the count of compensation actions
     */
    public int getCompensationCount() {
        return compensationActions.size();
    }
} 
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages the scheduler lifecycle for TUI runners.
 * <p>
 * Encapsulates scheduler resolution, periodic task execution (e.g. for ticks or resize checks),
 * cancellation on runner close, and owned-scheduler shutdown.
 */
final class RunnerScheduler implements AutoCloseable {

    private final ScheduledExecutorService scheduler;
    private final boolean schedulerOwned;
    private ScheduledFuture<?> task;

    /**
     * Creates a new runner scheduler from an externally-provided scheduler or creates an internal one.
     *
     * @param external the externally-provided scheduler, or null to create a new internal one
     */
    RunnerScheduler(ScheduledExecutorService external) {
        Schedulers.Scheduler resolved = Schedulers.resolve(external);
        this.scheduler = resolved.scheduler();
        this.schedulerOwned = resolved.owned();
    }

    /**
     * Starts a periodic task on the scheduler if a period is specified.
     *
     * @param period   the execution period, or null if periodic execution is not needed
     * @param callback the action to run periodically
     */
    void start(Duration period, Runnable callback) {
        if (period != null) {
            Objects.requireNonNull(callback, "callback cannot be null");
            if (this.task != null) {
                this.task.cancel(false);
            }
            long periodMs = period.toMillis();
            this.task = scheduler.scheduleAtFixedRate(
                    callback, periodMs, periodMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Returns the underlying scheduler service.
     *
     * @return the scheduled executor service
     */
    ScheduledExecutorService service() {
        return scheduler;
    }

    /**
     * Returns true if this scheduler was created internally and is owned by the runner.
     *
     * @return true if owned
     */
    boolean isOwned() {
        return schedulerOwned;
    }

    /**
     * Returns the periodic task future, or null if no periodic task is running.
     *
     * @return the scheduled future or null
     */
    ScheduledFuture<?> task() {
        return task;
    }

    /**
     * Cancels any active periodic task and shuts down the scheduler if owned.
     */
    @Override
    public void close() {
        if (task != null) {
            task.cancel(false);
        }

        if (schedulerOwned) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

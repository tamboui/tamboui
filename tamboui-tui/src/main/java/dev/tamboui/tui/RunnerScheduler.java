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
 * Manages the scheduler lifecycle and periodic runner tasks for TUI runners.
 * <p>
 * Owns the underlying {@link ScheduledExecutorService} (shutting it down on {@link #close()}
 * if created internally, or leaving it running if externally provided), and manages
 * any periodic runner task (ticks, resize checks) scheduled via {@link #start(Duration, Runnable)}.
 */
final class RunnerScheduler implements AutoCloseable {

    private final ScheduledExecutorService scheduler;
    private final boolean owned;
    private ScheduledFuture<?> task;

    RunnerScheduler(ScheduledExecutorService external) {
        Schedulers.Scheduler resolved = Schedulers.resolve(external);
        this.scheduler = resolved.scheduler();
        this.owned = resolved.owned();
    }

    /**
     * Schedules the given periodic callback at fixed rate if the period is not null.
     *
     * @param period the repetition period, or null if no periodic task is needed
     * @param callback the action to execute periodically
     */
    void start(Duration period, Runnable callback) {
        if (period != null) {
            Objects.requireNonNull(callback, "callback cannot be null");
            if (task != null) {
                task.cancel(false);
            }
            long periodMs = period.toMillis();
            this.task = this.scheduler.scheduleAtFixedRate(
                callback, periodMs, periodMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Returns the underlying {@link ScheduledExecutorService}.
     *
     * @return the scheduler service (never null)
     */
    ScheduledExecutorService service() {
        return scheduler;
    }

    /**
     * Returns true if this scheduler was created internally and is owned by this instance.
     *
     * @return true if owned
     */
    boolean isOwned() {
        return owned;
    }

    /**
     * Returns the periodic task future if one was scheduled, or null otherwise.
     *
     * @return the periodic task future, or null
     */
    ScheduledFuture<?> task() {
        return task;
    }

    @Override
    public void close() {
        // A scheduler passed in the config is not shut down below, and would otherwise keep
        // running the callback, and holding on to the runner, after close
        if (task != null) {
            task.cancel(false);
        }

        // Shutdown scheduler only if we own it
        if (owned) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

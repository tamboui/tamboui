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
 * Manages scheduler lifecycle and periodic task execution for TUI runners.
 * <p>
 * Encapsulates the runner's {@link ScheduledExecutorService}, whether it is owned
 * (internally created and shut down on close) or external (caller-owned and preserved on close),
 * and the runner's periodic scheduled task (ticks and resize checks).
 */
final class RunnerScheduler implements AutoCloseable {

    private final ScheduledExecutorService scheduler;
    private final boolean schedulerOwned;
    private ScheduledFuture<?> schedulerTask;

    /**
     * Creates a new {@code RunnerScheduler} resolving the given external scheduler.
     *
     * @param external the externally provided scheduler, or null to create an owned scheduler
     */
    RunnerScheduler(ScheduledExecutorService external) {
        Schedulers.Scheduler resolved = Schedulers.resolve(external);
        this.scheduler = resolved.scheduler();
        this.schedulerOwned = resolved.owned();
    }

    /**
     * Creates a {@code RunnerScheduler} with explicit scheduler and ownership.
     *
     * @param scheduler the scheduler to use
     * @param schedulerOwned true if this runner owns the scheduler and should shut it down on close
     */
    RunnerScheduler(ScheduledExecutorService scheduler, boolean schedulerOwned) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
        this.schedulerOwned = schedulerOwned;
    }

    /**
     * Starts the periodic task with the given period.
     * <p>
     * If a periodic task was already scheduled, it is cancelled before the new one is started.
     *
     * @param period the repetition period, or null if no periodic task is needed
     * @param callback the task to execute periodically
     */
    void start(Duration period, Runnable callback) {
        if (schedulerTask != null) {
            schedulerTask.cancel(false);
            schedulerTask = null;
        }
        if (period != null && callback != null && period.toMillis() > 0) {
            long periodMs = period.toMillis();
            this.schedulerTask = scheduler.scheduleAtFixedRate(
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
     * Returns true if the scheduler was internally created and will be shut down on {@link #close()}.
     *
     * @return true if owned
     */
    boolean isOwned() {
        return schedulerOwned;
    }

    /**
     * Returns the currently scheduled periodic task, or null if none is active.
     *
     * @return the scheduled task or null
     */
    ScheduledFuture<?> task() {
        return schedulerTask;
    }

    /**
     * Closes this scheduler and releases resources.
     * <p>
     * Cancels any active periodic task. If the scheduler is owned, it is shut down immediately
     * and awaits termination up to 100 milliseconds. If the scheduler is externally provided,
     * it is left running for the caller to manage.
     */
    @Override
    public void close() {
        if (schedulerTask != null) {
            schedulerTask.cancel(false);
            schedulerTask = null;
        }

        if (schedulerOwned && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for creating and resolving schedulers used by TUI runners.
 * <p>
 * This utility creates single-threaded daemon schedulers suitable for
 * tick events, resize handling, and user-scheduled actions.
 * <p>
 * When an external scheduler is provided, this utility validates it is
 * usable (not shut down) before returning it.
 */
final class Schedulers {

    private static final String THREAD_NAME_PREFIX = "tamboui-scheduler-";
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

    private Schedulers() {
        // Utility class
    }

    /**
     * Result of scheduler resolution containing the scheduler and ownership flag.
     */
    static final class Scheduler {
        private final ScheduledExecutorService scheduler;
        private final boolean owned;

        Scheduler(ScheduledExecutorService scheduler, boolean owned) {
            this.scheduler = scheduler;
            this.owned = owned;
        }

        /**
         * Returns the scheduler to use.
         *
         * @return the scheduler
         */
        ScheduledExecutorService scheduler() {
            return scheduler;
        }

        /**
         * Returns true if the scheduler was created by this utility and should be shut down on close.
         *
         * @return true if owned
         */
        boolean owned() {
            return owned;
        }
    }

    /**
     * Resolves the scheduler to use based on the provided external scheduler.
     * <p>
     * If the external scheduler is null, creates a new internal scheduler.
     * If the external scheduler is shut down, throws an exception.
     * Otherwise, uses the provided scheduler with owned=false.
     *
     * @param external the externally-provided scheduler, or null to create a new one
     * @return the scheduler result containing the scheduler and ownership flag
     * @throws IllegalStateException if the external scheduler is shut down
     */
    static Scheduler resolve(ScheduledExecutorService external) {
        if (external == null) {
            return new Scheduler(create(), true);
        }
        if (external.isShutdown()) {
            throw new IllegalStateException("Externally-provided scheduler is shut down");
        }
        return new Scheduler(external, false);
    }

    /**
     * Creates a new default scheduler.
     *
     * @return a new scheduler
     */
    static ScheduledExecutorService create() {
        return new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, THREAD_NAME_PREFIX + THREAD_COUNTER.getAndIncrement());
            t.setDaemon(true);
            return t;
        });
    }
}

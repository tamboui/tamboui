/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.util.concurrent.atomic.AtomicInteger;

import dev.tamboui.tui.error.TuiException;

/**
 * Utility class for render thread management.
 * <p>
 * TamboUI uses a dedicated render thread model similar to JavaFX. All rendering operations
 * must happen on a render thread. This class provides methods to check if the current
 * thread is a render thread and to assert that code is running on one.
 * <p>
 * Multiple concurrent render threads are supported (e.g. for concurrent virtual TUIs).
 * Each thread marks itself via {@link #markAsRenderThread()} when {@link TuiRunner#run}
 * starts and unmarks itself when it exits.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 * // Check if on render thread
 * if (RenderThread.isRenderThread()) {
 *     // Safe to perform UI operations
 * }
 *
 * // Assert on render thread (throws if not)
 * RenderThread.checkRenderThread();
 * doRenderOperation();
 * }</pre>
 *
 * @see TuiRunner#runOnRenderThread(Runnable)
 * @see TuiRunner#runLater(Runnable)
 */
public final class RenderThread {

    private static final ThreadLocal<Boolean> isRenderThread = new ThreadLocal<>();
    // The number of threads currently marked as render threads (i.e. active render loops)
    private static final AtomicInteger activeRenderThreads = new AtomicInteger(0);

    private RenderThread() {
        // Utility class
    }

    /**
     * Returns whether the current thread is a render thread.
     *
     * @return true if called from a render thread, false otherwise
     */
    public static boolean isRenderThread() {
        return Boolean.TRUE.equals(isRenderThread.get());
    }

    /**
     * Asserts that the current thread may perform render operations.
     * <p>
     * The check only enforces while a render loop is active (i.e. some thread has been marked as a
     * render thread by {@link TuiRunner#run}). In that case, this must be called from a render thread.
     * When no render loop is active, the check passes: a standalone render, such as an inline
     * {@code println} of an element or a unit test, needs no render thread.
     *
     * @throws TuiException if a render loop is active and this is not a render thread
     */
    public static void checkRenderThread() {
        if (isRenderThread()) {
            return;
        }
        if (activeRenderThreads.get() > 0) {
            Thread current = Thread.currentThread();
            throw new TuiException(
                "Must be called on render thread. Current: " + current.getName() +
                " (id=" + current.getId() + ")");
        }
    }

    /**
     * Marks the current thread as a render thread. Package-private for use by TuiRunner.
     * <p>
     * Idempotent: marking an already-marked thread does not change the active render thread count.
     */
    static void markAsRenderThread() {
        if (!Boolean.TRUE.equals(isRenderThread.get())) {
            isRenderThread.set(Boolean.TRUE);
            activeRenderThreads.incrementAndGet();
        }
    }

    /**
     * Clears the render thread mark from the current thread. Package-private for use by TuiRunner.
     */
    static void clearRenderThread() {
        if (Boolean.TRUE.equals(isRenderThread.get())) {
            isRenderThread.remove();
            activeRenderThreads.decrementAndGet();
        }
    }
}

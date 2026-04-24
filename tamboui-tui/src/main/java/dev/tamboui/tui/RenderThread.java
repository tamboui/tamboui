/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

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
     * Asserts that the current thread is a render thread.
     * <p>
     * This should be called at the start of any method that must only be
     * executed on a render thread.
     *
     * @throws TuiException if this thread has not been marked as a render thread
     */
    public static void checkRenderThread() {
        if (!Boolean.TRUE.equals(isRenderThread.get())) {
            Thread current = Thread.currentThread();
            throw new TuiException(
                "Must be called on render thread. Current: " + current.getName() +
                " (id=" + current.getId() + ")");
        }
    }

    /**
     * Marks the current thread as a render thread. Package-private for use by TuiRunner.
     */
    static void markAsRenderThread() {
        isRenderThread.set(Boolean.TRUE);
    }

    /**
     * Clears the render thread mark from the current thread. Package-private for use by TuiRunner.
     */
    static void clearRenderThread() {
        isRenderThread.remove();
    }
}

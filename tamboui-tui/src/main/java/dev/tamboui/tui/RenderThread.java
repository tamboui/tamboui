/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.util.concurrent.atomic.AtomicReference;

import dev.tamboui.tui.error.TuiException;

/**
 * Utility class for render thread management.
 * <p>
 * TamboUI uses a dedicated render thread model similar to JavaFX. All rendering operations
 * must happen on the render thread. This class provides methods to check if the current
 * thread is the render thread and to assert that code is running on the render thread.
 * <p>
 * The render thread is set when {@link TuiRunner#run} starts and cleared when it exits.
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

    private static final AtomicReference<Thread> renderThread = new AtomicReference<>();

    private RenderThread() {
        // Utility class
    }

    /**
     * Returns whether the current thread is the render thread.
     *
     * @return true if called from the render thread, false otherwise
     */
    public static boolean isRenderThread() {
        return Thread.currentThread() == renderThread.get();
    }

    /**
     * Asserts that the current thread is the render thread.
     * <p>
     * This should be called at the start of any method that must only be
     * executed on the render thread.
     * <p>
     * The check only enforces when a render thread has been set (i.e., when
     * TuiRunner.run() is active). If no render thread has been set, the check
     * passes silently, allowing unit tests to run without special setup.
     *
     * @throws IllegalStateException if a render thread has been set and this is not it
     */
    public static void checkRenderThread() {
        Thread ui = renderThread.get();
        // Only enforce if render thread has been set
        if (ui != null && Thread.currentThread() != ui) {
            Thread current = Thread.currentThread();
            throw new TuiException(
                "Must be called on render thread. Current: " + current.getName() +
                " (id=" + current.getId() + "), render thread: " + ui.getName());
        }
    }

    /**
     * Sets the render thread. Package-private for use by TuiRunner.
     *
     * @param thread the thread to set as the render thread
     */
    static void setRenderThread(Thread thread) {
        renderThread.set(thread);
    }

    /**
     * Clears the render thread reference. Package-private for use by TuiRunner.
     */
    static void clearRenderThread() {
        renderThread.set(null);
    }
}

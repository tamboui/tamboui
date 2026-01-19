/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility class for UI thread management.
 * <p>
 * TamboUI uses a dedicated UI thread model similar to JavaFX. All rendering operations
 * must happen on the UI thread. This class provides methods to check if the current
 * thread is the UI thread and to assert that code is running on the UI thread.
 * <p>
 * The UI thread is set when {@link TuiRunner#run} starts and cleared when it exits.
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 * // Check if on UI thread
 * if (UiThread.isUiThread()) {
 *     // Safe to perform UI operations
 * }
 *
 * // Assert on UI thread (throws if not)
 * UiThread.checkUiThread();
 * doRenderOperation();
 * }</pre>
 *
 * @see TuiRunner#runOnUiThread(Runnable)
 * @see TuiRunner#runLater(Runnable)
 */
public final class UiThread {

    private static final AtomicReference<Thread> uiThread = new AtomicReference<>();

    private UiThread() {
        // Utility class
    }

    /**
     * Returns whether the current thread is the UI thread.
     *
     * @return true if called from the UI thread, false otherwise
     */
    public static boolean isUiThread() {
        return Thread.currentThread() == uiThread.get();
    }

    /**
     * Asserts that the current thread is the UI thread.
     * <p>
     * This should be called at the start of any method that must only be
     * executed on the UI thread.
     * <p>
     * The check only enforces when a UI thread has been set (i.e., when
     * TuiRunner.run() is active). If no UI thread has been set, the check
     * passes silently, allowing unit tests to run without special setup.
     *
     * @throws IllegalStateException if a UI thread has been set and this is not it
     */
    public static void checkUiThread() {
        Thread ui = uiThread.get();
        // Only enforce if UI thread has been set
        if (ui != null && Thread.currentThread() != ui) {
            Thread current = Thread.currentThread();
            throw new IllegalStateException(
                "Must be called on UI thread. Current: " + current.getName() +
                " (id=" + current.getId() + "), UI thread: " + ui.getName());
        }
    }

    /**
     * Sets the UI thread. Package-private for use by TuiRunner.
     *
     * @param thread the thread to set as the UI thread
     */
    static void setUiThread(Thread thread) {
        uiThread.set(thread);
    }

    /**
     * Clears the UI thread reference. Package-private for use by TuiRunner.
     */
    static void clearUiThread() {
        uiThread.set(null);
    }
}

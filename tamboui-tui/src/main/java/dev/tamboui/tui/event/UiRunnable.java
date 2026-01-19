/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/**
 * An event that encapsulates a {@link Runnable} to be executed on the UI thread.
 * <p>
 * UiRunnable events are used internally by {@link dev.tamboui.tui.TuiRunner#runOnUiThread(Runnable)}
 * and {@link dev.tamboui.tui.TuiRunner#runLater(Runnable)} to schedule work on the UI thread
 * from other threads.
 * <p>
 * When the TuiRunner event loop processes a UiRunnable event, it executes the
 * enclosed action directly on the UI thread.
 *
 * @see dev.tamboui.tui.TuiRunner#runOnUiThread(Runnable)
 * @see dev.tamboui.tui.TuiRunner#runLater(Runnable)
 */
public final class UiRunnable implements Event {

    private final Runnable action;

    /**
     * Creates a new UiRunnable with the given action.
     *
     * @param action the action to execute on the UI thread
     */
    public UiRunnable(Runnable action) {
        this.action = action;
    }

    /**
     * Executes the enclosed action.
     * <p>
     * This is called by the TuiRunner event loop when processing this event.
     */
    public void run() {
        action.run();
    }
}

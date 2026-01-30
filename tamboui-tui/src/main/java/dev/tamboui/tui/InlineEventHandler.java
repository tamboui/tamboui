/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import dev.tamboui.tui.event.Event;

/**
 * Functional interface for handling inline TUI events.
 * <p>
 * The event handler is called for each event received by the application.
 * Return {@code true} to trigger a redraw, or {@code false} to skip redrawing.
 *
 * @see InlineTuiRunner#run(InlineEventHandler, Renderer)
 */
@FunctionalInterface
public interface InlineEventHandler {

    /**
     * Handles an event.
     *
     * @param event
     *            the event to handle
     * @param runner
     *            the inline TUI runner (can be used to call
     *            {@link InlineTuiRunner#quit()})
     * @return {@code true} if the UI should be redrawn, {@code false} otherwise
     */
    boolean handle(Event event, InlineTuiRunner runner);
}

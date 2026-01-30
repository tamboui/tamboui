/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import dev.tamboui.tui.event.KeyEvent;

/**
 * Handler for key events on an element.
 */
@FunctionalInterface
public interface KeyEventHandler {
    /**
     * Handles a key event.
     *
     * @param event
     *            the key event
     * @return HANDLED if the event was handled and should not propagate, UNHANDLED
     *         otherwise
     */
    EventResult handle(KeyEvent event);
}

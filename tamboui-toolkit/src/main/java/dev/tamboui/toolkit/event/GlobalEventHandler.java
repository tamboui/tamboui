/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import dev.tamboui.tui.event.Event;

/**
 * Handler for global events that are processed before element-specific handlers.
 * <p>
 * Global handlers can intercept events before they reach elements, allowing
 * for application-wide keyboard shortcuts or action handling.
 *
 * @see EventRouter#addGlobalHandler(GlobalEventHandler)
 */
@FunctionalInterface
public interface GlobalEventHandler {

    /**
     * Handles an event.
     *
     * @param event the event to handle
     * @return HANDLED if the event was consumed, UNHANDLED to let it propagate
     */
    EventResult handle(Event event);
}

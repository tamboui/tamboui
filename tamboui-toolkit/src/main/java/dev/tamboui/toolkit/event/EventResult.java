/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

/**
 * Result of handling an event.
 */
public enum EventResult {
    /**
     * The event was handled and should not be propagated further.
     */
    HANDLED,

    /**
     * The event was not handled and should continue propagating.
     */
    UNHANDLED;

    /**
     * Returns whether this result indicates the event was handled.
     *
     * @return true if handled
     */
    public boolean isHandled() {
        return this == HANDLED;
    }

    /**
     * Returns whether this result indicates the event was not handled.
     *
     * @return true if not handled
     */
    public boolean isUnhandled() {
        return this == UNHANDLED;
    }

    /**
     * Combines two results, returning HANDLED if either is HANDLED.
     *
     * @param other
     *            the other result
     * @return HANDLED if either is HANDLED, UNHANDLED otherwise
     */
    public EventResult or(EventResult other) {
        return this == HANDLED || other == HANDLED ? HANDLED : UNHANDLED;
    }
}

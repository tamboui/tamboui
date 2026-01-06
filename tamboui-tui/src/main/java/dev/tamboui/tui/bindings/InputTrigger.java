/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.tui.event.Event;

/**
 * Represents an input trigger that can match an {@link Event}.
 * <p>
 * Input triggers define what keyboard or mouse input activates an action.
 * Implementations include:
 * <ul>
 *   <li>{@link KeyTrigger} - Triggers on keyboard events</li>
 *   <li>{@link MouseTrigger} - Triggers on mouse events</li>
 * </ul>
 *
 * @see KeyTrigger
 * @see MouseTrigger
 */
public interface InputTrigger {

    /**
     * Returns true if this trigger matches the given event.
     *
     * @param event the event to match against
     * @return true if the trigger matches
     */
    boolean matches(Event event);

    /**
     * Returns a human-readable description of this trigger.
     * <p>
     * Examples: "Up", "Ctrl+c", "Mouse.Left.Press"
     *
     * @return the trigger description
     */
    String describe();
}

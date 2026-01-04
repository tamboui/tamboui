/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.keymap;

import dev.tamboui.tui.event.KeyEvent;

import java.util.List;
import java.util.Optional;

/**
 * Maps key events to semantic actions.
 * <p>
 * KeyMaps are immutable and can be customized via the builder pattern.
 *
 * <pre>{@code
 * // Using a predefined keymap
 * KeyMap keymap = KeyMaps.vim();
 *
 * // Checking if an event matches an action
 * if (keymap.matches(event, Action.MOVE_UP)) {
 *     state.moveUp();
 * }
 *
 * // Customizing a keymap
 * KeyMap custom = KeyMaps.standard()
 *     .toBuilder()
 *     .bind(Action.QUIT, KeyBinding.ch('x'))
 *     .build();
 * }</pre>
 *
 * @see KeyMaps
 * @see KeyBinding
 */
public interface KeyMap {

    /**
     * Returns true if the event matches the given action.
     *
     * @param event  the key event
     * @param action the action to check
     * @return true if the event triggers the action
     */
    boolean matches(KeyEvent event, Action action);

    /**
     * Returns the action that matches the event, if any.
     * <p>
     * If multiple actions could match, returns the first one found.
     *
     * @param event the key event
     * @return the matching action, or empty if no action matches
     */
    Optional<Action> actionFor(KeyEvent event);

    /**
     * Returns all bindings for the given action.
     *
     * @param action the action
     * @return list of bindings (may be empty, never null)
     */
    List<KeyBinding> bindingsFor(Action action);

    /**
     * Returns a human-readable description of bindings for the action.
     * <p>
     * Useful for generating help screens.
     *
     * @param action the action
     * @return description like "Up, k" or "(unbound)" if no bindings
     */
    String describeBindings(Action action);

    /**
     * Returns a builder pre-populated with this keymap's bindings.
     *
     * @return a new builder for customizing this keymap
     */
    Builder toBuilder();

    /**
     * Builder for creating custom keymaps.
     */
    interface Builder {

        /**
         * Binds an action to one or more key bindings.
         * <p>
         * This adds to existing bindings for the action.
         *
         * @param action   the action to bind
         * @param bindings the key bindings
         * @return this builder
         */
        Builder bind(Action action, KeyBinding... bindings);

        /**
         * Replaces all bindings for an action.
         *
         * @param action   the action
         * @param bindings the new bindings (replaces existing)
         * @return this builder
         */
        Builder rebind(Action action, KeyBinding... bindings);

        /**
         * Removes all bindings for an action.
         *
         * @param action the action to unbind
         * @return this builder
         */
        Builder unbind(Action action);

        /**
         * Copies all bindings from another keymap.
         *
         * @param other the keymap to copy from
         * @return this builder
         */
        Builder copyFrom(KeyMap other);

        /**
         * Builds the keymap.
         *
         * @return an immutable keymap
         */
        KeyMap build();
    }
}

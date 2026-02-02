/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import java.util.List;
import java.util.Optional;

import dev.tamboui.tui.event.Event;

/**
 * Maps input events to semantic actions.
 * <p>
 * Bindings support both keyboard and mouse events, allowing the same
 * action to be triggered by either input type.
 * <p>
 * Actions are identified by strings, with common actions defined as
 * constants in {@link Actions}. Custom actions can use any string.
 * <p>
 * Events (both {@code KeyEvent} and {@code MouseEvent}) carry their own bindings
 * and provide convenience methods for action matching. The preferred API is to
 * call {@code matches()} directly on the event:
 *
 * <pre>{@code
 * // Preferred: call matches() on the event
 * if (event.matches(Actions.MOVE_UP)) {
 *     state.moveUp();
 * }
 *
 * // Using custom action names
 * if (event.matches("myApp.save")) {
 *     save();
 * }
 *
 * // Customizing bindings
 * Bindings custom = BindingSets.standard()
 *     .toBuilder()
 *     .bind(KeyTrigger.ch('x'), Actions.QUIT)
 *     .bind(MouseTrigger.rightClick(), "myAction")
 *     .build();
 * }</pre>
 *
 * @see Actions
 * @see BindingSets
 * @see InputTrigger
 */
public interface Bindings {

    /**
     * Returns true if the event matches the given action.
     *
     * @param event  the input event
     * @param action the action to check
     * @return true if the event triggers the action
     */
    boolean matches(Event event, String action);

    /**
     * Returns the action that matches the event, if any.
     * <p>
     * If multiple actions could match, returns the first one found.
     *
     * @param event the input event
     * @return the matching action, or empty if no action matches
     */
    Optional<String> actionFor(Event event);

    /**
     * Returns all triggers for the given action.
     *
     * @param action the action
     * @return list of triggers (may be empty, never null)
     */
    List<InputTrigger> triggersFor(String action);

    /**
     * Returns a human-readable description of triggers for the action.
     * <p>
     * Useful for generating help screens.
     *
     * @param action the action
     * @return description like "Up, k" or "(unbound)" if no triggers
     */
    String describeBindings(String action);

    /**
     * Returns a builder pre-populated with this bindings' triggers.
     *
     * @return a new builder for customizing these bindings
     */
    Builder toBuilder();

    /**
     * Builder for creating custom bindings.
     */
    interface Builder {

        /**
         * Binds a trigger to an action.
         * <p>
         * This adds to existing triggers for the action.
         *
         * @param trigger the input trigger
         * @param action  the action to bind to
         * @return this builder
         */
        Builder bind(InputTrigger trigger, String action);

        /**
         * Replaces all triggers for an action with the given trigger.
         *
         * @param trigger the new trigger (replaces existing triggers)
         * @param action  the action
         * @return this builder
         */
        Builder rebind(InputTrigger trigger, String action);

        /**
         * Removes all triggers for an action.
         *
         * @param action the action to unbind
         * @return this builder
         */
        Builder unbind(String action);

        /**
         * Copies all triggers from another bindings instance.
         *
         * @param other the bindings to copy from
         * @return this builder
         */
        Builder copyFrom(Bindings other);

        /**
         * Builds the bindings.
         *
         * @return an immutable bindings instance
         */
        Bindings build();
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;

/**
 * An {@link InputTrigger} that matches {@link MouseEvent}s.
 * <p>
 * Mouse triggers can match combinations of:
 * <ul>
 *   <li>Event kind (PRESS, RELEASE, SCROLL_UP, SCROLL_DOWN, etc.)</li>
 *   <li>Button (LEFT, RIGHT, MIDDLE)</li>
 *   <li>Keyboard modifiers (Ctrl, Alt, Shift held during mouse action)</li>
 * </ul>
 *
 * <pre>{@code
 * // Match left click
 * MouseTrigger.click()
 *
 * // Match right click
 * MouseTrigger.rightClick()
 *
 * // Match Ctrl+click
 * MouseTrigger.ctrlClick()
 *
 * // Match scroll up
 * MouseTrigger.scrollUp()
 *
 * // Custom: Alt+right click
 * MouseTrigger.press(MouseButton.RIGHT, false, true, false)
 * }</pre>
 */
public final class MouseTrigger implements InputTrigger {

    private final MouseEventKind kind;
    private final MouseButton button;
    private final boolean ctrl;
    private final boolean alt;
    private final boolean shift;

    private MouseTrigger(MouseEventKind kind, MouseButton button,
                         boolean ctrl, boolean alt, boolean shift) {
        this.kind = kind;
        this.button = button;
        this.ctrl = ctrl;
        this.alt = alt;
        this.shift = shift;
    }

    /**
     * Creates a trigger for a mouse button press.
     *
     * @param button the mouse button
     * @return a trigger that matches the button press
     */
    public static MouseTrigger press(MouseButton button) {
        return new MouseTrigger(MouseEventKind.PRESS, button, false, false, false);
    }

    /**
     * Creates a trigger for a mouse button press with modifiers.
     *
     * @param button the mouse button
     * @param ctrl   true if Ctrl must be pressed
     * @param alt    true if Alt must be pressed
     * @param shift  true if Shift must be pressed
     * @return a trigger that matches the button press with modifiers
     */
    public static MouseTrigger press(MouseButton button, boolean ctrl, boolean alt, boolean shift) {
        return new MouseTrigger(MouseEventKind.PRESS, button, ctrl, alt, shift);
    }

    /**
     * Creates a trigger for a mouse button release.
     *
     * @param button the mouse button
     * @return a trigger that matches the button release
     */
    public static MouseTrigger release(MouseButton button) {
        return new MouseTrigger(MouseEventKind.RELEASE, button, false, false, false);
    }

    /**
     * Creates a trigger for left mouse button click (press).
     *
     * @return a trigger that matches left click
     */
    public static MouseTrigger click() {
        return press(MouseButton.LEFT);
    }

    /**
     * Creates a trigger for right mouse button click (press).
     *
     * @return a trigger that matches right click
     */
    public static MouseTrigger rightClick() {
        return press(MouseButton.RIGHT);
    }

    /**
     * Creates a trigger for middle mouse button click (press).
     *
     * @return a trigger that matches middle click
     */
    public static MouseTrigger middleClick() {
        return press(MouseButton.MIDDLE);
    }

    /**
     * Creates a trigger for Ctrl+left click.
     *
     * @return a trigger that matches Ctrl+click
     */
    public static MouseTrigger ctrlClick() {
        return press(MouseButton.LEFT, true, false, false);
    }

    /**
     * Creates a trigger for Shift+left click.
     *
     * @return a trigger that matches Shift+click
     */
    public static MouseTrigger shiftClick() {
        return press(MouseButton.LEFT, false, false, true);
    }

    /**
     * Creates a trigger for Alt+left click.
     *
     * @return a trigger that matches Alt+click
     */
    public static MouseTrigger altClick() {
        return press(MouseButton.LEFT, false, true, false);
    }

    /**
     * Creates a trigger for scroll wheel up.
     *
     * @return a trigger that matches scroll up
     */
    public static MouseTrigger scrollUp() {
        return new MouseTrigger(MouseEventKind.SCROLL_UP, MouseButton.NONE, false, false, false);
    }

    /**
     * Creates a trigger for scroll wheel down.
     *
     * @return a trigger that matches scroll down
     */
    public static MouseTrigger scrollDown() {
        return new MouseTrigger(MouseEventKind.SCROLL_DOWN, MouseButton.NONE, false, false, false);
    }

    /**
     * Creates a trigger for a drag event.
     *
     * @param button the button held during drag
     * @return a trigger that matches the drag
     */
    public static MouseTrigger drag(MouseButton button) {
        return new MouseTrigger(MouseEventKind.DRAG, button, false, false, false);
    }

    /**
     * Creates a mouse trigger with all parameters specified.
     *
     * @param kind   the event kind
     * @param button the mouse button
     * @param ctrl   true if Ctrl must be pressed
     * @param alt    true if Alt must be pressed
     * @param shift  true if Shift must be pressed
     * @return a trigger that matches the specified combination
     */
    public static MouseTrigger of(MouseEventKind kind, MouseButton button,
                                  boolean ctrl, boolean alt, boolean shift) {
        return new MouseTrigger(kind, button, ctrl, alt, shift);
    }

    @Override
    public boolean matches(Event event) {
        if (!(event instanceof MouseEvent)) {
            return false;
        }
        return matchesMouse((MouseEvent) event);
    }

    /**
     * Returns true if this trigger matches the given mouse event.
     *
     * @param event the mouse event to match against
     * @return true if this trigger matches the event
     */
    public boolean matchesMouse(MouseEvent event) {
        if (event.kind() != kind) {
            return false;
        }

        // For scroll events, button is NONE
        if (button != MouseButton.NONE && event.button() != button) {
            return false;
        }

        // Check modifiers
        KeyModifiers mods = event.modifiers();
        return ctrl == mods.ctrl() && alt == mods.alt() && shift == mods.shift();
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (ctrl) {
            sb.append("Ctrl+");
        }
        if (alt) {
            sb.append("Alt+");
        }
        if (shift) {
            sb.append("Shift+");
        }
        sb.append("Mouse.");
        if (button != MouseButton.NONE) {
            sb.append(button.name()).append(".");
        }
        sb.append(kind.name());
        return sb.toString();
    }

    @Override
    public String toString() {
        return describe();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MouseTrigger)) {
            return false;
        }
        MouseTrigger that = (MouseTrigger) o;
        return ctrl == that.ctrl &&
               alt == that.alt &&
               shift == that.shift &&
               kind == that.kind &&
               button == that.button;
    }

    @Override
    public int hashCode() {
        int result = kind != null ? kind.hashCode() : 0;
        result = 31 * result + (button != null ? button.hashCode() : 0);
        result = 31 * result + (ctrl ? 1 : 0);
        result = 31 * result + (alt ? 1 : 0);
        result = 31 * result + (shift ? 1 : 0);
        return result;
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;

import java.util.Optional;

/**
 * Represents a mouse input event.
 * <p>
 * MouseEvent is associated with {@link Bindings} that determine how semantic
 * actions are mapped to mouse inputs. Use the convenience methods like
 * {@link #isClick()}, {@link #isRightClick()}, etc. to check if this event
 * matches a semantic action according to the configured bindings.
 *
 * <pre>{@code
 * // Check semantic actions
 * if (event.isClick()) {
 *     handleClick(event.x(), event.y());
 * }
 * if (event.isRightClick()) {
 *     showContextMenu(event.x(), event.y());
 * }
 *
 * // Or use explicit action matching
 * if (event.matches(Actions.CLICK)) {
 *     handleClick(event.x(), event.y());
 * }
 *
 * // Or use custom action names
 * if (event.matches("myApp.contextMenu")) {
 *     showCustomMenu();
 * }
 * }</pre>
 */
public final class MouseEvent implements Event {

    private final MouseEventKind kind;
    private final MouseButton button;
    private final int x;
    private final int y;
    private final KeyModifiers modifiers;
    private final Bindings bindings;

    /**
     * Creates a mouse event with the default bindings.
     *
     * @param kind      kind of mouse event
     * @param button    button involved (or {@link MouseButton#NONE})
     * @param x         x coordinate (0-based)
     * @param y         y coordinate (0-based)
     * @param modifiers keyboard modifiers active during the event
     */
    public MouseEvent(
        MouseEventKind kind,
        MouseButton button,
        int x,
        int y,
        KeyModifiers modifiers
    ) {
        this(kind, button, x, y, modifiers, BindingSets.defaults());
    }

    /**
     * Creates a mouse event with specific bindings.
     *
     * @param kind      kind of mouse event
     * @param button    button involved (or {@link MouseButton#NONE})
     * @param x         x coordinate (0-based)
     * @param y         y coordinate (0-based)
     * @param modifiers keyboard modifiers active during the event
     * @param bindings  the bindings for semantic action matching
     */
    public MouseEvent(
        MouseEventKind kind,
        MouseButton button,
        int x,
        int y,
        KeyModifiers modifiers,
        Bindings bindings
    ) {
        this.kind = kind;
        this.button = button;
        this.x = x;
        this.y = y;
        this.modifiers = modifiers;
        this.bindings = bindings;
    }

    /**
     * Creates a mouse press event.
     *
     * @param button button pressed
     * @param x      x coordinate
     * @param y      y coordinate
     * @return mouse event
     */
    public static MouseEvent press(MouseButton button, int x, int y) {
        return new MouseEvent(MouseEventKind.PRESS, button, x, y, KeyModifiers.NONE);
    }

    /**
     * Creates a mouse press event with specific bindings.
     *
     * @param button   button pressed
     * @param x        x coordinate
     * @param y        y coordinate
     * @param bindings the bindings for semantic action matching
     * @return mouse event
     */
    public static MouseEvent press(MouseButton button, int x, int y, Bindings bindings) {
        return new MouseEvent(MouseEventKind.PRESS, button, x, y, KeyModifiers.NONE, bindings);
    }

    /**
     * Creates a mouse release event.
     *
     * @param button button released
     * @param x      x coordinate
     * @param y      y coordinate
     * @return mouse event
     */
    public static MouseEvent release(MouseButton button, int x, int y) {
        return new MouseEvent(MouseEventKind.RELEASE, button, x, y, KeyModifiers.NONE);
    }

    /**
     * Creates a mouse move event.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return mouse event
     */
    public static MouseEvent move(int x, int y) {
        return new MouseEvent(MouseEventKind.MOVE, MouseButton.NONE, x, y, KeyModifiers.NONE);
    }

    /**
     * Creates a mouse drag event.
     *
     * @param button button held during drag
     * @param x      x coordinate
     * @param y      y coordinate
     * @return mouse event
     */
    public static MouseEvent drag(MouseButton button, int x, int y) {
        return new MouseEvent(MouseEventKind.DRAG, button, x, y, KeyModifiers.NONE);
    }

    /**
     * Creates a scroll up event.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return mouse event
     */
    public static MouseEvent scrollUp(int x, int y) {
        return new MouseEvent(MouseEventKind.SCROLL_UP, MouseButton.NONE, x, y, KeyModifiers.NONE);
    }

    /**
     * Creates a scroll down event.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return mouse event
     */
    public static MouseEvent scrollDown(int x, int y) {
        return new MouseEvent(MouseEventKind.SCROLL_DOWN, MouseButton.NONE, x, y, KeyModifiers.NONE);
    }

    /**
     * Returns true if this is a press event.
     */
    public boolean isPress() {
        return kind == MouseEventKind.PRESS;
    }

    /**
     * Returns true if this is a release event.
     */
    public boolean isRelease() {
        return kind == MouseEventKind.RELEASE;
    }

    /**
     * Returns true if this is a left button event.
     */
    public boolean isLeftButton() {
        return button == MouseButton.LEFT;
    }

    /**
     * Returns true if this is a right button event.
     */
    public boolean isRightButton() {
        return button == MouseButton.RIGHT;
    }

    /**
     * Returns true if this is a scroll event.
     */
    public boolean isScroll() {
        return kind == MouseEventKind.SCROLL_UP || kind == MouseEventKind.SCROLL_DOWN;
    }

    /** Returns the event kind. */
    public MouseEventKind kind() {
        return kind;
    }

    /** Returns the button involved in the event. */
    public MouseButton button() {
        return button;
    }

    /** Returns the x coordinate (0-based). */
    public int x() {
        return x;
    }

    /** Returns the y coordinate (0-based). */
    public int y() {
        return y;
    }

    /** Returns active keyboard modifiers. */
    public KeyModifiers modifiers() {
        return modifiers;
    }

    /** Returns the bindings associated with this event. */
    public Bindings bindings() {
        return bindings;
    }

    // ========== Semantic Action Methods (delegating to bindings) ==========

    /**
     * Returns true if this event matches the given action in the configured bindings.
     *
     * @param action the action to check (use {@link Actions} constants or custom strings)
     * @return true if this event triggers the action
     */
    public boolean matches(String action) {
        return bindings.matches(this, action);
    }

    /**
     * Returns the action that this event matches, if any.
     *
     * @return the matching action name, or empty if no action matches
     */
    public Optional<String> action() {
        return bindings.actionFor(this);
    }

    /**
     * Returns true if this is a "click" event (left mouse press) according to the bindings.
     */
    public boolean isClick() {
        return matches(Actions.CLICK);
    }

    /**
     * Returns true if this is a "right click" event according to the bindings.
     */
    public boolean isRightClick() {
        return matches(Actions.RIGHT_CLICK);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MouseEvent)) {
            return false;
        }
        MouseEvent that = (MouseEvent) o;
        return x == that.x
            && y == that.y
            && kind == that.kind
            && button == that.button
            && modifiers.equals(that.modifiers);
    }

    @Override
    public int hashCode() {
        int result = kind != null ? kind.hashCode() : 0;
        result = 31 * result + (button != null ? button.hashCode() : 0);
        result = 31 * result + Integer.hashCode(x);
        result = 31 * result + Integer.hashCode(y);
        result = 31 * result + modifiers.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format(
            "MouseEvent[kind=%s, button=%s, x=%d, y=%d, modifiers=%s]",
            kind,
            button,
            x,
            y,
            modifiers
        );
    }
}

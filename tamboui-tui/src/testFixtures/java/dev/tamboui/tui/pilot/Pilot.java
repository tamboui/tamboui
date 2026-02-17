/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import java.time.Duration;

import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;

/**
 * Fluent API for driving a TUI application in tests (key presses, mouse, resize).
 * <p>
 * All implementations support coordinate-based input. Implementations for
 * toolkit applications (e.g. {@link dev.tamboui.toolkit.app.ToolkitPilot}) may
 * also support widget selection by element ID ({@link #findElement(String)},
 * {@link #hasElement(String)}, {@link #click(String)}). The default implementations
 * of the by-ID methods throw or return false so that basic pilots (e.g. TuiPilot)
 * do not need to implement them.
 */
public interface Pilot {

    // --- Keys ---

    /**
     * Simulates a key press with no modifiers.
     *
     * @param keyCode the key code
     */
    void press(KeyCode keyCode);

    /**
     * Simulates a key press with modifiers.
     *
     * @param keyCode   the key code
     * @param modifiers the modifiers
     */
    void press(KeyCode keyCode, KeyModifiers modifiers);

    /**
     * Simulates a character key press with no modifiers.
     *
     * @param c the character
     */
    void press(char c);

    /**
     * Simulates a character key press with modifiers.
     *
     * @param c         the character
     * @param modifiers the modifiers
     */
    void press(char c, KeyModifiers modifiers);

    /**
     * Simulates a sequence of key presses (e.g. "q", "Enter", "Escape").
     * Single-character strings are treated as characters; longer strings
     * are parsed as KeyCode enum names.
     *
     * @param keys the keys to press in order
     */
    void press(String... keys);

    // --- Mouse ---

    /**
     * Simulates a mouse button press at the given position.
     *
     * @param button the button
     * @param x      x coordinate (0-based)
     * @param y      y coordinate (0-based)
     */
    void mousePress(MouseButton button, int x, int y);

    /**
     * Simulates a mouse button release at the given position.
     *
     * @param button the button
     * @param x      x coordinate (0-based)
     * @param y      y coordinate (0-based)
     */
    void mouseRelease(MouseButton button, int x, int y);

    /**
     * Simulates a left click at the given position (press followed by release).
     *
     * @param x x coordinate (0-based)
     * @param y y coordinate (0-based)
     */
    void click(int x, int y);

    /**
     * Simulates a mouse move to the given position.
     *
     * @param x x coordinate (0-based)
     * @param y y coordinate (0-based)
     */
    void mouseMove(int x, int y);

    // --- Terminal ---

    /**
     * Simulates a terminal resize.
     *
     * @param width  the new width in columns
     * @param height the new height in rows
     */
    void resize(int width, int height);

    /**
     * Simulates a terminal resize.
     *
     * @param size the new size
     */
    void resize(Size size);

    /**
     * Pauses briefly to allow event processing (default delay).
     */
    void pause();

    /**
     * Pauses for the given duration to allow event processing.
     *
     * @param delay the delay
     */
    void pause(Duration delay);

    /**
     * Quits the application.
     */
    void quit();

    // --- Widget selection by ID (optional; default = unsupported) ---

    /**
     * Clicks the center of the element with the given ID.
     * <p>
     * Default implementation throws {@link UnsupportedOperationException}.
     * Override in pilots that support widget selection (e.g. ToolkitPilot).
     *
     * @param elementId the element ID
     * @throws ElementNotFoundException if the element is not found
     * @throws UnsupportedOperationException if this pilot does not support widget selection
     */
    default void click(String elementId) throws ElementNotFoundException {
        throw new UnsupportedOperationException("Widget selection by ID is not supported");
    }

    /**
     * Clicks the element with the given ID at an offset from its top-left.
     * <p>
     * Default implementation throws {@link UnsupportedOperationException}.
     *
     * @param elementId the element ID
     * @param offsetX   x offset from the element's left
     * @param offsetY   y offset from the element's top
     * @throws ElementNotFoundException if the element is not found
     * @throws UnsupportedOperationException if this pilot does not support widget selection
     */
    default void click(String elementId, int offsetX, int offsetY) throws ElementNotFoundException {
        throw new UnsupportedOperationException("Widget selection by ID is not supported");
    }

    /**
     * Returns the rendered area of the element with the given ID.
     * <p>
     * Default implementation throws {@link UnsupportedOperationException}.
     *
     * @param elementId the element ID
     * @return the element's area
     * @throws ElementNotFoundException if the element is not found
     * @throws UnsupportedOperationException if this pilot does not support widget selection
     */
    default Rect findElement(String elementId) throws ElementNotFoundException {
        throw new UnsupportedOperationException("Widget selection by ID is not supported");
    }

    /**
     * Returns whether an element with the given ID exists.
     * <p>
     * Default implementation returns false.
     *
     * @param elementId the element ID
     * @return true if the element exists
     */
    default boolean hasElement(String elementId) {
        return false;
    }

    // --- Convenience defaults (optional) ---

    /**
     * Simulates a double-click (two clicks in quick succession).
     *
     * @param x x coordinate (0-based)
     * @param y y coordinate (0-based)
     */
    default void doubleClick(int x, int y) {
        click(x, y);
        pause();
        click(x, y);
    }

    /**
     * Simulates a double-click on an element by its ID.
     *
     * @param elementId the element ID to double-click
     * @throws ElementNotFoundException if the element is not found
     * @throws UnsupportedOperationException if widget selection is not supported
     */
    default void doubleClick(String elementId) throws ElementNotFoundException {
        Rect area = findElement(elementId);
        int centerX = area.x() + area.width() / 2;
        int centerY = area.y() + area.height() / 2;
        doubleClick(centerX, centerY);
    }

    /**
     * Simulates a triple-click (three clicks in quick succession).
     *
     * @param x x coordinate (0-based)
     * @param y y coordinate (0-based)
     */
    default void tripleClick(int x, int y) {
        doubleClick(x, y);
        pause();
        click(x, y);
    }

    /**
     * Simulates hovering over a position (mouse move + pause).
     *
     * @param x x coordinate (0-based)
     * @param y y coordinate (0-based)
     */
    default void hover(int x, int y) {
        mouseMove(x, y);
        pause();
    }

    /**
     * Simulates hovering over an element by its ID.
     *
     * @param elementId the element ID to hover over
     * @throws ElementNotFoundException if the element is not found
     * @throws UnsupportedOperationException if widget selection is not supported
     */
    default void hover(String elementId) throws ElementNotFoundException {
        Rect area = findElement(elementId);
        int centerX = area.x() + area.width() / 2;
        int centerY = area.y() + area.height() / 2;
        hover(centerX, centerY);
    }
}

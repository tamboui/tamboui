/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;

import java.time.Duration;

/**
 * Interface for programmatically operating a TUI application during testing.
 * <p>
 * Implementations are provided by each module (TuiRunner, ToolkitRunner).
 * This interface defines the common testing API that all implementations must support.
 *
 * <pre>{@code
 * try (TestRunner test = TestRunner.runTest(...)) {
 *     Pilot pilot = test.pilot();
 *     pilot.press('q');
 *     pilot.click(10, 5);
 *     pilot.resize(100, 30);
 * }
 * }</pre>
 */
public interface Pilot {

    // ========== Basic Operations (all implementations) ==========

    /**
     * Simulates pressing a key.
     *
     * @param keyCode the key code to press
     */
    void press(KeyCode keyCode);

    /**
     * Simulates pressing a key with modifiers.
     *
     * @param keyCode the key code to press
     * @param modifiers the modifier keys
     */
    void press(KeyCode keyCode, KeyModifiers modifiers);

    /**
     * Simulates pressing a character key.
     *
     * @param c the character to press
     */
    void press(char c);

    /**
     * Simulates pressing a character key with modifiers.
     *
     * @param c the character to press
     * @param modifiers the modifier keys
     */
    void press(char c, KeyModifiers modifiers);

    /**
     * Simulates pressing multiple keys in sequence.
     *
     * @param keys the keys to press (as strings like "q", "Enter", "Escape")
     */
    void press(String... keys);

    /**
     * Simulates a mouse click (press + release).
     *
     * @param x the x coordinate (0-based)
     * @param y the y coordinate (0-based)
     */
    void click(int x, int y);

    /**
     * Simulates a mouse press event.
     *
     * @param button the mouse button
     * @param x the x coordinate (0-based)
     * @param y the y coordinate (0-based)
     */
    void mousePress(MouseButton button, int x, int y);

    /**
     * Simulates a mouse release event.
     *
     * @param button the mouse button
     * @param x the x coordinate (0-based)
     * @param y the y coordinate (0-based)
     */
    void mouseRelease(MouseButton button, int x, int y);

    /**
     * Simulates a mouse move event.
     *
     * @param x the x coordinate (0-based)
     * @param y the y coordinate (0-based)
     */
    void mouseMove(int x, int y);

    /**
     * Simulates a terminal resize.
     *
     * @param width the new width
     * @param height the new height
     */
    void resize(int width, int height);

    /**
     * Simulates a terminal resize.
     *
     * @param size the new size
     */
    void resize(Size size);

    /**
     * Inserts a brief pause to allow events to be processed.
     * This is useful when chaining multiple interactions.
     */
    void pause();

    /**
     * Inserts a pause with the specified duration.
     *
     * @param delay the delay duration
     */
    void pause(Duration delay);

    /**
     * Quits the TUI application.
     */
    void quit();

    // ========== Widget Selection (optional - throws UnsupportedOperationException if not supported) ==========

    /**
     * Clicks on an element by its ID.
     * <p>
     * This method is only supported by implementations that have widget selection
     * capabilities (e.g., ToolkitRunner). For basic TuiRunner implementations,
     * this throws {@link UnsupportedOperationException}.
     *
     * @param elementId the element ID to click
     * @throws ElementNotFoundException if the element is not found
     * @throws UnsupportedOperationException if widget selection is not supported
     */
    default void click(String elementId) throws ElementNotFoundException {
        throw new UnsupportedOperationException("Widget selection not supported");
    }

    /**
     * Clicks on an element by its ID with an offset.
     * <p>
     * This method is only supported by implementations that have widget selection
     * capabilities (e.g., ToolkitRunner). For basic TuiRunner implementations,
     * this throws {@link UnsupportedOperationException}.
     *
     * @param elementId the element ID to click
     * @param offsetX the x offset from the element's top-left corner
     * @param offsetY the y offset from the element's top-left corner
     * @throws ElementNotFoundException if the element is not found
     * @throws UnsupportedOperationException if widget selection is not supported
     */
    default void click(String elementId, int offsetX, int offsetY) throws ElementNotFoundException {
        throw new UnsupportedOperationException("Widget selection not supported");
    }

    /**
     * Finds an element by its ID and returns its area.
     * <p>
     * This method is only supported by implementations that have widget selection
     * capabilities (e.g., ToolkitRunner). For basic TuiRunner implementations,
     * this throws {@link UnsupportedOperationException}.
     *
     * @param elementId the element ID to find
     * @return the element's area
     * @throws ElementNotFoundException if the element is not found
     * @throws UnsupportedOperationException if widget selection is not supported
     */
    default Rect findElement(String elementId) throws ElementNotFoundException {
        throw new UnsupportedOperationException("Widget selection not supported");
    }

    /**
     * Checks if an element with the given ID exists.
     * <p>
     * This method is only supported by implementations that have widget selection
     * capabilities (e.g., ToolkitRunner). For basic TuiRunner implementations,
     * this returns false.
     *
     * @param elementId the element ID to check
     * @return true if the element exists
     */
    default boolean hasElement(String elementId) {
        return false;
    }

    // ========== Enhanced Features (optional, with default implementations) ==========

    /**
     * Simulates a double-click (two clicks in quick succession).
     *
     * @param x the x coordinate (0-based)
     * @param y the y coordinate (0-based)
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
     * @param x the x coordinate (0-based)
     * @param y the y coordinate (0-based)
     */
    default void tripleClick(int x, int y) {
        doubleClick(x, y);
        pause();
        click(x, y);
    }

    /**
     * Simulates hovering over a position (mouse move + pause).
     *
     * @param x the x coordinate (0-based)
     * @param y the y coordinate (0-based)
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

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.pilot.ElementNotFoundException;
import dev.tamboui.tui.pilot.Pilot;
import dev.tamboui.tui.pilot.TestBackend;

import java.time.Duration;
import java.util.Map;

/**
 * Pilot implementation for ToolkitRunner applications with widget selection.
 * <p>
 * This implementation extends the basic Pilot functionality with the ability
 * to interact with elements by their ID, using the FocusManager to locate them.
 */
public final class ToolkitPilot implements Pilot {

    private final TuiRunner tuiRunner;
    private final TestBackend backend;
    private final FocusManager focusManager;

    /**
     * Creates a pilot for the given runner, backend, and focus manager.
     *
     * @param tuiRunner the underlying TUI runner
     * @param backend the test backend
     * @param focusManager the focus manager for widget selection
     */
    ToolkitPilot(TuiRunner tuiRunner, TestBackend backend, FocusManager focusManager) {
        this.tuiRunner = tuiRunner;
        this.backend = backend;
        this.focusManager = focusManager;
    }

    // Basic operations (duplicated from TuiPilot to avoid cross-module dependencies)

    @Override
    public void press(KeyCode keyCode) {
        press(keyCode, KeyModifiers.NONE);
    }

    @Override
    public void press(KeyCode keyCode, KeyModifiers modifiers) {
        dev.tamboui.tui.event.KeyEvent event = dev.tamboui.tui.event.KeyEvent.ofKey(keyCode, modifiers);
        dispatch(event);
    }

    @Override
    public void press(char c) {
        press(c, KeyModifiers.NONE);
    }

    @Override
    public void press(char c, KeyModifiers modifiers) {
        dev.tamboui.tui.event.KeyEvent event = dev.tamboui.tui.event.KeyEvent.ofChar(c, modifiers);
        dispatch(event);
    }

    @Override
    public void press(String... keys) {
        for (String key : keys) {
            if (key.length() == 1) {
                press(key.charAt(0));
            } else {
                // Try to parse as KeyCode enum name
                try {
                    KeyCode code = KeyCode.valueOf(key.toUpperCase());
                    press(code);
                } catch (IllegalArgumentException e) {
                    // If not a valid key code, treat as a single character
                    if (key.length() > 0) {
                        press(key.charAt(0));
                    }
                }
            }
            pause();
        }
    }

    @Override
    public void mousePress(MouseButton button, int x, int y) {
        dev.tamboui.tui.event.MouseEvent event = dev.tamboui.tui.event.MouseEvent.press(button, x, y);
        dispatch(event);
    }

    @Override
    public void mouseRelease(MouseButton button, int x, int y) {
        dev.tamboui.tui.event.MouseEvent event = dev.tamboui.tui.event.MouseEvent.release(button, x, y);
        dispatch(event);
    }

    @Override
    public void click(int x, int y) {
        click(MouseButton.LEFT, x, y);
    }

    /**
     * Simulates a mouse click (press + release).
     *
     * @param button the mouse button
     * @param x the x coordinate (0-based)
     * @param y the y coordinate (0-based)
     */
    private void click(MouseButton button, int x, int y) {
        mousePress(button, x, y);
        pause();
        mouseRelease(button, x, y);
        pause();
    }

    @Override
    public void mouseMove(int x, int y) {
        dev.tamboui.tui.event.MouseEvent event = dev.tamboui.tui.event.MouseEvent.move(x, y);
        dispatch(event);
    }

    @Override
    public void resize(int width, int height) {
        dev.tamboui.tui.event.ResizeEvent event = dev.tamboui.tui.event.ResizeEvent.of(width, height);
        dispatch(event);
    }

    @Override
    public void resize(Size size) {
        resize(size.width(), size.height());
    }

    @Override
    public void pause() {
        try {
            Thread.sleep(50); // Delay to allow event processing
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void dispatch(dev.tamboui.tui.event.Event event) {
        tuiRunner.dispatch(event);
        pause();
    }

    @Override
    public void quit() {
        tuiRunner.quit();
    }

    // Widget selection methods (unique to ToolkitPilot)

    @Override
    public void click(String elementId) throws ElementNotFoundException {
        Rect area = findElement(elementId);
        int centerX = area.x() + area.width() / 2;
        int centerY = area.y() + area.height() / 2;
        click(centerX, centerY);
    }

    @Override
    public void click(String elementId, int offsetX, int offsetY) throws ElementNotFoundException {
        Rect area = findElement(elementId);
        click(area.x() + offsetX, area.y() + offsetY);
    }

    @Override
    public Rect findElement(String elementId) throws ElementNotFoundException {
        Map<String, Rect> areas = focusManager.findElements();
        Rect area = areas.get(elementId);
        if (area == null) {
            throw new ElementNotFoundException("Element not found: " + elementId);
        }
        return area;
    }

    @Override
    public boolean hasElement(String elementId) {
        return focusManager.focusableAreas().containsKey(elementId);
    }
}

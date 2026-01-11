/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import dev.tamboui.layout.Size;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.pilot.Pilot;
import dev.tamboui.tui.pilot.TestBackend;

import java.time.Duration;

/**
 * Pilot implementation for TuiRunner applications.
 * <p>
 * This implementation provides basic testing capabilities but does not
 * support widget selection by ID (that requires ToolkitRunner).
 */
public final class TuiPilot implements Pilot {

    private final TuiRunner runner;
    private final TestBackend backend;

    /**
     * Creates a pilot for the given runner and backend.
     *
     * @param runner the TUI runner
     * @param backend the test backend
     */
    TuiPilot(TuiRunner runner, TestBackend backend) {
        this.runner = runner;
        this.backend = backend;
    }

    /**
     * Returns the TUI runner being controlled by this pilot.
     *
     * @return the TUI runner
     */
    public TuiRunner runner() {
        return runner;
    }

    @Override
    public void press(KeyCode keyCode) {
        press(keyCode, KeyModifiers.NONE);
    }

    @Override
    public void press(KeyCode keyCode, KeyModifiers modifiers) {
        KeyEvent event = KeyEvent.ofKey(keyCode, modifiers);
        injectEvent(event);
    }

    @Override
    public void press(char c) {
        press(c, KeyModifiers.NONE);
    }

    @Override
    public void press(char c, KeyModifiers modifiers) {
        KeyEvent event = KeyEvent.ofChar(c, modifiers);
        injectEvent(event);
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
        MouseEvent event = MouseEvent.press(button, x, y);
        injectEvent(event);
    }

    @Override
    public void mouseRelease(MouseButton button, int x, int y) {
        MouseEvent event = MouseEvent.release(button, x, y);
        injectEvent(event);
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
    public void click(MouseButton button, int x, int y) {
        mousePress(button, x, y);
        pause();
        mouseRelease(button, x, y);
        pause();
    }

    @Override
    public void mouseMove(int x, int y) {
        MouseEvent event = MouseEvent.move(x, y);
        injectEvent(event);
    }

    @Override
    public void resize(int width, int height) {
        ResizeEvent event = ResizeEvent.of(width, height);
        injectEvent(event);
    }

    @Override
    public void resize(Size size) {
        resize(size.width(), size.height());
    }

    @Override
    public void pause() {
        try {
            Thread.sleep(50); // Delay to allow event processing (needs to be longer than pollTimeout)
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

    @Override
    public void quit() {
        runner.quit();
    }

    private void injectEvent(dev.tamboui.tui.event.Event event) {
        runner.injectEvent(event);
        pause();
    }
}

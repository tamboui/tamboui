/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import java.time.Duration;

import dev.tamboui.layout.Size;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.ResizeEvent;

/**
 * Pilot implementation for TuiRunner applications.
 * Drives the application via {@link TuiRunner#dispatch(Event)} and does not
 * support widget selection by ID (uses interface defaults).
 */
public final class TuiPilot implements Pilot {

    private static final int DEFAULT_PAUSE_MS = 50;

    private final TuiRunner tuiRunner;
    private final TestBackend backend;

    /**
     * Creates a pilot for the given runner and backend.
     *
     * @param tuiRunner the TUI runner
     * @param backend   the test backend (for size; events are injected via dispatch)
     */
    public TuiPilot(TuiRunner tuiRunner, TestBackend backend) {
        this.tuiRunner = tuiRunner;
        this.backend = backend;
    }

    @Override
    public void press(KeyCode keyCode) {
        press(keyCode, KeyModifiers.NONE);
    }

    @Override
    public void press(KeyCode keyCode, KeyModifiers modifiers) {
        dispatch(KeyEvent.ofKey(keyCode, modifiers));
    }

    @Override
    public void press(char c) {
        press(c, KeyModifiers.NONE);
    }

    @Override
    public void press(char c, KeyModifiers modifiers) {
        dispatch(KeyEvent.ofChar(c, modifiers));
    }

    @Override
    public void press(String... keys) {
        for (String key : keys) {
            if (key.length() == 1) {
                press(key.charAt(0));
            } else {
                try {
                    KeyCode code = KeyCode.valueOf(key.toUpperCase());
                    press(code);
                } catch (IllegalArgumentException e) {
                    if (!key.isEmpty()) {
                        press(key.charAt(0));
                    }
                }
            }
            pause();
        }
    }

    @Override
    public void mousePress(MouseButton button, int x, int y) {
        dispatch(MouseEvent.press(button, x, y));
    }

    @Override
    public void mouseRelease(MouseButton button, int x, int y) {
        dispatch(MouseEvent.release(button, x, y));
    }

    @Override
    public void click(int x, int y) {
        click(MouseButton.LEFT, x, y);
    }

    private void click(MouseButton button, int x, int y) {
        mousePress(button, x, y);
        pause();
        mouseRelease(button, x, y);
        pause();
    }

    @Override
    public void mouseMove(int x, int y) {
        dispatch(MouseEvent.move(x, y));
    }

    @Override
    public void resize(int width, int height) {
        dispatch(ResizeEvent.of(width, height));
    }

    @Override
    public void resize(Size size) {
        resize(size.width(), size.height());
    }

    @Override
    public void pause() {
        pause(Duration.ofMillis(DEFAULT_PAUSE_MS));
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
        tuiRunner.quit();
    }

    private void dispatch(dev.tamboui.tui.event.Event event) {
        tuiRunner.dispatch(event);
        pause();
    }
}

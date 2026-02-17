/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import java.time.Duration;

import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.pilot.ElementNotFoundException;
import dev.tamboui.tui.pilot.Pilot;

/**
 * Pilot implementation for ToolkitRunner applications.
 * Supports widget selection by element ID via the runner's element registry.
 */
public final class ToolkitPilot implements Pilot {

    private static final int DEFAULT_PAUSE_MS = 50;

    private final TuiRunner tuiRunner;
    private final TestBackend backend;
    private final ToolkitRunner runner;

    /**
     * Creates a pilot for the given toolkit runner and backend.
     *
     * @param runner the toolkit runner
     * @param backend the test backend
     */
    public ToolkitPilot(ToolkitRunner runner, TestBackend backend) {
        this.runner = runner;
        this.tuiRunner = runner.tuiRunner();
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
        Rect area = runner.elementRegistry().getArea(elementId);
        if (area == null) {
            throw new ElementNotFoundException("Element not found: " + elementId);
        }
        return area;
    }

    @Override
    public boolean hasElement(String elementId) {
        return runner.elementRegistry().contains(elementId);
    }

    private void dispatch(dev.tamboui.tui.event.Event event) {
        tuiRunner.dispatch(event);
        pause();
    }
}

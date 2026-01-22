/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Frame-compatible wrapper around {@link InlineDisplay}.
 * <p>
 * This internal class bridges the widget rendering system with inline displays,
 * allowing elements and widgets to render using the standard Frame API while
 * the output stays inline in the terminal.
 */
final class InlineViewport {

    private final InlineDisplay display;
    private final Buffer buffer;
    private final Rect area;
    private final Frame frame;

    /**
     * Creates a new viewport wrapping the given display.
     *
     * @param display the inline display to wrap
     */
    InlineViewport(InlineDisplay display) {
        this.display = display;
        this.area = Rect.of(display.width(), display.height());
        this.buffer = Buffer.empty(area);
        this.frame = Frame.forTesting(buffer);
    }

    /**
     * Returns the width of the viewport.
     *
     * @return the width in characters
     */
    int width() {
        return area.width();
    }

    /**
     * Returns the height of the viewport.
     *
     * @return the height in lines
     */
    int height() {
        return area.height();
    }

    /**
     * Returns the viewport area.
     *
     * @return the area rectangle
     */
    Rect area() {
        return area;
    }

    /**
     * Draws the UI using the given renderer.
     * <p>
     * The buffer is cleared, the renderer is called, and then
     * the buffer content is pushed to the inline display.
     *
     * @param renderer the render function that populates the frame
     */
    void draw(Consumer<Frame> renderer) {
        buffer.clear();
        frame.clearCursor();  // Reset cursor before render
        renderer.accept(frame);

        // Get cursor position from frame (if set by a text input)
        int cursorX = frame.cursorPosition().map(p -> p.x()).orElse(-1);
        int cursorY = frame.cursorPosition().map(p -> p.y()).orElse(-1);

        display.render((a, b) -> {
            // Copy our buffer to the display's buffer
            for (int y = 0; y < area.height(); y++) {
                for (int x = 0; x < area.width(); x++) {
                    b.set(x, y, buffer.get(x, y));
                }
            }
        }, cursorX, cursorY);
    }

    /**
     * Prints a plain text message above the viewport.
     *
     * @param message the message to print
     */
    void println(String message) {
        display.println(message);
    }

    /**
     * Prints styled text above the viewport.
     *
     * @param text the styled text to print
     */
    void println(Text text) {
        display.println(text);
    }

    /**
     * Releases the display and moves the cursor below the viewport.
     */
    void release() {
        display.release();
    }

    /**
     * Closes the underlying display.
     *
     * @throws IOException if an I/O error occurs
     */
    void close() throws IOException {
        display.close();
    }
}

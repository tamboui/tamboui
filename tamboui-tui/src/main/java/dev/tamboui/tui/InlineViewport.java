/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.io.IOException;
import java.util.function.Consumer;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;

/**
 * Frame-compatible wrapper around {@link InlineDisplay}.
 * <p>
 * This internal class bridges the widget rendering system with inline displays,
 * allowing elements and widgets to render using the standard Frame API while
 * the output stays inline in the terminal.
 */
final class InlineViewport {

    private final InlineDisplay display;
    private Buffer buffer;
    private Rect area;
    private Frame frame;
    private int contentHeight;  // Current content height to allocate

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
        this.contentHeight = display.height();  // Default to initial height
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
     * Sets the content height for the next draw.
     * <p>
     * This determines how many terminal lines will be allocated
     * for the inline display. The display will grow or shrink
     * accordingly on the next draw() call. If the requested height
     * exceeds the current buffer size, the buffer is resized.
     *
     * @param height the desired content height in lines
     */
    void setContentHeight(int height) {
        height = Math.max(0, height);
        // Grow buffer if needed
        if (height > area.height()) {
            this.area = Rect.of(area.width(), height);
            this.buffer = Buffer.empty(area);
            this.frame = Frame.forTesting(buffer);
        }
        this.contentHeight = height;
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
            // Note: Only copy up to currentHeight, but the display handles resizing
            for (int y = 0; y < Math.min(contentHeight, area.height()); y++) {
                for (int x = 0; x < area.width(); x++) {
                    b.set(x, y, buffer.get(x, y));
                }
            }
        }, contentHeight, cursorX, cursorY);
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

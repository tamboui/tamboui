/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.inline;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AnsiStringBuilder;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.text.Text;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.BiConsumer;

/**
 * Manages a fixed-height inline display area for Gradle/NPM-style progress UX.
 * <p>
 * InlineDisplay reserves a number of lines at the current cursor position and
 * allows rendering widgets to that area. Content can be printed above the display
 * area using {@link #println(String)}, which scrolls output while the status area
 * stays in place.
 *
 * <p>Unlike full TUI runners, InlineDisplay does NOT:
 * <ul>
 *   <li>Enter alternate screen mode</li>
 *   <li>Hide the cursor</li>
 *   <li>Capture the entire terminal</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * try (InlineDisplay display = InlineDisplay.create(3)) {
 *     for (int i = 0; i <= 100; i += 10) {
 *         display.render((area, buffer) -> {
 *             myProgressWidget.render(area, buffer);
 *         });
 *         display.println("Step " + i + " completed");
 *         Thread.sleep(100);
 *     }
 * }
 * }</pre>
 *
 * @see Buffer#toAnsiString()
 */
public final class InlineDisplay implements AutoCloseable {

    private static final String ESC = "\u001b";
    private static final String CSI = ESC + "[";

    private final int height;
    private final int width;
    private final Buffer buffer;
    private final PrintWriter out;
    private final Backend backend;
    private boolean initialized;
    private boolean released;
    private boolean shouldClearOnClose;

    InlineDisplay(int height, int width, Backend backend, PrintWriter out) {
        this.height = height;
        this.width = width;
        this.backend = backend;
        this.out = out;
        this.buffer = Buffer.empty(Rect.of(width, height));
        this.initialized = false;
        this.released = false;
        this.shouldClearOnClose = false;
    }

    /**
     * Creates an InlineDisplay with the specified height.
     * The width is automatically set to the terminal width.
     *
     * @param height the number of lines to reserve for the display area
     * @return a new InlineDisplay
     * @throws IOException if terminal initialization fails
     */
    public static InlineDisplay create(int height) throws IOException {
        Backend backend = BackendFactory.create();
        Size size = backend.size();
        PrintWriter out = new PrintWriter(System.out, true);
        return new InlineDisplay(height, size.width(), backend, out);
    }

    /**
     * Creates an InlineDisplay with the specified height and width.
     *
     * @param height the number of lines to reserve for the display area
     * @param width  the width of the display area in characters
     * @return a new InlineDisplay
     * @throws IOException if terminal initialization fails
     */
    public static InlineDisplay create(int height, int width) throws IOException {
        Backend backend = BackendFactory.create();
        PrintWriter out = new PrintWriter(System.out, true);
        return new InlineDisplay(height, width, backend, out);
    }

    /**
     * Configures the display to clear the status area when closed.
     * By default, the status area content remains visible after close.
     *
     * @return this display for chaining
     */
    public InlineDisplay clearOnClose() {
        this.shouldClearOnClose = true;
        return this;
    }

    /**
     * Renders widgets to the status area.
     * The provided consumer receives the display area and buffer to render into.
     *
     * @param renderer the rendering function that populates the buffer
     */
    public void render(BiConsumer<Rect, Buffer> renderer) {
        ensureInitialized();
        buffer.clear();
        renderer.accept(buffer.area(), buffer);
        redrawStatusArea();
    }

    /**
     * Sets a single line of text in the status area.
     * This is a convenience method for simple text updates without widgets.
     *
     * @param line    the line index (0-based, must be less than height)
     * @param content the text content to display
     */
    public void setLine(int line, String content) {
        if (line < 0 || line >= height) {
            return;
        }
        ensureInitialized();

        // Clear the line in buffer and set new content
        for (int x = 0; x < width; x++) {
            buffer.set(x, line, Cell.EMPTY);
        }
        buffer.setString(0, line, content, Style.EMPTY);

        redrawStatusArea();
    }

    /**
     * Sets a single line of styled text in the status area.
     *
     * @param line the line index (0-based, must be less than height)
     * @param text the styled text to display
     */
    public void setLine(int line, Text text) {
        if (line < 0 || line >= height) {
            return;
        }
        ensureInitialized();

        // Clear the line in buffer
        for (int x = 0; x < width; x++) {
            buffer.set(x, line, Cell.EMPTY);
        }

        // Render each line of the text
        if (!text.lines().isEmpty()) {
            buffer.setLine(0, line, text.lines().get(0));
        }

        redrawStatusArea();
    }

    /**
     * Prints a line of text above the status area.
     * The text scrolls up as new lines are added, while the status area
     * stays in place at the bottom.
     *
     * @param message the message to print
     */
    public void println(String message) {
        ensureInitialized();

        // Move cursor to top of status area
        out.print(CSI + height + "A");  // Move up
        out.print("\r");                 // Move to start of line

        // Insert a new line (pushes status area down)
        out.print(CSI + "L");

        // Print the message
        out.print(message);
        out.print(CSI + "K");  // Clear to end of line

        // Move back down to bottom of status area
        out.print("\n");
        out.print(CSI + (height - 1) + "B");

        out.flush();

        // Redraw the status area since it was pushed down
        redrawStatusArea();
    }

    /**
     * Prints styled text above the status area.
     *
     * @param text the styled text to print
     */
    public void println(Text text) {
        // Render text to a temporary buffer for ANSI conversion
        if (text.lines().isEmpty()) {
            println("");
            return;
        }

        Buffer tempBuffer = Buffer.empty(Rect.of(width, 1));
        tempBuffer.setLine(0, 0, text.lines().get(0));
        println(tempBuffer.toAnsiStringTrimmed());
    }

    /**
     * Explicitly releases the display before close().
     * Moves the cursor below the status area and optionally clears it.
     */
    public void release() {
        if (released) {
            return;
        }

        if (shouldClearOnClose) {
            clearStatusArea();
        }

        // Move cursor to after the status area
        out.print("\r");
        out.print(CSI + "0m");  // Reset style
        out.println();
        out.flush();

        released = true;
    }

    /**
     * Returns the height of this display in lines.
     *
     * @return the display height
     */
    public int height() {
        return height;
    }

    /**
     * Returns the width of this display in characters.
     *
     * @return the display width
     */
    public int width() {
        return width;
    }

    @Override
    public void close() throws IOException {
        if (!released) {
            release();
        }
        backend.close();
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }

        // Print blank lines to reserve space for the status area
        for (int i = 0; i < height; i++) {
            out.println();
        }

        // Move cursor back up to the start of the status area
        out.print(CSI + height + "A");
        out.flush();

        initialized = true;
    }

    private void redrawStatusArea() {
        // Save cursor position
        out.print(CSI + "s");

        // Move to start of status area
        out.print("\r");

        // Render each line
        for (int y = 0; y < height; y++) {
            if (y > 0) {
                out.print("\n\r");
            }

            // Render the line from buffer
            Style lastStyle = null;
            for (int x = 0; x < width; x++) {
                Cell cell = buffer.get(x, y);

                if (!cell.style().equals(lastStyle)) {
                    out.print(AnsiStringBuilder.styleToAnsi(cell.style()));
                    lastStyle = cell.style();
                }
                out.print(cell.symbol());
            }

            // Clear to end of line (in case content is shorter than before)
            out.print(CSI + "K");
        }

        // Reset style and restore cursor position
        out.print(AnsiStringBuilder.RESET);
        out.print(CSI + "u");
        out.flush();
    }

    private void clearStatusArea() {
        // Move to start of status area
        out.print("\r");

        // Clear each line
        for (int y = 0; y < height; y++) {
            if (y > 0) {
                out.print("\n\r");
            }
            out.print(CSI + "K");  // Clear line
        }

        // Move back up
        if (height > 1) {
            out.print(CSI + (height - 1) + "A");
        }
        out.flush();
    }
}

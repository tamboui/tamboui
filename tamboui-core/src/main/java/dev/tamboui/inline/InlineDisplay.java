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
import java.io.Writer;
import java.util.function.BiConsumer;

/**
 * Manages a fixed-height inline display area for Gradle/NPM-style progress UX.
 * <p>
 * InlineDisplay reserves a number of lines at the current cursor position and
 * allows rendering widgets to that area. Content can be printed above the display
 * area using {@link #println(String)}, which scrolls output while the display area
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
    private int lastCursorY;  // Track where cursor was left for next render

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
        PrintWriter out = createPrintWriter(backend);
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
        return withBackend(height, width, backend);
    }

    /**
     * Creates an InlineDisplay using an existing backend. The InlineDisplay will not take ownership
     * of the backend; callers should avoid invoking {@link #close()} and instead call {@link #release()}.
     *
     * @param height the number of lines to reserve for the display area
     * @param width  the width of the display area in characters
     * @param backend the backend to use for output
     * @return a new InlineDisplay
     * @throws IOException if initialization fails
     */
    public static InlineDisplay withBackend(int height, int width, Backend backend) throws IOException {
        PrintWriter out = createPrintWriter(backend);
        return new InlineDisplay(height, width, backend, out);
    }

    private static PrintWriter createPrintWriter(Backend backend) {
        try {
            // Probe backend raw support by writing and flushing a no-op sequence
            backend.writeRaw("");
            return new PrintWriter(new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    backend.writeRaw(new String(cbuf, off, len));
                }

                @Override
                public void flush() throws IOException {
                    backend.flush();
                }

                @Override
                public void close() throws IOException {
                    flush();
                }
            }, true);
        } catch (UnsupportedOperationException | IOException e) {
            // Fallback to System.out
            return new PrintWriter(System.out, true);
        }
    }

    /**
     * Configures the display to clear the display area when closed.
     * By default, the display area content remains visible after close.
     *
     * @return this display for chaining
     */
    public InlineDisplay clearOnClose() {
        this.shouldClearOnClose = true;
        return this;
    }

    /**
     * Renders widgets to the display area.
     * The provided consumer receives the area and buffer to render into.
     *
     * @param renderer the rendering function that populates the buffer
     */
    public void render(BiConsumer<Rect, Buffer> renderer) {
        render(renderer, -1, -1);
    }

    /**
     * Renders widgets to the display area with explicit cursor positioning.
     * The provided consumer receives the area and buffer to render into.
     *
     * @param renderer the rendering function that populates the buffer
     * @param cursorX the x position for the cursor, or -1 to use default positioning
     * @param cursorY the y position for the cursor, or -1 to use default positioning
     */
    public void render(BiConsumer<Rect, Buffer> renderer, int cursorX, int cursorY) {
        ensureInitialized();
        buffer.clear();
        renderer.accept(buffer.area(), buffer);
        redrawDisplayArea(cursorX, cursorY);
    }

    /**
     * Sets a single line of text in the display area.
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

        redrawDisplayArea(-1, -1);
    }

    /**
     * Sets a single line of styled text in the display area.
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

        redrawDisplayArea(-1, -1);
    }

    /**
     * Prints a line of text above the display area.
     * The text scrolls up as new lines are added, while the display area
     * stays in place at the bottom.
     *
     * @param message the message to print
     */
    public void println(String message) {
        ensureInitialized();

        // Move cursor to top of display area
        out.print(CSI + height + "A");  // Move up
        out.print("\r");                 // Move to start of line

        // Insert a new line (pushes display area down)
        out.print(CSI + "L");

        // Print the message
        out.print(message);
        out.print(CSI + "K");  // Clear to end of line

        // Move back down to bottom of display area
        out.print("\n");
        out.print(CSI + (height - 1) + "B");

        out.flush();

        // Redraw the display area since it was pushed down
        redrawDisplayArea(-1, -1);
    }

    /**
     * Prints styled text above the display area.
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
     * Moves the cursor below the display area and optionally clears it.
     */
    public void release() {
        if (released) {
            return;
        }

        if (shouldClearOnClose) {
            clearDisplayArea();
        }

        // Move cursor to after the display area and restore cursor visibility
        out.print("\r");
        out.print(CSI + "0m");  // Reset style
        out.print(CSI + "?25h");  // Show cursor again
        out.print(CSI + "0 q");  // Reset cursor to default style
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

        // Hide cursor - we render cursor as a styled cell in the buffer instead.
        // This avoids flicker from hide/show cycling during redraws.
        out.print(CSI + "?25l");

        // Print blank lines to reserve space for the display area
        for (int i = 0; i < height; i++) {
            out.println();
        }

        // Move cursor back up to the start of the display area
        out.print(CSI + height + "A");
        out.flush();

        initialized = true;
    }

    private void redrawDisplayArea(int cursorX, int cursorY) {
        // Cursor is hidden since initialization - no need to hide/show here.
        // TextInput renders cursor as a styled cell (reversed) in the buffer.

        // Move to start of display area (line 0)
        // First, go to start of current line
        out.print("\r");
        // Then move up to line 0 if cursor was left on a different line
        if (lastCursorY > 0) {
            out.print(CSI + lastCursorY + "A");
        }

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

        // Reset style
        out.print(AnsiStringBuilder.RESET);

        // Position cursor
        // First go back to start of display area
        if (height > 1) {
            out.print(CSI + (height - 1) + "A");  // Move up to first line
        }
        out.print("\r");

        if (cursorX >= 0 && cursorY >= 0) {
            // Use explicit cursor position
            if (cursorY > 0) {
                out.print(CSI + cursorY + "B");  // Move down to cursor line
            }
            if (cursorX > 0) {
                out.print(CSI + cursorX + "C");  // Move right to cursor column
            }
            lastCursorY = cursorY;
        } else {
            // Default: move cursor to end of first line content (for prompt-style UX)
            int endX = findLastContentPosition(0);
            if (endX > 0) {
                out.print(CSI + endX + "C");  // Move right to end of content
            }
            lastCursorY = 0;
        }

        // Keep terminal cursor hidden - TextInput renders cursor as a styled cell
        // in the buffer (reversed style), so we don't need the terminal cursor.
        // This eliminates flicker from hide/show cycling.
        out.flush();
    }

    /**
     * Finds the position after the last non-empty cell on a line.
     */
    private int findLastContentPosition(int line) {
        for (int x = width - 1; x >= 0; x--) {
            Cell cell = buffer.get(x, line);
            String symbol = cell.symbol();
            if (!symbol.isEmpty() && !symbol.equals(" ")) {
                return x + 1;
            }
        }
        return 0;
    }

    private void clearDisplayArea() {
        // Move to start of display area
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

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.inline;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AnsiCellWriter;
import dev.tamboui.terminal.AnsiStringBuilder;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Text;

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

    private static final Pattern LINE_BREAK = Pattern.compile("\r?\n");

    private final int height;  // Maximum height
    private final boolean autoWidth;
    private int width;
    private Buffer buffer;  // Resizable buffer
    private final PrintWriter out;
    private final Backend backend;
    private boolean initialized;
    private boolean released;
    private boolean shouldClearOnClose;
    private int lastCursorY;  // Track where cursor was left for next render
    private int currentHeight;  // Current terminal lines allocated

    InlineDisplay(int height, int width, Backend backend, PrintWriter out) {
        this(height, width, false, backend, out);
    }

    InlineDisplay(int height, int width, boolean autoWidth, Backend backend, PrintWriter out) {
        this.height = height;
        this.width = width;
        this.autoWidth = autoWidth;
        this.backend = backend;
        this.out = out;
        this.buffer = Buffer.empty(Rect.of(width, height));
        this.initialized = false;
        this.released = false;
        this.shouldClearOnClose = false;
        this.currentHeight = 0;  // Not allocated yet
    }

    /**
     * Creates an InlineDisplay with the specified height.
     * The width is automatically set to the terminal width and tracks terminal
     * resizes on subsequent updates.
     *
     * @param height the number of lines to reserve for the display area
     * @return a new InlineDisplay
     * @throws IOException if terminal initialization fails
     */
    public static InlineDisplay create(int height) throws IOException {
        Backend backend = BackendFactory.create();
        return withBackend(height, backend);
    }

    /**
     * Creates an InlineDisplay with the specified height using an existing backend.
     * <p>
     * The width is initialized from the backend and tracks terminal resizes on
     * subsequent updates.
     * The InlineDisplay will not take ownership of the backend; callers should
     * avoid invoking {@link #close()} and instead call {@link #release()}.
     *
     * @param height the number of lines to reserve for the display area
     * @param backend the backend to use for output
     * @return a new InlineDisplay
     * @throws IOException if initialization fails
     */
    public static InlineDisplay withBackend(int height, Backend backend) throws IOException {
        Size size = backend.size();
        PrintWriter out = createPrintWriter(backend);
        return new InlineDisplay(height, size.width(), true, backend, out);
    }

    /**
     * Creates an InlineDisplay with the specified height and fixed width.
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
     * of the backend; callers should avoid invoking {@link #close()} and instead call
     * {@link #release()}. The width remains fixed at the provided value.
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
        render(renderer, height, -1, -1);
    }

    /**
     * Renders widgets to the display area with explicit cursor positioning.
     * The provided consumer receives the area and buffer to render into.
     *
     * @param renderer the rendering function that populates the buffer
     * @param contentHeight the desired height of the content in lines
     * @param cursorX the x position for the cursor, or -1 to use default positioning
     * @param cursorY the y position for the cursor, or -1 to use default positioning
     */
    public void render(BiConsumer<Rect, Buffer> renderer, int contentHeight, int cursorX, int cursorY) {
        ensureInitialized();
        syncWidth();

        // Resize display if content height changed
        resizeDisplay(contentHeight);

        // Only render if we have space
        if (currentHeight > 0) {
            buffer.clear();
            renderer.accept(buffer.area(), buffer);
            redrawDisplayArea(cursorX, cursorY);
        }
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
        syncWidth();

        // Ensure display is allocated to full height
        if (currentHeight != height) {
            resizeDisplay(height);
        }

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
        syncWidth();

        // Ensure display is allocated to full height
        if (currentHeight != height) {
            resizeDisplay(height);
        }

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
     * stays in place at the bottom. The message may contain line breaks,
     * and lines wider than the terminal wrap.
     *
     * @param message the message to print
     */
    public void println(String message) {
        ensureInitialized();
        syncWidth();

        try {
            if (currentHeight > 0) {
                // Move cursor to display line 0
                backend.carriageReturn();
                if (lastCursorY > 0) {
                    backend.moveCursorUp(lastCursorY);
                }

                // Remove the display lines and print the message in their place: the terminal
                // wraps and scrolls it like any other output, however many lines it takes,
                // and the display is redrawn below it.
                backend.deleteLines(currentHeight);
                lastCursorY = 0;
            }
            printLines(message);
        } catch (IOException e) {
            // PrintWriter swallows exceptions, match that behavior
        }

        // Redraw the display area from its new position
        redrawDisplayArea(-1, -1);
        out.flush();
    }

    /**
     * Prints styled text above the display area.
     * Each line of the text is printed on its own line.
     *
     * @param text the styled text to print
     */
    public void println(Text text) {
        if (text.lines().isEmpty()) {
            println("");
            return;
        }

        // Render each line to a temporary buffer for ANSI conversion. A line ends with a
        // style reset, so its background does not spill into the next line when the
        // terminal scrolls.
        syncWidth();
        List<Line> lines = text.lines();
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                message.append('\n');
            }
            Buffer lineBuffer = Buffer.empty(Rect.of(width, 1));
            lineBuffer.setLine(0, 0, lines.get(i));
            message.append(lineBuffer.toAnsiStringTrimmed());
        }
        println(message.toString());
    }

    /**
     * Prints each line of the message followed by a line break. Every line starts at
     * column 0, also on a terminal whose line feed keeps the column (raw output).
     */
    private void printLines(String message) throws IOException {
        for (String line : LINE_BREAK.split(String.valueOf(message), -1)) {
            out.print(line);
            out.print("\n");
            backend.carriageReturn();
        }
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

        try {
            // Move cursor to after the display area and restore cursor visibility
            backend.carriageReturn();
            int toBottom = currentHeight - 1 - lastCursorY;
            if (toBottom > 0) {
                backend.moveCursorDown(toBottom);
            }
            out.print(AnsiStringBuilder.RESET);  // Reset style
            backend.showCursor();
            out.print("\u001b[0 q");  // Reset cursor to default style
            out.println();
            out.flush();
        } catch (IOException e) {
            // PrintWriter swallows exceptions, match that behavior
        }

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
     * <p>
     * For auto-width displays created with {@link #create(int)} or
     * {@link #withBackend(int, Backend)}, this reflects the current terminal width.
     *
     * @return the display width
     */
    public int width() {
        syncWidth();
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

        try {
            // Hide cursor - we render cursor as a styled cell in the buffer instead.
            // This avoids flicker from hide/show cycling during redraws.
            backend.hideCursor();

            out.flush();
        } catch (IOException e) {
            // PrintWriter swallows exceptions, match that behavior
        }

        initialized = true;
        // Note: Initial height allocation happens in first render() call via resizeDisplay()
    }

    private void syncWidth() {
        if (!autoWidth) {
            return;
        }

        try {
            int newWidth = backend.size().width();
            if (newWidth <= 0 || newWidth == width) {
                return;
            }

            int currentBufferHeight = buffer.area().height();
            Buffer resized = Buffer.empty(Rect.of(newWidth, currentBufferHeight));
            int copyWidth = Math.min(width, newWidth);
            for (int y = 0; y < currentBufferHeight; y++) {
                for (int x = 0; x < copyWidth; x++) {
                    resized.set(x, y, buffer.get(x, y));
                }
            }

            width = newWidth;
            buffer = resized;
        } catch (IOException e) {
            // Keep using the last known width if the terminal size cannot be queried.
        }
    }

    private void redrawDisplayArea(int cursorX, int cursorY) {
        // Cursor is hidden since initialization - no need to hide/show here.
        // TextInput renders cursor as a styled cell (reversed) in the buffer.

        if (currentHeight == 0) {
            return;  // Nothing to draw
        }

        try {
            // Move to start of display area (line 0)
            // First, go to start of current line
            backend.carriageReturn();
            // Then move up to line 0 if cursor was left on a different line
            if (lastCursorY > 0) {
                backend.moveCursorUp(lastCursorY);
            }

            // Render each line
            try (AnsiCellWriter cellWriter = new AnsiCellWriter(out::print)) {
                for (int y = 0; y < currentHeight; y++) {
                    if (y > 0) {
                        out.print("\n");
                    }
                    // Clear the line first (from col 0) to remove stale content.
                    // This avoids calling eraseToEndOfLine after rendering, which
                    // would interact badly with "pending wrap" state on some terminals.
                    backend.carriageReturn();
                    backend.eraseToEndOfLine();

                    int lineEnd = findLastContentPosition(y);

                    for (int x = 0; x < lineEnd; x++) {
                        cellWriter.writeCell(buffer.get(x, y));
                    }
                }
            }
            backend.carriageReturn();

            // Position cursor - go back to start of display area
            if (currentHeight > 1) {
                backend.moveCursorUp(currentHeight - 1);
            }

            if (cursorX >= 0 && cursorY >= 0) {
                // Use explicit cursor position
                if (cursorY > 0) {
                    backend.moveCursorDown(cursorY);
                }
                if (cursorX > 0) {
                    backend.moveCursorRight(cursorX);
                }
                lastCursorY = cursorY;
            } else {
                // Default: move cursor to end of first line content (for prompt-style UX)
                int endX = findLastContentPosition(0);
                if (endX > 0) {
                    backend.moveCursorRight(endX);
                }
                lastCursorY = 0;
            }

            // Keep terminal cursor hidden - TextInput renders cursor as a styled cell
            // in the buffer (reversed style), so we don't need the terminal cursor.
            // This eliminates flicker from hide/show cycling.
            out.flush();
        } catch (IOException e) {
            // PrintWriter swallows exceptions, match that behavior
        }
    }

    /**
     * Finds the position after the last non-empty cell on a line,
     * accounting for wide character display widths.
     */
    private int findLastContentPosition(int line) {
        for (int x = width - 1; x >= 0; x--) {
            Cell cell = buffer.get(x, line);
            if (cell.isContinuation()) {
                continue;
            }
            String symbol = cell.symbol();
            if (!symbol.isEmpty() && !symbol.equals(" ")) {
                return x + CharWidth.of(symbol);
            }
        }
        return 0;
    }

    private void clearDisplayArea() {
        try {
            // Move to start of display area (line 0), from the line the cursor was left on
            backend.carriageReturn();
            if (lastCursorY > 0) {
                backend.moveCursorUp(lastCursorY);
            }

            // Clear each line
            for (int y = 0; y < currentHeight; y++) {
                if (y > 0) {
                    out.print("\n");
                    backend.carriageReturn();
                }
                backend.eraseToEndOfLine();
            }

            // Move back up
            if (currentHeight > 1) {
                backend.moveCursorUp(currentHeight - 1);
            }
            lastCursorY = 0;
            out.flush();
        } catch (IOException e) {
            // PrintWriter swallows exceptions, match that behavior
        }
    }

    /**
     * Resizes the display area to the specified height.
     * <p>
     * When growing, inserts new lines at the bottom and moves cursor up.
     * When shrinking, deletes lines from the bottom and moves cursor up.
     * This ensures content above the display is pushed down/up accordingly.
     *
     * @param newHeight the new height in lines
     */
    private void resizeDisplay(int newHeight) {
        if (newHeight < 0) {
            newHeight = 0;
        }

        if (newHeight == currentHeight) {
            return;  // No change needed
        }

        try {
            int delta = newHeight - currentHeight;

            if (delta > 0) {
                // Growing: add lines at bottom
                // Move to bottom of current display, accounting for lastCursorY
                if (currentHeight > 0) {
                    int toBottom = currentHeight - 1 - lastCursorY;
                    if (toBottom > 0) {
                        backend.moveCursorDown(toBottom);
                    }
                    backend.carriageReturn();
                }

                // Add new lines
                for (int i = 0; i < delta; i++) {
                    out.print("\n");
                }

                // Move cursor back to top of display.
                // After newlines, cursor is at newHeight - 1 (when coming from non-zero)
                // or delta lines below start (when from zero).
                int cursorLineFromTop = currentHeight > 0 ? newHeight - 1 : delta;
                if (cursorLineFromTop > 0) {
                    backend.moveCursorUp(cursorLineFromTop);
                }
                backend.carriageReturn();

            } else {
                // Shrinking: delete lines from bottom
                // Move to first line to delete, accounting for lastCursorY
                int toTarget = newHeight - lastCursorY;
                if (toTarget > 0) {
                    backend.moveCursorDown(toTarget);
                } else if (toTarget < 0) {
                    backend.moveCursorUp(-toTarget);
                }
                backend.carriageReturn();

                // Delete lines
                backend.deleteLines(-delta);

                // Move cursor back to top of display
                if (newHeight > 0) {
                    backend.moveCursorUp(newHeight);
                }
                backend.carriageReturn();
            }

            lastCursorY = 0;
            out.flush();
        } catch (IOException e) {
            // PrintWriter swallows exceptions, match that behavior
        }

        currentHeight = newHeight;
        buffer = Buffer.empty(Rect.of(width, newHeight));
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AnsiStringBuilder;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A buffer that stores cells for a rectangular area.
 * Widgets render to a Buffer, and the Terminal calculates diffs between buffers
 * to minimize updates sent to the backend.
 */
public final class Buffer {

    private final Rect area;
    private final Cell[] content;
    private BiConsumer<Style, Rect> styledContentListener;

    private Buffer(Rect area, Cell[] content) {
        this.area = area;
        this.content = content;
    }

    /**
     * Creates an empty buffer filled with empty cells.
     *
     * @param area the area for the buffer
     * @return a new empty buffer
     */
    public static Buffer empty(Rect area) {
        Cell[] content = new Cell[area.area()];
        Arrays.fill(content, Cell.EMPTY);
        return new Buffer(area, content);
    }

    /**
     * Creates a buffer filled with the given cell.
     *
     * @param area the area for the buffer
     * @param cell the cell to fill the buffer with
     * @return a new buffer filled with the given cell
     */
    public static Buffer filled(Rect area, Cell cell) {
        Cell[] content = new Cell[area.area()];
        Arrays.fill(content, cell);
        return new Buffer(area, content);
    }

    /**
     * Creates a buffer from an array of strings.
     * Each string represents a line in the buffer.
     * The buffer area is determined by the maximum line width and the number of lines.
     * Lines shorter than the maximum width are padded with spaces.
     *
     * @param lines the lines to create the buffer from
     * @return a new buffer containing the lines
     */
    public static Buffer withLines(String... lines) {
        if (lines.length == 0) {
            return empty(new Rect(0, 0, 0, 0));
        }

        // Find the maximum display width
        int maxWidth = 0;
        for (String line : lines) {
            if (line != null) {
                int width = CharWidth.of(line);
                maxWidth = Math.max(maxWidth, width);
            }
        }

        int height = lines.length;
        Rect area = new Rect(0, 0, maxWidth, height);
        Buffer buffer = empty(area);

        for (int y = 0; y < height; y++) {
            String line = lines[y];
            if (line != null) {
                buffer.setString(0, y, line, Style.EMPTY);
            }
        }

        return buffer;
    }

    /**
     * Sets a listener to be notified when styled content is written to this buffer.
     * <p>
     * The listener receives the style and area for each span written to the buffer.
     * This allows external systems (like Frame) to track styled areas for
     * effect targeting without Buffer needing to know about registries.
     *
     * @param listener the listener, or null to disable notifications
     */
    public void setStyledContentListener(BiConsumer<Style, Rect> listener) {
        this.styledContentListener = listener;
    }

    /**
     * Returns the area of this buffer.
     *
     * @return the buffer area
     */
    public Rect area() {
        return area;
    }

    /**
     * Returns the width of this buffer.
     *
     * @return the buffer width
     */
    public int width() {
        return area.width();
    }

    /**
     * Returns the height of this buffer.
     *
     * @return the buffer height
     */
    public int height() {
        return area.height();
    }

    /**
     * Gets the cell at the given position.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the cell at the position, or an empty cell if out of bounds
     */
    public Cell get(int x, int y) {
        if (!area.contains(x, y)) {
            return Cell.EMPTY;
        }
        return content[index(x, y)];
    }

    /**
     * Gets the cell at the given position.
     *
     * @param pos the position
     * @return the cell at the position, or an empty cell if out of bounds
     */
    public Cell get(Position pos) {
        return get(pos.x(), pos.y());
    }

    /**
     * Sets the cell at the given position.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param cell the cell to set
     */
    public void set(int x, int y, Cell cell) {
        if (area.contains(x, y)) {
            content[index(x, y)] = cell;
        }
    }

    /**
     * Sets the cell at the given position.
     *
     * @param pos the position
     * @param cell the cell to set
     */
    public void set(Position pos, Cell cell) {
        set(pos.x(), pos.y(), cell);
    }

    /**
     * Sets a string at the given position with the given style.
     * Returns the x position after the last character written.
     * <p>
     * This method patches the style of existing cells rather than replacing them,
     * preserving background colors and other style attributes that were previously set.
     * This matches the behavior of ratatui.rs.
     * <p>
     * Handles grapheme clusters correctly:
     * <ul>
     *   <li>ZWJ sequences: characters after ZWJ are appended to the preceding cell</li>
     *   <li>Regional Indicator pairs: combined into a single 2-wide cell</li>
     *   <li>Skin tone modifiers: appended to preceding cell (zero-width)</li>
     * </ul>
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param string the string to set
     * @param style the style to apply (will be patched onto existing cell style)
     * @return the x position after the last character written
     */
    public int setString(int x, int y, String string, Style style) {
        if (y < area.top() || y >= area.bottom()) {
            return x;
        }

        int col = x;
        boolean appendToLast = false; // true after ZWJ - next char should join
        for (int i = 0; i < string.length(); ) {
            if (col >= area.right()) {
                break;
            }

            int codePoint = string.codePointAt(i);
            int charWidth = CharWidth.of(codePoint);

            if (charWidth == 0) {
                // Zero-width character: append to preceding cell's symbol (grapheme clustering)
                int baseCol = findBaseCell(col, y);
                if (baseCol >= area.left()) {
                    Cell baseCell = get(baseCol, y);
                    String combined = baseCell.symbol() + new String(Character.toChars(codePoint));
                    set(baseCol, y, baseCell.symbol(combined));
                }
                // Check if this is ZWJ - next char should join
                if (codePoint == 0x200D) {
                    appendToLast = true;
                }
                i += Character.charCount(codePoint);
                continue;
            }

            // Check for Regional Indicator pair (flag emoji)
            if (isRegionalIndicator(codePoint)) {
                int nextIdx = i + Character.charCount(codePoint);
                if (nextIdx < string.length()) {
                    int next = string.codePointAt(nextIdx);
                    if (isRegionalIndicator(next)) {
                        // Combine both RIs into a single 2-wide cell
                        String flag = new String(Character.toChars(codePoint)) +
                                      new String(Character.toChars(next));

                        if (col + 1 >= area.right()) {
                            // No room for 2-wide flag, replace with space
                            if (col >= area.left()) {
                                Cell existing = get(col, y);
                                set(col, y, existing.patchStyle(style).symbol(" "));
                            }
                            col++;
                        } else if (col >= area.left()) {
                            Cell current = get(col, y);
                            if (current.isContinuation() && col > area.left()) {
                                set(col - 1, y, get(col - 1, y).symbol(" "));
                            }
                            Cell existing = get(col, y);
                            set(col, y, existing.patchStyle(style).symbol(flag));
                            set(col + 1, y, Cell.CONTINUATION);
                            col += 2;
                        }
                        i = nextIdx + Character.charCount(next);
                        appendToLast = false;
                        continue;
                    }
                }
            }

            // This character follows a ZWJ - append to preceding cell
            if (appendToLast) {
                int baseCol = findBaseCell(col, y);
                if (baseCol >= area.left()) {
                    Cell baseCell = get(baseCol, y);
                    String combined = baseCell.symbol() + new String(Character.toChars(codePoint));
                    set(baseCol, y, baseCell.symbol(combined));
                    appendToLast = false;
                    i += Character.charCount(codePoint);
                    continue;
                }
            }
            appendToLast = false;

            String symbol = new String(Character.toChars(codePoint));

            if (charWidth == 2 && col + 1 >= area.right()) {
                // Wide char at rightmost column: no room for continuation, replace with space
                if (col >= area.left()) {
                    Cell existing = get(col, y);
                    set(col, y, existing.patchStyle(style).symbol(" "));
                }
                col++;
                i += Character.charCount(codePoint);
                continue;
            }

            if (col >= area.left()) {
                // When overwriting a continuation cell, clear the preceding wide char
                Cell current = get(col, y);
                if (current.isContinuation() && col > area.left()) {
                    set(col - 1, y, get(col - 1, y).symbol(" "));
                }

                // If this is a wide char, check if the next cell is also a continuation of something
                if (charWidth == 2 && col + 1 < area.right()) {
                    Cell next = get(col + 1, y);
                    if (next.isContinuation()) {
                        // The cell after our continuation was itself a continuation - clear its owner
                        // (This handles overwriting in the middle of an existing wide char)
                    }
                }

                Cell existing = get(col, y);
                Cell newCell = existing.patchStyle(style).symbol(symbol);
                set(col, y, newCell);

                // Place continuation cell for wide characters
                if (charWidth == 2) {
                    set(col + 1, y, Cell.CONTINUATION);
                }
            }

            col += charWidth;
            i += Character.charCount(codePoint);
        }

        return col;
    }

    /**
     * Finds the base cell (non-continuation) for a given column.
     * Looks backward from col-1 to find the first non-continuation cell.
     * Returns -1 if no base cell found within the area.
     */
    private int findBaseCell(int col, int y) {
        int searchCol = col - 1;
        while (searchCol >= area.left()) {
            Cell cell = get(searchCol, y);
            if (!cell.isContinuation()) {
                return searchCol;
            }
            searchCol--;
        }
        return -1;
    }

    /**
     * Returns true if the code point is a Regional Indicator symbol (U+1F1E6-U+1F1FF).
     */
    private static boolean isRegionalIndicator(int codePoint) {
        return codePoint >= 0x1F1E6 && codePoint <= 0x1F1FF;
    }

    /**
     * Sets a span at the given position.
     * Returns the x position after the last character written.
     * <p>
     * If a styled content listener is set, it will be notified with the
     * span's style and area.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param span the span to set
     * @return the x position after the last character written
     */
    public int setSpan(int x, int y, Span span) {
        int startX = x;
        int endX = setString(x, y, span.content(), span.style());

        // Notify listener of styled content
        if (styledContentListener != null) {
            int width = endX - startX;
            if (width > 0) {
                Rect spanArea = new Rect(startX, y, width, 1);
                styledContentListener.accept(span.style(), spanArea);
            }
        }

        return endX;
    }

    /**
     * Sets a line at the given position.
     * Returns the x position after the last character written.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param line the line to set
     * @return the x position after the last character written
     */
    public int setLine(int x, int y, Line line) {
        int col = x;
        List<Span> spans = line.spans();
        for (int i = 0; i < spans.size(); i++) {
            col = setSpan(col, y, spans.get(i));
        }
        return col;
    }

    /**
     * Sets the style for all cells in the given area.
     *
     * @param area the area to set the style for
     * @param style the style to apply
     */
    public void setStyle(Rect area, Style style) {
        Rect intersection = this.area.intersection(area);
        if (intersection.isEmpty()) {
            return;
        }

        for (int y = intersection.top(); y < intersection.bottom(); y++) {
            for (int x = intersection.left(); x < intersection.right(); x++) {
                Cell cell = get(x, y);
                set(x, y, cell.patchStyle(style));
            }
        }
    }

    /**
     * Fills the given area with the specified cell.
     *
     * @param area the area to fill
     * @param cell the cell to fill with
     */
    public void fill(Rect area, Cell cell) {
        Rect intersection = this.area.intersection(area);
        if (intersection.isEmpty()) {
            return;
        }

        for (int y = intersection.top(); y < intersection.bottom(); y++) {
            for (int x = intersection.left(); x < intersection.right(); x++) {
                set(x, y, cell);
            }
        }
    }

    /**
     * Clears the buffer, resetting all cells to empty.
     */
    public void clear() {
        Arrays.fill(content, Cell.EMPTY);
    }

    /**
     * Resets the buffer to empty cells within the given area.
     *
     * @param area the area to clear
     */
    public void clear(Rect area) {
        fill(area, Cell.EMPTY);
    }

    /**
     * Merges another buffer into this one at the specified position.
     *
     * @param other the buffer to merge
     * @param offsetX the x offset for merging
     * @param offsetY the y offset for merging
     */
    public void merge(Buffer other, int offsetX, int offsetY) {
        for (int y = 0; y < other.height(); y++) {
            for (int x = 0; x < other.width(); x++) {
                int destX = offsetX + x;
                int destY = offsetY + y;
                if (area.contains(destX, destY)) {
                    set(destX, destY, other.get(x + other.area.x(), y + other.area.y()));
                }
            }
        }
    }

    /**
     * Creates a deep copy of this buffer.
     *
     * @return a new buffer with the same content
     */
    public Buffer copy() {
        Cell[] contentCopy = Arrays.copyOf(content, content.length);
        return new Buffer(area, contentCopy);
    }

    /**
     * Calculates the differences between this buffer and another.
     * Returns a list of cell updates needed to transform this buffer into the other.
     *
     * @param other the buffer to compare with
     * @return a list of cell updates representing the differences
     */
    public List<CellUpdate> diff(Buffer other) {
        List<CellUpdate> updates = new ArrayList<>();

        if (!this.area.equals(other.area)) {
            // If areas differ, return all cells from other as updates
            for (int y = other.area.top(); y < other.area.bottom(); y++) {
                for (int x = other.area.left(); x < other.area.right(); x++) {
                    updates.add(new CellUpdate(x, y, other.get(x, y)));
                }
            }
            return updates;
        }

        for (int i = 0; i < content.length; i++) {
            Cell thisCell = content[i];
            Cell otherCell = other.content[i];
            // Fast path: reference equality means same cell, no update needed
            if (thisCell != otherCell && !thisCell.equals(otherCell)) {
                int x = area.x() + (i % area.width());
                int y = area.y() + (i / area.width());
                updates.add(new CellUpdate(x, y, otherCell));
            }
        }

        return updates;
    }

    /**
     * Renders the buffer content as an ANSI-escaped string.
     * Each row becomes a line of output with embedded ANSI escape codes for styling.
     * The result can be printed directly to stdout without needing the full TUI system.
     *
     * <p>Example usage:
     * <pre>{@code
     * Buffer buffer = Buffer.empty(Rect.of(80, 3));
     * myWidget.render(buffer.area(), buffer);
     * System.out.println(buffer.toAnsiString());
     * }</pre>
     *
     * @return an ANSI string representation of the buffer
     */
    public String toAnsiString() {
        StringBuilder result = new StringBuilder();
        Style lastStyle = null;

        for (int y = area.top(); y < area.bottom(); y++) {
            if (y > area.top()) {
                // Use \r\n to ensure the cursor returns to column 0 on each new line.
                // A bare \n only moves the cursor down without a carriage return when
                // the terminal has OPOST disabled (e.g. the Panama backend's raw mode).
                result.append("\r\n");
            }

            for (int x = area.left(); x < area.right(); x++) {
                Cell cell = get(x, y);
                if (cell.isContinuation()) {
                    continue;
                }

                // Apply style if changed
                if (!cell.style().equals(lastStyle)) {
                    result.append(AnsiStringBuilder.styleToAnsi(cell.style()));
                    lastStyle = cell.style();
                }

                result.append(cell.symbol());
            }
        }

        // Reset at end
        result.append(AnsiStringBuilder.RESET);
        return result.toString();
    }

    /**
     * Renders the buffer content as an ANSI-escaped string using explicit cursor positioning.
     * Each row is preceded by a cursor position escape sequence (CSI row;1H) to ensure
     * correct rendering in terminal recording players like asciinema.
     *
     * @return an ANSI string representation of the buffer with cursor positioning
     */
    public String toAnsiStringWithCursorPositioning() {
        StringBuilder result = new StringBuilder();
        Style lastStyle = null;

        for (int y = area.top(); y < area.bottom(); y++) {
            // Position cursor at start of row (1-based coordinates)
            result.append("\u001b[").append(y - area.top() + 1).append(";1H");

            for (int x = area.left(); x < area.right(); x++) {
                Cell cell = get(x, y);
                if (cell.isContinuation()) {
                    continue;
                }

                // Apply style if changed
                if (!cell.style().equals(lastStyle)) {
                    result.append(AnsiStringBuilder.styleToAnsi(cell.style()));
                    lastStyle = cell.style();
                }

                result.append(cell.symbol());
            }
        }

        // Reset at end
        result.append(AnsiStringBuilder.RESET);
        return result.toString();
    }

    /**
     * Renders the buffer content as an ANSI-escaped string, trimming trailing whitespace
     * from each line. This produces cleaner output when the buffer contains significant
     * empty space on the right side.
     *
     * @return an ANSI string representation of the buffer with trailing spaces removed
     */
    public String toAnsiStringTrimmed() {
        StringBuilder result = new StringBuilder();
        Style lastStyle = null;

        for (int y = area.top(); y < area.bottom(); y++) {
            if (y > area.top()) {
                // Use \r\n to ensure the cursor returns to column 0 on each new line.
                // A bare \n only moves the cursor down without a carriage return when
                // the terminal has OPOST disabled (e.g. the Panama backend's raw mode).
                result.append("\r\n");
            }

            // Find the last non-empty cell in this row (skip continuation cells)
            int lastNonEmpty = area.left() - 1;
            for (int x = area.right() - 1; x >= area.left(); x--) {
                Cell cell = get(x, y);
                if (cell.isContinuation()) {
                    continue;
                }
                if (!cell.symbol().equals(" ") || !cell.style().equals(Style.EMPTY)) {
                    lastNonEmpty = x;
                    break;
                }
            }

            // Render up to the last non-empty cell
            for (int x = area.left(); x <= lastNonEmpty; x++) {
                Cell cell = get(x, y);
                if (cell.isContinuation()) {
                    continue;
                }

                // Apply style if changed
                if (!cell.style().equals(lastStyle)) {
                    result.append(AnsiStringBuilder.styleToAnsi(cell.style()));
                    lastStyle = cell.style();
                }

                result.append(cell.symbol());
            }
        }

        // Reset at end
        result.append(AnsiStringBuilder.RESET);
        return result.toString();
    }

    private int index(int x, int y) {
        return (y - area.y()) * area.width() + (x - area.x());
    }
 
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Buffer)) {
            return false;
        }
        Buffer buffer = (Buffer) o;
        if (!area.equals(buffer.area)) {
            return false;
        }
        return Arrays.equals(content, buffer.content);
    }

    @Override
    public int hashCode() {
        int result = area.hashCode();
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Buffer[area=%s, width=%d, height=%d]", area, width(), height());
    }
}

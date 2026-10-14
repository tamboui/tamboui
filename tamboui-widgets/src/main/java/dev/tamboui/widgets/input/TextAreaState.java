/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.tamboui.style.Overflow;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.GraphemeClusters;

/**
 * State for a TextArea widget, tracking multi-line text, cursor position, and scroll offset.
 */
public final class TextAreaState {

    private final List<StringBuilder> lines;
    private int cursorRow;
    private int cursorCol;
    private int scrollRow;
    private int scrollCol;
    private int lastRenderedWidth;

    /** Bumped on every text mutation; used to invalidate the cached display rows. */
    private int textVersion;

    // Cached result of the most recent computeDisplayRows() call.
    private List<DisplayRow> cachedRows;
    private int cachedWidth;
    private Overflow cachedOverflow;
    private int cachedVersion = -1;

    /** Creates a new empty text area state. */
    public TextAreaState() {
        this.lines = new ArrayList<>();
        this.lines.add(new StringBuilder());
        this.cursorRow = 0;
        this.cursorCol = 0;
        this.scrollRow = 0;
        this.scrollCol = 0;
    }

    /**
     * Creates a new text area state with the given initial text.
     *
     * @param initialText the initial text content
     */
    public TextAreaState(String initialText) {
        this();
        setText(initialText);
    }

    // --- Text Access ---

    /**
     * Returns the full text content.
     *
     * @return the text
     */
    public String text() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    /**
     * Returns the number of lines.
     *
     * @return the line count
     */
    public int lineCount() {
        return lines.size();
    }

    /**
     * Returns the text of the line at the given row.
     *
     * @param row the row index
     * @return the line text, or empty string if out of range
     */
    public String getLine(int row) {
        if (row >= 0 && row < lines.size()) {
            return lines.get(row).toString();
        }
        return "";
    }

    // --- Cursor Access ---

    /**
     * Returns the cursor row.
     *
     * @return the cursor row index
     */
    public int cursorRow() {
        return cursorRow;
    }

    /**
     * Returns the cursor column.
     *
     * @return the cursor column index
     */
    public int cursorCol() {
        return cursorCol;
    }

    /**
     * Returns the vertical scroll offset.
     *
     * @return the scroll row
     */
    public int scrollRow() {
        return scrollRow;
    }

    /**
     * Returns the horizontal scroll offset.
     *
     * @return the scroll column
     */
    public int scrollCol() {
        return scrollCol;
    }

    /**
     * Returns the text-content width, in display columns, that the most recent render used
     * (that is, the area width minus any block border and line-number gutter).
     * <p>
     * This is the single source of truth for callers that need the wrap width outside of a
     * render pass, such as {@code Up}/{@code Down} key handling. It is {@code 0} until the
     * widget has been rendered at least once.
     *
     * @return the last rendered text-content width, or {@code 0} if never rendered
     */
    public int lastRenderedWidth() {
        return lastRenderedWidth;
    }

    /** Records the text-content width used by a render pass. Called by {@link TextArea}. */
    void lastRenderedWidth(int width) {
        this.lastRenderedWidth = width;
    }

    // --- Text Modification ---

    /**
     * Inserts a character at the cursor position.
     *
     * @param c the character to insert
     */
    public void insert(char c) {
        textVersion++;
        if (c == '\n') {
            insertNewline();
        } else {
            lines.get(cursorRow).insert(cursorCol, c);
            cursorCol++;
        }
    }

    /**
     * Inserts a string at the cursor position.
     *
     * @param s the string to insert
     */
    public void insert(String s) {
        for (char c : s.toCharArray()) {
            insert(c);
        }
    }

    private void insertNewline() {
        StringBuilder currentLine = lines.get(cursorRow);
        String afterCursor = currentLine.substring(cursorCol);
        currentLine.setLength(cursorCol);
        cursorRow++;
        cursorCol = 0;
        lines.add(cursorRow, new StringBuilder(afterCursor));
    }

    /** Deletes the grapheme cluster before the cursor. */
    public void deleteBackward() {
        textVersion++;
        if (cursorCol > 0) {
            StringBuilder line = lines.get(cursorRow);
            int start = GraphemeClusters.clusterStart(line, cursorCol);
            line.delete(start, cursorCol);
            cursorCol = start;
        } else if (cursorRow > 0) {
            // Merge with previous line
            StringBuilder prevLine = lines.get(cursorRow - 1);
            cursorCol = prevLine.length();
            prevLine.append(lines.get(cursorRow));
            lines.remove(cursorRow);
            cursorRow--;
        }
    }

    /** Deletes the grapheme cluster after the cursor. */
    public void deleteForward() {
        textVersion++;
        StringBuilder currentLine = lines.get(cursorRow);
        if (cursorCol < currentLine.length()) {
            int end = GraphemeClusters.clusterEnd(currentLine, cursorCol);
            currentLine.delete(cursorCol, end);
        } else if (cursorRow < lines.size() - 1) {
            // Merge with next line
            currentLine.append(lines.get(cursorRow + 1));
            lines.remove(cursorRow + 1);
        }
    }

    // --- Cursor Movement ---

    /** Moves the cursor one grapheme cluster to the left. */
    public void moveCursorLeft() {
        if (cursorCol > 0) {
            cursorCol = GraphemeClusters.clusterStart(lines.get(cursorRow), cursorCol);
        } else if (cursorRow > 0) {
            cursorRow--;
            cursorCol = lines.get(cursorRow).length();
        }
    }

    /** Moves the cursor one grapheme cluster to the right. */
    public void moveCursorRight() {
        StringBuilder currentLine = lines.get(cursorRow);
        if (cursorCol < currentLine.length()) {
            cursorCol = GraphemeClusters.clusterEnd(currentLine, cursorCol);
        } else if (cursorRow < lines.size() - 1) {
            cursorRow++;
            cursorCol = 0;
        }
    }

    /**
     * Moves the cursor one logical row up, keeping its screen column (assumes {@link Overflow#CLIP},
     * no wrapping).
     */
    public void moveCursorUp() {
        moveCursorUpClip();
    }

    /**
     * Moves the cursor up one row.
     * <p>
     * With {@code WRAP_WORD}/{@code WRAP_CHARACTER}, moves to the previous visual (wrapped)
     * row, which may be a wrapped segment of the same logical line or the last segment of the
     * previous logical line. Every other {@link Overflow} value (including {@code null}) is
     * treated as {@link Overflow#CLIP} and behaves exactly like {@link #moveCursorUp()} (one
     * logical row) — see {@link #computeDisplayRows}.
     *
     * @param visibleCols the number of visible columns (used to compute wrapped rows)
     * @param overflow    the overflow mode
     */
    public void moveCursorUp(int visibleCols, Overflow overflow) {
        if (!isWrapping(overflow)) {
            moveCursorUpClip();
            return;
        }

        List<DisplayRow> rows = computeDisplayRows(visibleCols, overflow);
        int index = findCursorDisplayRowIndex(rows, visibleCols);
        if (index <= 0) {
            return;
        }
        moveCursorToDisplayRow(rows, index, index - 1, visibleCols);
    }

    private void moveCursorUpClip() {
        if (cursorRow > 0) {
            moveCursorToLine(cursorRow - 1);
        }
    }

    /**
     * Moves the cursor one logical row down, keeping its screen column (assumes {@link Overflow#CLIP},
     * no wrapping).
     */
    public void moveCursorDown() {
        moveCursorDownClip();
    }

    /**
     * Moves the cursor down one row.
     * <p>
     * With {@code WRAP_WORD}/{@code WRAP_CHARACTER}, moves to the next visual (wrapped) row,
     * which may be a wrapped segment of the same logical line or the first segment of the next
     * logical line. Every other {@link Overflow} value (including {@code null}) is treated as
     * {@link Overflow#CLIP} and behaves exactly like {@link #moveCursorDown()} (one logical
     * row) — see {@link #computeDisplayRows}.
     *
     * @param visibleCols the number of visible columns (used to compute wrapped rows)
     * @param overflow    the overflow mode
     */
    public void moveCursorDown(int visibleCols, Overflow overflow) {
        if (!isWrapping(overflow)) {
            moveCursorDownClip();
            return;
        }

        List<DisplayRow> rows = computeDisplayRows(visibleCols, overflow);
        int index = findCursorDisplayRowIndex(rows, visibleCols);
        if (index < 0 || index >= rows.size() - 1) {
            return;
        }
        moveCursorToDisplayRow(rows, index, index + 1, visibleCols);
    }

    private void moveCursorDownClip() {
        if (cursorRow < lines.size() - 1) {
            moveCursorToLine(cursorRow + 1);
        }
    }

    /**
     * Moves the cursor to logical line {@code targetRow}, keeping it in the same screen column.
     * The column is measured in display width from the visible viewport (or the line start when
     * not horizontally scrolled) rather than reused as a char offset. On a line with emoji or CJK
     * characters the same offset can land inside a grapheme, where the next keystroke would split it.
     */
    private void moveCursorToLine(int targetRow) {
        StringBuilder line = lines.get(cursorRow);
        StringBuilder target = lines.get(targetRow);
        int start = 0;
        int column;
        if (scrollCol > 0 && cursorCol >= scrollCol && scrollCol <= line.length()) {
            // The caret is drawn relative to scrollCol, not the start of the logical line.
            column = CharWidth.of(line.substring(scrollCol, cursorCol));
            start = Math.min(scrollCol, target.length());
            // The same char offset can fall inside a grapheme on another line.
            if (start < target.length()) {
                int boundary = 0;
                while (boundary < start) {
                    boundary = GraphemeClusters.clusterEnd(target, boundary);
                }
                start = boundary;
            }
        } else {
            column = CharWidth.of(line.substring(0, Math.min(cursorCol, line.length())));
        }
        cursorRow = targetRow;
        cursorCol = offsetAtColumn(target, start, target.length(), column);
    }

    /**
     * Returns the offset of the last grapheme cluster boundary in {@code line} between {@code start}
     * and {@code end} whose display width from {@code start} does not pass {@code column}.
     */
    private static int offsetAtColumn(StringBuilder line, int start, int end, int column) {
        int offset = start;
        int width = 0;
        while (offset < end) {
            int next = GraphemeClusters.clusterEnd(line, offset);
            int clusterWidth = CharWidth.of(line.substring(offset, next));
            if (width + clusterWidth > column) {
                break;
            }
            width += clusterWidth;
            offset = next;
        }
        return offset;
    }

    /**
     * Moves the cursor from display row {@code fromIndex} (where it is drawn) to display row
     * {@code toIndex}, keeping it in the same screen column.
     */
    private void moveCursorToDisplayRow(List<DisplayRow> rows, int fromIndex, int toIndex, int visibleCols) {
        DisplayRow from = rows.get(fromIndex);
        String fromLine = getLine(from.logicalRow());
        // Same column math as TextArea.renderWithCursor, so the caret moves from where it is seen.
        int fromCol = Math.max(from.startCol(), Math.min(cursorCol, fromLine.length()));
        int column = CharWidth.of(fromLine.substring(from.startCol(), fromCol));

        DisplayRow target = rows.get(toIndex);
        StringBuilder targetLine = lines.get(target.logicalRow());
        int offset = offsetAtColumn(targetLine, target.startCol(), target.endCol(), column);
        cursorRow = target.logicalRow();
        cursorCol = offset;

        // The end of a row can be drawn on the next one (see findCursorDisplayRowIndex); step
        // back a cluster so the caret stays on the row it moved to.
        if (offset > target.startCol() && findCursorDisplayRowIndex(rows, visibleCols) != toIndex) {
            cursorCol = GraphemeClusters.clusterStart(targetLine, offset);
        }
    }

    /** Moves the cursor to the start of the current line. */
    public void moveCursorToLineStart() {
        cursorCol = 0;
    }

    /** Moves the cursor to the end of the current line. */
    public void moveCursorToLineEnd() {
        cursorCol = lines.get(cursorRow).length();
    }

    /** Moves the cursor to the very beginning of the text. */
    public void moveCursorToStart() {
        cursorRow = 0;
        cursorCol = 0;
    }

    /** Moves the cursor to the very end of the text. */
    public void moveCursorToEnd() {
        cursorRow = lines.size() - 1;
        cursorCol = lines.get(cursorRow).length();
    }

    // --- Wrapping ---

    /**
     * One visually-wrapped row of a logical line: the character-offset range
     * {@code [startCol, endCol)} of that line rendered on a single screen row.
     */
    public static final class DisplayRow {
        private final int logicalRow;
        private final int startCol;
        private final int endCol;

        DisplayRow(int logicalRow, int startCol, int endCol) {
            this.logicalRow = logicalRow;
            this.startCol = startCol;
            this.endCol = endCol;
        }

        /**
         * Returns the logical (newline-delimited) line index this row belongs to.
         *
         * @return the logical line index
         */
        public int logicalRow() {
            return logicalRow;
        }

        /**
         * Returns where this row starts within its logical line.
         *
         * @return the inclusive char offset of the row start
         */
        public int startCol() {
            return startCol;
        }

        /**
         * Returns where this row ends within its logical line.
         *
         * @return the exclusive char offset of the row end
         */
        public int endCol() {
            return endCol;
        }
    }

    /**
     * Returns whether {@code overflow} makes a text area wrap.
     * <p>
     * Only {@link Overflow#WRAP_WORD} and {@link Overflow#WRAP_CHARACTER} wrap. A text area is
     * an editor, so the truncating modes ({@code ELLIPSIS}, {@code ELLIPSIS_START},
     * {@code ELLIPSIS_MIDDLE}) would hide text the caret can still reach; they are treated as
     * {@link Overflow#CLIP} (horizontal scroll) instead, as is {@code null}.
     *
     * @param overflow the overflow mode, may be null
     * @return true if the mode wraps
     */
    static boolean isWrapping(Overflow overflow) {
        return overflow == Overflow.WRAP_WORD || overflow == Overflow.WRAP_CHARACTER;
    }

    /**
     * Computes the visual rows produced by wrapping every logical line to {@code visibleWidth}.
     * <p>
     * Only {@link Overflow#WRAP_WORD} and {@link Overflow#WRAP_CHARACTER} wrap; every other
     * value (and a non-positive width) maps each logical line to exactly one display row,
     * matching the unwrapped {@link Overflow#CLIP} behavior. See {@link #isWrapping}.
     * <p>
     * When wrapping, a line whose last row fills the whole width (or ends in whitespace that a
     * word break consumed) is followed by an empty row at the end of the line, so a caret placed
     * there has a cell to be drawn in.
     * <p>
     * The result is cached and reused until the text, the width, or the mode changes, so
     * calling this several times per frame is cheap.
     *
     * @param visibleWidth the width available for text, in display columns
     * @param overflow     the overflow mode
     * @return an unmodifiable list of the display rows, in document order
     */
    public List<DisplayRow> computeDisplayRows(int visibleWidth, Overflow overflow) {
        if (cachedVersion == textVersion && cachedWidth == visibleWidth && cachedOverflow == overflow) {
            return cachedRows;
        }

        List<DisplayRow> result = new ArrayList<>();
        boolean noWrap = !isWrapping(overflow) || visibleWidth <= 0;
        for (int row = 0; row < lines.size(); row++) {
            StringBuilder line = lines.get(row);
            if (noWrap) {
                result.add(new DisplayRow(row, 0, line.length()));
                continue;
            }
            if (line.length() == 0) {
                result.add(new DisplayRow(row, 0, 0));
                continue;
            }
            List<int[]> segments = overflow == Overflow.WRAP_WORD
                ? wrapLineByWord(line, visibleWidth)
                : wrapLineByCharacter(line, visibleWidth);
            for (int[] segment : segments) {
                result.add(new DisplayRow(row, segment[0], segment[1]));
            }
            // A caret at the end of the line needs a cell to be drawn in. When the last row
            // already fills the width, or a word break consumed trailing whitespace, the end of
            // the line gets an empty row of its own.
            int[] last = segments.get(segments.size() - 1);
            if (last[1] < line.length() || CharWidth.of(line.substring(last[0], last[1])) >= visibleWidth) {
                result.add(new DisplayRow(row, line.length(), line.length()));
            }
        }

        cachedRows = Collections.unmodifiableList(result);
        cachedWidth = visibleWidth;
        cachedOverflow = overflow;
        cachedVersion = textVersion;
        return cachedRows;
    }

    // Both wrap modes step by grapheme cluster, measured as a whole, so a ZWJ sequence or a
    // flag is never split across rows and is as wide as CharWidth.of(String) says it is.

    private static List<int[]> wrapLineByCharacter(StringBuilder line, int maxWidth) {
        List<int[]> segments = new ArrayList<>();
        int start = 0;
        int width = 0;
        int i = 0;
        int len = line.length();
        while (i < len) {
            int clusterEnd = GraphemeClusters.clusterEnd(line, i);
            int clusterWidth = CharWidth.of(line.substring(i, clusterEnd));

            if (width + clusterWidth > maxWidth) {
                if (width == 0) {
                    // Single cluster wider than maxWidth: give it its own row.
                    segments.add(new int[] {start, clusterEnd});
                    i = clusterEnd;
                    start = i;
                    width = 0;
                    continue;
                }
                segments.add(new int[] {start, i});
                start = i;
                width = 0;
                continue;
            }

            width += clusterWidth;
            i = clusterEnd;
        }
        // Nothing is left over when the line ended on a cluster that got a row of its own.
        if (start < len) {
            segments.add(new int[] {start, len});
        }
        return segments;
    }

    private static List<int[]> wrapLineByWord(StringBuilder line, int maxWidth) {
        List<Integer> clusterOffsets = new ArrayList<>();
        List<Integer> clusterWidths = new ArrayList<>();
        int i = 0;
        while (i < line.length()) {
            int clusterEnd = GraphemeClusters.clusterEnd(line, i);
            clusterOffsets.add(i);
            clusterWidths.add(CharWidth.of(line.substring(i, clusterEnd)));
            i = clusterEnd;
        }
        clusterOffsets.add(line.length());
        int clusterCount = clusterWidths.size();

        List<int[]> segments = new ArrayList<>();
        int pos = 0;
        while (pos < clusterCount) {
            int lineEnd = findNextWordBreakByWidth(line, clusterOffsets, clusterWidths, pos, clusterCount, maxWidth);
            segments.add(new int[] {clusterOffsets.get(pos), clusterOffsets.get(lineEnd)});

            // Skip whitespace consumed by the break so the next row never starts with it.
            int nextPos = lineEnd;
            while (nextPos < clusterCount
                && Character.isWhitespace(line.codePointAt(clusterOffsets.get(nextPos)))) {
                nextPos++;
            }
            pos = nextPos;
        }
        return segments;
    }

    private static int findNextWordBreakByWidth(StringBuilder text, List<Integer> clusterOffsets,
                                                  List<Integer> clusterWidths, int startPos, int clusterCount,
                                                  int maxWidth) {
        int width = 0;
        int maxEnd = startPos;
        while (maxEnd < clusterCount) {
            int clusterWidth = clusterWidths.get(maxEnd);
            if (width + clusterWidth > maxWidth) {
                break;
            }
            width += clusterWidth;
            maxEnd++;
        }

        if (maxEnd == startPos) {
            // Single cluster wider than maxWidth: force progress.
            maxEnd = Math.min(startPos + 1, clusterCount);
        }

        if (maxEnd >= clusterCount) {
            return clusterCount;
        }

        // The fit already ends exactly at a word boundary; no need to backtrack.
        if (Character.isWhitespace(text.codePointAt(clusterOffsets.get(maxEnd)))) {
            return maxEnd;
        }

        for (int idx = maxEnd - 1; idx > startPos; idx--) {
            int codePoint = text.codePointAt(clusterOffsets.get(idx));
            if (Character.isWhitespace(codePoint)) {
                return idx;
            }
        }

        for (int idx = maxEnd - 1; idx > startPos; idx--) {
            int codePoint = text.codePointAt(clusterOffsets.get(idx));
            if (codePoint == '-' || codePoint == '/' || codePoint == '\\') {
                return idx + 1;
            }
        }

        return maxEnd;
    }

    // --- Scrolling ---

    /**
     * Adjusts scroll offsets to keep the cursor visible, assuming {@link Overflow#CLIP}
     * (no wrapping, horizontal scroll instead).
     *
     * @param visibleRows the number of visible rows
     * @param visibleCols the number of visible columns
     */
    public void ensureCursorVisible(int visibleRows, int visibleCols) {
        ensureCursorVisibleClip(visibleRows, visibleCols);
    }

    /**
     * Adjusts scroll offsets to keep the cursor visible.
     * <p>
     * With {@code WRAP_WORD}/{@code WRAP_CHARACTER}, {@code scrollRow} is treated as an index
     * into the wrapped display rows (see {@link #computeDisplayRows}) and {@code scrollCol} is
     * always {@code 0}, since a wrapped row always fits within {@code visibleCols}. Every other
     * {@link Overflow} value (including {@code null}) is treated as {@link Overflow#CLIP} and
     * behaves exactly like {@link #ensureCursorVisible(int, int)}.
     *
     * @param visibleRows the number of visible rows
     * @param visibleCols the number of visible columns
     * @param overflow    the overflow mode
     */
    public void ensureCursorVisible(int visibleRows, int visibleCols, Overflow overflow) {
        if (!isWrapping(overflow)) {
            ensureCursorVisibleClip(visibleRows, visibleCols);
            return;
        }

        List<DisplayRow> rows = computeDisplayRows(visibleCols, overflow);
        int cursorDisplayIndex = findCursorDisplayRowIndex(rows, visibleCols);

        if (cursorDisplayIndex < scrollRow) {
            scrollRow = cursorDisplayIndex;
        } else if (cursorDisplayIndex >= scrollRow + visibleRows) {
            scrollRow = cursorDisplayIndex - visibleRows + 1;
        }
        scrollCol = 0;
    }

    private void ensureCursorVisibleClip(int visibleRows, int visibleCols) {
        // Vertical scrolling
        if (cursorRow < scrollRow) {
            scrollRow = cursorRow;
        } else if (cursorRow >= scrollRow + visibleRows) {
            scrollRow = cursorRow - visibleRows + 1;
        }

        // Horizontal scrolling (display-width aware: cursorCol is a char offset)
        String line = getLine(cursorRow);
        if (cursorCol <= scrollCol) {
            scrollCol = cursorCol;
        } else {
            int from = Math.min(scrollCol, line.length());
            int to = Math.min(cursorCol, line.length());
            int displayWidth = CharWidth.of(line.substring(from, to));
            if (displayWidth >= visibleCols) {
                scrollCol = findScrollColForCursor(line, cursorCol, visibleCols);
            }
        }
    }

    /**
     * Finds the index within {@code rows} (as returned by {@link #computeDisplayRows}) of the
     * display row containing the given logical position.
     * <p>
     * A position on whitespace consumed by a word-wrap break belongs to no row and resolves to
     * the row before the break; see {@link #findCursorDisplayRowIndex} for where such a
     * position is drawn.
     *
     * @param rows       the display rows to search
     * @param logicalRow the logical (newline-delimited) line index
     * @param col        the char offset within that logical line
     * @return the index of the containing display row
     */
    public static int findDisplayRowIndex(List<DisplayRow> rows, int logicalRow, int col) {
        int best = 0;
        for (int i = 0; i < rows.size(); i++) {
            DisplayRow row = rows.get(i);
            if (row.logicalRow() == logicalRow && row.startCol() <= col) {
                best = i;
            } else if (row.logicalRow() > logicalRow) {
                break;
            }
        }
        return best;
    }

    /**
     * Like {@link #findDisplayRowIndex} for the cursor, but resolves the row it is actually
     * <em>drawn</em> on.
     * <p>
     * A word-wrap break consumes the whitespace between two rows, so {@code "one two three"} at
     * width 7 yields {@code [0,7)} and {@code [8,13)} — leaving char offset 7 (the consumed
     * space, reachable with Left/Right) inside no display row. The caret at the break itself
     * (offset 7) is drawn right after the row's text when the row leaves room for it; here
     * {@code "one two"} fills the width, so it is drawn at the start of the following row
     * instead, as is any offset further into the consumed whitespace.
     * <p>
     * Up/Down start from this row too, so the caret always moves from where it is seen.
     *
     * @param rows         the display rows, as returned by {@link #computeDisplayRows}
     * @param visibleWidth the width the rows were wrapped to
     * @return the index of the display row the cursor is drawn on
     */
    int findCursorDisplayRowIndex(List<DisplayRow> rows, int visibleWidth) {
        int index = findDisplayRowIndex(rows, cursorRow, cursorCol);
        DisplayRow row = rows.get(index);
        if (index + 1 >= rows.size() || rows.get(index + 1).logicalRow() != cursorRow
            || cursorCol < row.endCol()) {
            return index;
        }
        if (cursorCol == row.endCol()
            && CharWidth.of(getLine(cursorRow).substring(row.startCol(), row.endCol())) < visibleWidth) {
            return index;
        }
        return index + 1;
    }

    private static int findScrollColForCursor(String line, int cursorCol, int visibleCols) {
        int end = Math.min(cursorCol, line.length());
        // prefixWidth[i] = display width of line.substring(0, i), stepping by grapheme
        // clusters the same way CharWidth.of(String) does. For a multi-char cluster, the
        // width is recorded on the offset past the cluster; intermediate offsets carry the
        // running width as it stood before the cluster.
        int[] prefixWidth = new int[end + 1];
        int width = 0;
        int i = 0;
        while (i < end) {
            int clusterEnd = GraphemeClusters.clusterEnd(line, i);
            int clusterChars = clusterEnd - i;
            int clusterWidth = CharWidth.of(line.substring(i, clusterEnd));

            int newWidth = width + clusterWidth;
            // Carry the previous running width across intermediate offsets, then jump
            // to the new running width at the offset past the cluster.
            for (int j = 1; j < clusterChars; j++) {
                if (i + j <= end) {
                    prefixWidth[i + j] = width;
                }
            }
            if (i + clusterChars <= end) {
                prefixWidth[i + clusterChars] = newWidth;
            }
            width = newWidth;
            i += clusterChars;
        }

        int totalWidth = prefixWidth[end];
        for (int col = end - 1; col >= 0; col--) {
            if (totalWidth - prefixWidth[col] >= visibleCols) {
                return col + 1;
            }
        }
        return 0;
    }

    /**
     * Scrolls up by the given amount of rows.
     *
     * @param amount the number of rows to scroll up
     */
    public void scrollUp(int amount) {
        scrollRow = Math.max(0, scrollRow - amount);
    }

    /**
     * Scrolls down by the given amount of rows.
     *
     * @param amount      the number of rows to scroll down
     * @param visibleRows the number of visible rows
     */
    public void scrollDown(int amount, int visibleRows) {
        int maxScroll = Math.max(0, lines.size() - visibleRows);
        scrollRow = Math.min(maxScroll, scrollRow + amount);
    }

    /**
     * Scrolls down by the given amount of rows.
     * <p>
     * With {@code WRAP_WORD}/{@code WRAP_CHARACTER}, {@code scrollRow} indexes the wrapped
     * display rows (see {@link #computeDisplayRows}), so scrolling is clamped to their count
     * rather than to the number of logical lines. Every other {@link Overflow} value (including
     * {@code null}) is treated as {@link Overflow#CLIP} and behaves exactly like
     * {@link #scrollDown(int, int)}.
     *
     * @param amount      the number of rows to scroll down
     * @param visibleRows the number of visible rows
     * @param visibleCols the number of visible columns (used to compute wrapped rows)
     * @param overflow    the overflow mode
     */
    public void scrollDown(int amount, int visibleRows, int visibleCols, Overflow overflow) {
        if (!isWrapping(overflow)) {
            scrollDown(amount, visibleRows);
            return;
        }
        int maxScroll = Math.max(0, computeDisplayRows(visibleCols, overflow).size() - visibleRows);
        scrollRow = Math.min(maxScroll, scrollRow + amount);
    }

    // --- Bulk Operations ---

    /** Clears all text and resets the cursor and scroll positions. */
    public void clear() {
        textVersion++;
        lines.clear();
        lines.add(new StringBuilder());
        cursorRow = 0;
        cursorCol = 0;
        scrollRow = 0;
        scrollCol = 0;
    }

    /**
     * Replaces the text content and moves the cursor to the end.
     *
     * @param newText the new text content
     */
    public void setText(String newText) {
        textVersion++;
        lines.clear();
        if (newText == null || newText.isEmpty()) {
            lines.add(new StringBuilder());
        } else {
            String[] splitLines = newText.split("\n", -1);
            for (String line : splitLines) {
                lines.add(new StringBuilder(line));
            }
        }
        cursorRow = lines.size() - 1;
        cursorCol = lines.get(cursorRow).length();
        scrollRow = 0;
        scrollCol = 0;
    }
}

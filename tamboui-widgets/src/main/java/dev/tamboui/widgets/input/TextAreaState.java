/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import java.util.ArrayList;
import java.util.List;

/**
 * State for a TextArea widget, tracking multi-line text, cursor position, and scroll offset.
 */
public final class TextAreaState {

    private final List<StringBuilder> lines;
    private int cursorRow;
    private int cursorCol;
    private int scrollRow;
    private int scrollCol;

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

    // --- Text Modification ---

    /**
     * Inserts a character at the cursor position.
     *
     * @param c the character to insert
     */
    public void insert(char c) {
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

    /** Deletes the character before the cursor. */
    public void deleteBackward() {
        if (cursorCol > 0) {
            lines.get(cursorRow).deleteCharAt(cursorCol - 1);
            cursorCol--;
        } else if (cursorRow > 0) {
            // Merge with previous line
            StringBuilder prevLine = lines.get(cursorRow - 1);
            cursorCol = prevLine.length();
            prevLine.append(lines.get(cursorRow));
            lines.remove(cursorRow);
            cursorRow--;
        }
    }

    /** Deletes the character after the cursor. */
    public void deleteForward() {
        StringBuilder currentLine = lines.get(cursorRow);
        if (cursorCol < currentLine.length()) {
            currentLine.deleteCharAt(cursorCol);
        } else if (cursorRow < lines.size() - 1) {
            // Merge with next line
            currentLine.append(lines.get(cursorRow + 1));
            lines.remove(cursorRow + 1);
        }
    }

    // --- Cursor Movement ---

    /** Moves the cursor one position to the left. */
    public void moveCursorLeft() {
        if (cursorCol > 0) {
            cursorCol--;
        } else if (cursorRow > 0) {
            cursorRow--;
            cursorCol = lines.get(cursorRow).length();
        }
    }

    /** Moves the cursor one position to the right. */
    public void moveCursorRight() {
        StringBuilder currentLine = lines.get(cursorRow);
        if (cursorCol < currentLine.length()) {
            cursorCol++;
        } else if (cursorRow < lines.size() - 1) {
            cursorRow++;
            cursorCol = 0;
        }
    }

    /** Moves the cursor one row up. */
    public void moveCursorUp() {
        if (cursorRow > 0) {
            cursorRow--;
            cursorCol = Math.min(cursorCol, lines.get(cursorRow).length());
        }
    }

    /** Moves the cursor one row down. */
    public void moveCursorDown() {
        if (cursorRow < lines.size() - 1) {
            cursorRow++;
            cursorCol = Math.min(cursorCol, lines.get(cursorRow).length());
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

    // --- Scrolling ---

    /**
     * Adjusts scroll offsets to keep the cursor visible.
     *
     * @param visibleRows the number of visible rows
     * @param visibleCols the number of visible columns
     */
    public void ensureCursorVisible(int visibleRows, int visibleCols) {
        // Vertical scrolling
        if (cursorRow < scrollRow) {
            scrollRow = cursorRow;
        } else if (cursorRow >= scrollRow + visibleRows) {
            scrollRow = cursorRow - visibleRows + 1;
        }

        // Horizontal scrolling
        if (cursorCol < scrollCol) {
            scrollCol = cursorCol;
        } else if (cursorCol >= scrollCol + visibleCols) {
            scrollCol = cursorCol - visibleCols + 1;
        }
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

    // --- Bulk Operations ---

    /** Clears all text and resets the cursor and scroll positions. */
    public void clear() {
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

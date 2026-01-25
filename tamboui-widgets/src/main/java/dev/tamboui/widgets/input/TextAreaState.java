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

    public TextAreaState() {
        this.lines = new ArrayList<>();
        this.lines.add(new StringBuilder());
        this.cursorRow = 0;
        this.cursorCol = 0;
        this.scrollRow = 0;
        this.scrollCol = 0;
    }

    public TextAreaState(String initialText) {
        this();
        setText(initialText);
    }

    // --- Text Access ---

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

    public int lineCount() {
        return lines.size();
    }

    public String getLine(int row) {
        if (row >= 0 && row < lines.size()) {
            return lines.get(row).toString();
        }
        return "";
    }

    // --- Cursor Access ---

    public int cursorRow() {
        return cursorRow;
    }

    public int cursorCol() {
        return cursorCol;
    }

    public int scrollRow() {
        return scrollRow;
    }

    public int scrollCol() {
        return scrollCol;
    }

    // --- Text Modification ---

    public void insert(char c) {
        if (c == '\n') {
            insertNewline();
        } else {
            lines.get(cursorRow).insert(cursorCol, c);
            cursorCol++;
        }
    }

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

    public void moveCursorLeft() {
        if (cursorCol > 0) {
            cursorCol--;
        } else if (cursorRow > 0) {
            cursorRow--;
            cursorCol = lines.get(cursorRow).length();
        }
    }

    public void moveCursorRight() {
        StringBuilder currentLine = lines.get(cursorRow);
        if (cursorCol < currentLine.length()) {
            cursorCol++;
        } else if (cursorRow < lines.size() - 1) {
            cursorRow++;
            cursorCol = 0;
        }
    }

    public void moveCursorUp() {
        if (cursorRow > 0) {
            cursorRow--;
            cursorCol = Math.min(cursorCol, lines.get(cursorRow).length());
        }
    }

    public void moveCursorDown() {
        if (cursorRow < lines.size() - 1) {
            cursorRow++;
            cursorCol = Math.min(cursorCol, lines.get(cursorRow).length());
        }
    }

    public void moveCursorToLineStart() {
        cursorCol = 0;
    }

    public void moveCursorToLineEnd() {
        cursorCol = lines.get(cursorRow).length();
    }

    public void moveCursorToStart() {
        cursorRow = 0;
        cursorCol = 0;
    }

    public void moveCursorToEnd() {
        cursorRow = lines.size() - 1;
        cursorCol = lines.get(cursorRow).length();
    }

    // --- Scrolling ---

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

    public void scrollUp(int amount) {
        scrollRow = Math.max(0, scrollRow - amount);
    }

    public void scrollDown(int amount, int visibleRows) {
        int maxScroll = Math.max(0, lines.size() - visibleRows);
        scrollRow = Math.min(maxScroll, scrollRow + amount);
    }

    // --- Bulk Operations ---

    public void clear() {
        lines.clear();
        lines.add(new StringBuilder());
        cursorRow = 0;
        cursorCol = 0;
        scrollRow = 0;
        scrollCol = 0;
    }

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

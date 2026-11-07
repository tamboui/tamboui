/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.inline;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.Backend;

/**
 * A backend that plays what is written to it on a small simulated terminal screen, so a test
 * can check the text a user would see, including the lines that scrolled off the top.
 * <p>
 * It understands what {@link InlineDisplay} writes: printable ASCII characters with auto-wrap at
 * the right margin, carriage return, line feed, cursor up/down/left/right, erase to the end of
 * the line, and insert and delete lines. Styles and other escape sequences are ignored.
 */
final class TerminalScreen implements Backend {

    private final int width;
    private final int height;
    private final boolean lineFeedReturnsToColumnZero;
    private final List<String> scrolledOff = new ArrayList<>();
    private final List<char[]> rows = new ArrayList<>();
    private final StringBuilder sequence = new StringBuilder();
    private int row;
    private int column;
    private boolean wrapPending;
    private boolean inEscape;
    private boolean inSequence;

    /**
     * Creates a screen whose line feed also returns the cursor to column 0, as a terminal does when
     * the tty translates "\n" to "\r\n" (the JLine backend in raw mode).
     */
    TerminalScreen(int width, int height) {
        this(width, height, true);
    }

    private TerminalScreen(int width, int height, boolean lineFeedReturnsToColumnZero) {
        this.width = width;
        this.height = height;
        this.lineFeedReturnsToColumnZero = lineFeedReturnsToColumnZero;
        for (int y = 0; y < height; y++) {
            rows.add(blankRow());
        }
    }

    /**
     * Creates a screen whose line feed keeps the column, as with output processing turned off
     * (the Panama backend in raw mode).
     */
    static TerminalScreen withRawOutput(int width, int height) {
        return new TerminalScreen(width, height, false);
    }

    /**
     * Returns the lines that scrolled off the top followed by the rows of the screen, without
     * trailing spaces and without the empty rows at the bottom of the screen.
     */
    List<String> lines() {
        List<String> lines = new ArrayList<>(scrolledOff);
        for (char[] chars : rows) {
            lines.add(trimEnd(new String(chars)));
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }

    @Override
    public void writeRaw(byte[] data) {
        String text = new String(data, StandardCharsets.UTF_8);
        for (int i = 0; i < text.length(); i++) {
            accept(text.charAt(i));
        }
    }

    private void accept(char c) {
        if (inSequence) {
            if (c >= '@' && c <= '~') {
                inSequence = false;
                applySequence(sequence.toString(), c);
            } else {
                sequence.append(c);
            }
        } else if (inEscape) {
            inEscape = false;
            if (c == '[') {
                inSequence = true;
                sequence.setLength(0);
            }
        } else if (c == '\u001b') {
            inEscape = true;
        } else if (c == '\r') {
            column = 0;
            wrapPending = false;
        } else if (c == '\n') {
            lineFeed();
            if (lineFeedReturnsToColumnZero) {
                column = 0;
            }
            wrapPending = false;
        } else if (c >= ' ') {
            print(c);
        }
    }

    private void print(char c) {
        if (wrapPending) {
            column = 0;
            lineFeed();
            wrapPending = false;
        }
        rows.get(row)[column] = c;
        if (column == width - 1) {
            wrapPending = true;
        } else {
            column++;
        }
    }

    private void lineFeed() {
        if (row < height - 1) {
            row++;
            return;
        }
        scrolledOff.add(trimEnd(new String(rows.remove(0))));
        rows.add(blankRow());
    }

    private void applySequence(String parameters, char command) {
        if (!parameters.matches("[0-9]*")) {
            return;
        }
        int n = parameters.isEmpty() ? 1 : Integer.parseInt(parameters);
        switch (command) {
            case 'A':
                row = Math.max(0, row - n);
                break;
            case 'B':
                row = Math.min(height - 1, row + n);
                break;
            case 'C':
                column = Math.min(width - 1, column + n);
                break;
            case 'D':
                column = Math.max(0, column - n);
                break;
            case 'K':
                if (parameters.isEmpty() || n == 0) {
                    Arrays.fill(rows.get(row), column, width, ' ');
                }
                break;
            case 'L':
                for (int i = 0; i < n; i++) {
                    rows.remove(height - 1);
                    rows.add(row, blankRow());
                }
                column = 0;
                break;
            case 'M':
                for (int i = 0; i < n; i++) {
                    rows.remove(row);
                    rows.add(blankRow());
                }
                column = 0;
                break;
            default:
                return;
        }
        wrapPending = false;
    }

    private char[] blankRow() {
        char[] chars = new char[width];
        Arrays.fill(chars, ' ');
        return chars;
    }

    private static String trimEnd(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }

    @Override
    public void draw(DiffResult diff) {
    }

    @Override
    public void flush() {
    }

    @Override
    public void clear() {
    }

    @Override
    public Size size() {
        return new Size(width, height);
    }

    @Override
    public void showCursor() {
    }

    @Override
    public void hideCursor() {
    }

    @Override
    public Position getCursorPosition() {
        return new Position(column, row);
    }

    @Override
    public void setCursorPosition(Position position) {
        column = position.x();
        row = position.y();
        wrapPending = false;
    }

    @Override
    public void enterAlternateScreen() {
    }

    @Override
    public void leaveAlternateScreen() {
    }

    @Override
    public void enableRawMode() {
    }

    @Override
    public void disableRawMode() {
    }

    @Override
    public void onResize(Runnable handler) {
    }

    @Override
    public int read(int timeoutMs) {
        return -2;
    }

    @Override
    public int peek(int timeoutMs) {
        return -2;
    }

    @Override
    public void close() {
    }
}

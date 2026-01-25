/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

/**
 * State for a TextInput widget, tracking the input text and cursor position.
 */
public final class TextInputState {

    private StringBuilder text;
    private int cursorPosition;

    public TextInputState() {
        this.text = new StringBuilder();
        this.cursorPosition = 0;
    }

    public TextInputState(String initialText) {
        this.text = new StringBuilder(initialText);
        this.cursorPosition = initialText.length();
    }

    public String text() {
        return text.toString();
    }

    public int cursorPosition() {
        return cursorPosition;
    }

    public int length() {
        return text.length();
    }

    public void insert(char c) {
        text.insert(cursorPosition, c);
        cursorPosition++;
    }

    public void insert(String s) {
        text.insert(cursorPosition, s);
        cursorPosition += s.length();
    }

    public void deleteBackward() {
        if (cursorPosition > 0) {
            text.deleteCharAt(cursorPosition - 1);
            cursorPosition--;
        }
    }

    public void deleteForward() {
        if (cursorPosition < text.length()) {
            text.deleteCharAt(cursorPosition);
        }
    }

    public void moveCursorLeft() {
        if (cursorPosition > 0) {
            cursorPosition--;
        }
    }

    public void moveCursorRight() {
        if (cursorPosition < text.length()) {
            cursorPosition++;
        }
    }

    public void moveCursorToStart() {
        cursorPosition = 0;
    }

    public void moveCursorToEnd() {
        cursorPosition = text.length();
    }

    public void clear() {
        text.setLength(0);
        cursorPosition = 0;
    }

    public void setText(String newText) {
        text = new StringBuilder(newText);
        cursorPosition = Math.min(cursorPosition, text.length());
    }
}

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

    /** Creates a new empty text input state. */
    public TextInputState() {
        this.text = new StringBuilder();
        this.cursorPosition = 0;
    }

    /**
     * Creates a new text input state with the given initial text.
     *
     * @param initialText the initial text content
     */
    public TextInputState(String initialText) {
        this.text = new StringBuilder(initialText);
        this.cursorPosition = initialText.length();
    }

    /**
     * Returns the current text.
     *
     * @return the text
     */
    public String text() {
        return text.toString();
    }

    /**
     * Returns the cursor position.
     *
     * @return the cursor position index
     */
    public int cursorPosition() {
        return cursorPosition;
    }

    /**
     * Returns the text length.
     *
     * @return the text length
     */
    public int length() {
        return text.length();
    }

    /**
     * Inserts a character at the cursor position.
     *
     * @param c the character to insert
     */
    public void insert(char c) {
        text.insert(cursorPosition, c);
        cursorPosition++;
    }

    /**
     * Inserts a string at the cursor position.
     *
     * @param s the string to insert
     */
    public void insert(String s) {
        text.insert(cursorPosition, s);
        cursorPosition += s.length();
    }

    /** Deletes the character before the cursor. */
    public void deleteBackward() {
        if (cursorPosition > 0) {
            text.deleteCharAt(cursorPosition - 1);
            cursorPosition--;
        }
    }

    /** Deletes the character after the cursor. */
    public void deleteForward() {
        if (cursorPosition < text.length()) {
            text.deleteCharAt(cursorPosition);
        }
    }

    /** Moves the cursor one position to the left. */
    public void moveCursorLeft() {
        if (cursorPosition > 0) {
            cursorPosition--;
        }
    }

    /** Moves the cursor one position to the right. */
    public void moveCursorRight() {
        if (cursorPosition < text.length()) {
            cursorPosition++;
        }
    }

    /** Moves the cursor to the start. */
    public void moveCursorToStart() {
        cursorPosition = 0;
    }

    /** Moves the cursor to the end. */
    public void moveCursorToEnd() {
        cursorPosition = text.length();
    }

    /** Clears all text and resets the cursor. */
    public void clear() {
        text.setLength(0);
        cursorPosition = 0;
    }

    /**
     * Replaces the text content.
     *
     * @param newText the new text content
     */
    public void setText(String newText) {
        text = new StringBuilder(newText);
        cursorPosition = Math.min(cursorPosition, text.length());
    }
}

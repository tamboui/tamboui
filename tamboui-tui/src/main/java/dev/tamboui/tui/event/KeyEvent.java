/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import dev.tamboui.tui.keymap.Action;
import dev.tamboui.tui.keymap.KeyMap;
import dev.tamboui.tui.keymap.KeyMaps;

import java.util.Optional;

/**
 * Represents a keyboard input event.
 * <p>
 * KeyEvent is associated with a {@link KeyMap} that determines how semantic
 * actions (like "move up" or "quit") are mapped to key presses. Use the
 * convenience methods like {@link #isUp()}, {@link #isDown()}, etc. to check
 * if this event matches a semantic action according to the configured keymap.
 *
 * <pre>{@code
 * // Check semantic actions
 * if (event.isUp()) {
 *     state.moveUp();
 * }
 * if (event.isQuit()) {
 *     runner.quit();
 * }
 *
 * // Or use explicit action matching
 * if (event.matches(Action.MOVE_UP)) {
 *     state.moveUp();
 * }
 * }</pre>
 */
public final class KeyEvent implements Event {

    private final KeyCode code;
    private final KeyModifiers modifiers;
    private final char character;
    private final KeyMap keyMap;

    /**
     * Creates a key event with the default keymap.
     *
     * @param code the key code ({@link KeyCode#CHAR} for printable characters)
     * @param modifiers modifier state
     * @param character the character when {@code code} is {@link KeyCode#CHAR}, otherwise ignored
     */
    public KeyEvent(KeyCode code, KeyModifiers modifiers, char character) {
        this(code, modifiers, character, KeyMaps.defaults());
    }

    /**
     * Creates a key event with a specific keymap.
     *
     * @param code the key code ({@link KeyCode#CHAR} for printable characters)
     * @param modifiers modifier state
     * @param character the character when {@code code} is {@link KeyCode#CHAR}, otherwise ignored
     * @param keyMap the keymap for semantic action matching
     */
    public KeyEvent(KeyCode code, KeyModifiers modifiers, char character, KeyMap keyMap) {
        this.code = code;
        this.modifiers = modifiers;
        this.character = character;
        this.keyMap = keyMap;
    }

    /**
     * Creates a key event for a printable character with the default keymap.
     *
     * @param c the character
     * @return key event representing the character with no modifiers
     */
    public static KeyEvent ofChar(char c) {
        return new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, c);
    }

    /**
     * Creates a key event for a printable character with modifiers and the default keymap.
     *
     * @param c         the character
     * @param modifiers modifier state
     * @return key event representing the character
     */
    public static KeyEvent ofChar(char c, KeyModifiers modifiers) {
        return new KeyEvent(KeyCode.CHAR, modifiers, c);
    }

    /**
     * Creates a key event for a printable character with a specific keymap.
     *
     * @param c      the character
     * @param keyMap the keymap for semantic action matching
     * @return key event representing the character with no modifiers
     */
    public static KeyEvent ofChar(char c, KeyMap keyMap) {
        return new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, c, keyMap);
    }

    /**
     * Creates a key event for a printable character with modifiers and a specific keymap.
     *
     * @param c         the character
     * @param modifiers modifier state
     * @param keyMap    the keymap for semantic action matching
     * @return key event representing the character
     */
    public static KeyEvent ofChar(char c, KeyModifiers modifiers, KeyMap keyMap) {
        return new KeyEvent(KeyCode.CHAR, modifiers, c, keyMap);
    }

    /**
     * Creates a key event for a special key with the default keymap.
     *
     * @param code the key code
     * @return key event with no modifiers
     */
    public static KeyEvent ofKey(KeyCode code) {
        return new KeyEvent(code, KeyModifiers.NONE, '\0');
    }

    /**
     * Creates a key event for a special key with modifiers and the default keymap.
     *
     * @param code      the key code
     * @param modifiers modifier state
     * @return key event
     */
    public static KeyEvent ofKey(KeyCode code, KeyModifiers modifiers) {
        return new KeyEvent(code, modifiers, '\0');
    }

    /**
     * Creates a key event for a special key with a specific keymap.
     *
     * @param code   the key code
     * @param keyMap the keymap for semantic action matching
     * @return key event with no modifiers
     */
    public static KeyEvent ofKey(KeyCode code, KeyMap keyMap) {
        return new KeyEvent(code, KeyModifiers.NONE, '\0', keyMap);
    }

    /**
     * Creates a key event for a special key with modifiers and a specific keymap.
     *
     * @param code      the key code
     * @param modifiers modifier state
     * @param keyMap    the keymap for semantic action matching
     * @return key event
     */
    public static KeyEvent ofKey(KeyCode code, KeyModifiers modifiers, KeyMap keyMap) {
        return new KeyEvent(code, modifiers, '\0', keyMap);
    }

    /**
     * Returns true if this is a character event matching the given character (case-sensitive).
     *
     * @param c character to compare
     * @return true if matches
     */
    public boolean isChar(char c) {
        return code == KeyCode.CHAR && character == c;
    }

    /**
     * Returns true if this is a character event matching the given character (case-insensitive).
     *
     * @param c character to compare (case-insensitive)
     * @return true if matches ignoring case
     */
    public boolean isCharIgnoreCase(char c) {
        return code == KeyCode.CHAR && Character.toLowerCase(character) == Character.toLowerCase(c);
    }

    /**
     * Returns true if this is a key event matching the given key code.
     *
     * @param keyCode key code to compare
     * @return true if matches
     */
    public boolean isKey(KeyCode keyCode) {
        return code == keyCode;
    }

    /**
     * Returns true if Ctrl modifier was pressed.
     */
    public boolean hasCtrl() {
        return modifiers.ctrl();
    }

    /**
     * Returns true if Alt modifier was pressed.
     */
    public boolean hasAlt() {
        return modifiers.alt();
    }

    /**
     * Returns true if Shift modifier was pressed.
     */
    public boolean hasShift() {
        return modifiers.shift();
    }

    /**
     * Returns true if this is a Ctrl+C event (common quit signal).
     */
    public boolean isCtrlC() {
        return hasCtrl() && isChar('c');
    }

    /**
     * Returns the key code.
     */
    public KeyCode code() {
        return code;
    }

    /**
     * Returns the modifier state.
     */
    public KeyModifiers modifiers() {
        return modifiers;
    }

    /**
     * Returns the character for {@link KeyCode#CHAR} events, or {@code '\0'} otherwise.
     */
    public char character() {
        return character;
    }

    /**
     * Returns the keymap associated with this event.
     */
    public KeyMap keyMap() {
        return keyMap;
    }

    // ========== Semantic Action Methods (delegating to keymap) ==========

    /**
     * Returns true if this event matches the given action in the configured keymap.
     *
     * @param action the action to check
     * @return true if this event triggers the action
     */
    public boolean matches(Action action) {
        return keyMap.matches(this, action);
    }

    /**
     * Returns the action that this event matches, if any.
     *
     * @return the matching action, or empty if no action matches
     */
    public Optional<Action> action() {
        return keyMap.actionFor(this);
    }

    /**
     * Returns true if this is an "up" navigation event according to the keymap.
     */
    public boolean isUp() {
        return matches(Action.MOVE_UP);
    }

    /**
     * Returns true if this is a "down" navigation event according to the keymap.
     */
    public boolean isDown() {
        return matches(Action.MOVE_DOWN);
    }

    /**
     * Returns true if this is a "left" navigation event according to the keymap.
     */
    public boolean isLeft() {
        return matches(Action.MOVE_LEFT);
    }

    /**
     * Returns true if this is a "right" navigation event according to the keymap.
     */
    public boolean isRight() {
        return matches(Action.MOVE_RIGHT);
    }

    /**
     * Returns true if this is a "page up" navigation event according to the keymap.
     */
    public boolean isPageUp() {
        return matches(Action.PAGE_UP);
    }

    /**
     * Returns true if this is a "page down" navigation event according to the keymap.
     */
    public boolean isPageDown() {
        return matches(Action.PAGE_DOWN);
    }

    /**
     * Returns true if this is a "home" navigation event according to the keymap.
     */
    public boolean isHome() {
        return matches(Action.HOME);
    }

    /**
     * Returns true if this is an "end" navigation event according to the keymap.
     */
    public boolean isEnd() {
        return matches(Action.END);
    }

    /**
     * Returns true if this is a "select" event (Enter or Space) according to the keymap.
     */
    public boolean isSelect() {
        return matches(Action.SELECT);
    }

    /**
     * Returns true if this is a "confirm" event (Enter) according to the keymap.
     */
    public boolean isConfirm() {
        return matches(Action.CONFIRM);
    }

    /**
     * Returns true if this is a "cancel" event (Escape) according to the keymap.
     */
    public boolean isCancel() {
        return matches(Action.CANCEL);
    }

    /**
     * Returns true if this is a "quit" event according to the keymap.
     */
    public boolean isQuit() {
        return matches(Action.QUIT);
    }

    /**
     * Returns true if this is a "focus next" event (Tab) according to the keymap.
     */
    public boolean isFocusNext() {
        return matches(Action.FOCUS_NEXT);
    }

    /**
     * Returns true if this is a "focus previous" event (Shift+Tab) according to the keymap.
     */
    public boolean isFocusPrevious() {
        return matches(Action.FOCUS_PREVIOUS);
    }

    /**
     * Returns true if this is a "delete backward" event (Backspace) according to the keymap.
     */
    public boolean isDeleteBackward() {
        return matches(Action.DELETE_BACKWARD);
    }

    /**
     * Returns true if this is a "delete forward" event (Delete) according to the keymap.
     */
    public boolean isDeleteForward() {
        return matches(Action.DELETE_FORWARD);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyEvent)) {
            return false;
        }
        KeyEvent keyEvent = (KeyEvent) o;
        return character == keyEvent.character
            && code == keyEvent.code
            && modifiers.equals(keyEvent.modifiers);
    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + modifiers.hashCode();
        result = 31 * result + Character.hashCode(character);
        return result;
    }

    @Override
    public String toString() {
        return String.format("KeyEvent[code=%s, modifiers=%s, character=%s]", code, modifiers, character);
    }
}

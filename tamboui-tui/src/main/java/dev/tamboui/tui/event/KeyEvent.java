/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;

import java.util.Optional;

/**
 * Represents a keyboard input event.
 * <p>
 * KeyEvent is associated with {@link Bindings} that determine how semantic
 * actions (like "move up" or "quit") are mapped to key presses. Use the
 * convenience methods like {@link #isUp()}, {@link #isDown()}, etc. to check
 * if this event matches a semantic action according to the configured bindings.
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
 * if (event.matches(Actions.MOVE_UP)) {
 *     state.moveUp();
 * }
 *
 * // Or use custom action names
 * if (event.matches("myApp.customAction")) {
 *     handleCustomAction();
 * }
 * }</pre>
 */
public final class KeyEvent implements Event {

    private final KeyCode code;
    private final KeyModifiers modifiers;
    private final char character;
    private final Bindings bindings;

    /**
     * Creates a key event with the default bindings.
     *
     * @param code the key code ({@link KeyCode#CHAR} for printable characters)
     * @param modifiers modifier state
     * @param character the character when {@code code} is {@link KeyCode#CHAR}, otherwise ignored
     */
    public KeyEvent(KeyCode code, KeyModifiers modifiers, char character) {
        this(code, modifiers, character, BindingSets.defaults());
    }

    /**
     * Creates a key event with specific bindings.
     *
     * @param code the key code ({@link KeyCode#CHAR} for printable characters)
     * @param modifiers modifier state
     * @param character the character when {@code code} is {@link KeyCode#CHAR}, otherwise ignored
     * @param bindings the bindings for semantic action matching
     */
    public KeyEvent(KeyCode code, KeyModifiers modifiers, char character, Bindings bindings) {
        this.code = code;
        this.modifiers = modifiers;
        this.character = character;
        this.bindings = bindings;
    }

    /**
     * Creates a key event for a printable character with the default bindings.
     *
     * @param c the character
     * @return key event representing the character with no modifiers
     */
    public static KeyEvent ofChar(char c) {
        return new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, c);
    }

    /**
     * Creates a key event for a printable character with modifiers and the default bindings.
     *
     * @param c         the character
     * @param modifiers modifier state
     * @return key event representing the character
     */
    public static KeyEvent ofChar(char c, KeyModifiers modifiers) {
        return new KeyEvent(KeyCode.CHAR, modifiers, c);
    }

    /**
     * Creates a key event for a printable character with specific bindings.
     *
     * @param c        the character
     * @param bindings the bindings for semantic action matching
     * @return key event representing the character with no modifiers
     */
    public static KeyEvent ofChar(char c, Bindings bindings) {
        return new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, c, bindings);
    }

    /**
     * Creates a key event for a printable character with modifiers and specific bindings.
     *
     * @param c         the character
     * @param modifiers modifier state
     * @param bindings  the bindings for semantic action matching
     * @return key event representing the character
     */
    public static KeyEvent ofChar(char c, KeyModifiers modifiers, Bindings bindings) {
        return new KeyEvent(KeyCode.CHAR, modifiers, c, bindings);
    }

    /**
     * Creates a key event for a special key with the default bindings.
     *
     * @param code the key code
     * @return key event with no modifiers
     */
    public static KeyEvent ofKey(KeyCode code) {
        return new KeyEvent(code, KeyModifiers.NONE, '\0');
    }

    /**
     * Creates a key event for a special key with modifiers and the default bindings.
     *
     * @param code      the key code
     * @param modifiers modifier state
     * @return key event
     */
    public static KeyEvent ofKey(KeyCode code, KeyModifiers modifiers) {
        return new KeyEvent(code, modifiers, '\0');
    }

    /**
     * Creates a key event for a special key with specific bindings.
     *
     * @param code     the key code
     * @param bindings the bindings for semantic action matching
     * @return key event with no modifiers
     */
    public static KeyEvent ofKey(KeyCode code, Bindings bindings) {
        return new KeyEvent(code, KeyModifiers.NONE, '\0', bindings);
    }

    /**
     * Creates a key event for a special key with modifiers and specific bindings.
     *
     * @param code      the key code
     * @param modifiers modifier state
     * @param bindings  the bindings for semantic action matching
     * @return key event
     */
    public static KeyEvent ofKey(KeyCode code, KeyModifiers modifiers, Bindings bindings) {
        return new KeyEvent(code, modifiers, '\0', bindings);
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
     * Returns the bindings associated with this event.
     */
    public Bindings bindings() {
        return bindings;
    }

    // ========== Semantic Action Methods (delegating to bindings) ==========

    /**
     * Returns true if this event matches the given action in the configured bindings.
     *
     * @param action the action to check (use {@link Actions} constants or custom strings)
     * @return true if this event triggers the action
     */
    public boolean matches(String action) {
        return bindings.matches(this, action);
    }

    /**
     * Returns the action that this event matches, if any.
     *
     * @return the matching action name, or empty if no action matches
     */
    public Optional<String> action() {
        return bindings.actionFor(this);
    }

    /**
     * Returns true if this is an "up" navigation event according to the bindings.
     */
    public boolean isUp() {
        return matches(Actions.MOVE_UP);
    }

    /**
     * Returns true if this is a "down" navigation event according to the bindings.
     */
    public boolean isDown() {
        return matches(Actions.MOVE_DOWN);
    }

    /**
     * Returns true if this is a "left" navigation event according to the bindings.
     */
    public boolean isLeft() {
        return matches(Actions.MOVE_LEFT);
    }

    /**
     * Returns true if this is a "right" navigation event according to the bindings.
     */
    public boolean isRight() {
        return matches(Actions.MOVE_RIGHT);
    }

    /**
     * Returns true if this is a "page up" navigation event according to the bindings.
     */
    public boolean isPageUp() {
        return matches(Actions.PAGE_UP);
    }

    /**
     * Returns true if this is a "page down" navigation event according to the bindings.
     */
    public boolean isPageDown() {
        return matches(Actions.PAGE_DOWN);
    }

    /**
     * Returns true if this is a "home" navigation event according to the bindings.
     */
    public boolean isHome() {
        return matches(Actions.HOME);
    }

    /**
     * Returns true if this is an "end" navigation event according to the bindings.
     */
    public boolean isEnd() {
        return matches(Actions.END);
    }

    /**
     * Returns true if this is a "select" event (Enter or Space) according to the bindings.
     */
    public boolean isSelect() {
        return matches(Actions.SELECT);
    }

    /**
     * Returns true if this is a "confirm" event (Enter) according to the bindings.
     */
    public boolean isConfirm() {
        return matches(Actions.CONFIRM);
    }

    /**
     * Returns true if this is a "cancel" event (Escape) according to the bindings.
     */
    public boolean isCancel() {
        return matches(Actions.CANCEL);
    }

    /**
     * Returns true if this is a "quit" event according to the bindings.
     */
    public boolean isQuit() {
        return matches(Actions.QUIT);
    }

    /**
     * Returns true if this is a "focus next" event (Tab) according to the bindings.
     */
    public boolean isFocusNext() {
        return matches(Actions.FOCUS_NEXT);
    }

    /**
     * Returns true if this is a "focus previous" event (Shift+Tab) according to the bindings.
     */
    public boolean isFocusPrevious() {
        return matches(Actions.FOCUS_PREVIOUS);
    }

    /**
     * Returns true if this is a "delete backward" event (Backspace) according to the bindings.
     */
    public boolean isDeleteBackward() {
        return matches(Actions.DELETE_BACKWARD);
    }

    /**
     * Returns true if this is a "delete forward" event (Delete) according to the bindings.
     */
    public boolean isDeleteForward() {
        return matches(Actions.DELETE_FORWARD);
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

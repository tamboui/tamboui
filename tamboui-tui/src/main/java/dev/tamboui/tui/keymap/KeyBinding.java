/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.keymap;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;

/**
 * Represents a key binding that can match a {@link KeyEvent}.
 * <p>
 * Bindings can match:
 * <ul>
 *   <li>A specific {@link KeyCode} (e.g., UP, ENTER)</li>
 *   <li>A character with optional modifiers (e.g., 'k', Ctrl+'u')</li>
 * </ul>
 *
 * <pre>{@code
 * // Match the UP arrow key
 * KeyBinding.key(KeyCode.UP)
 *
 * // Match lowercase 'j'
 * KeyBinding.ch('j')
 *
 * // Match 'j' or 'J' (case-insensitive)
 * KeyBinding.chIgnoreCase('j')
 *
 * // Match Ctrl+U
 * KeyBinding.ctrl('u')
 *
 * // Match Alt+V
 * KeyBinding.alt('v')
 *
 * // Match Shift+Tab
 * KeyBinding.key(KeyCode.TAB, false, false, true)
 * }</pre>
 */
public final class KeyBinding {

    private final KeyCode keyCode;
    private final Character character;
    private final boolean ctrl;
    private final boolean alt;
    private final boolean shift;
    private final boolean ignoreCase;

    private KeyBinding(KeyCode keyCode, Character character,
                       boolean ctrl, boolean alt, boolean shift, boolean ignoreCase) {
        this.keyCode = keyCode;
        this.character = character;
        this.ctrl = ctrl;
        this.alt = alt;
        this.shift = shift;
        this.ignoreCase = ignoreCase;
    }

    /**
     * Creates a binding for a special key code without modifiers.
     *
     * @param code the key code
     * @return a binding that matches the specified key
     */
    public static KeyBinding key(KeyCode code) {
        return new KeyBinding(code, null, false, false, false, false);
    }

    /**
     * Creates a binding for a special key code with modifiers.
     *
     * @param code  the key code
     * @param ctrl  true if Ctrl must be pressed
     * @param alt   true if Alt must be pressed
     * @param shift true if Shift must be pressed
     * @return a binding that matches the specified key with modifiers
     */
    public static KeyBinding key(KeyCode code, boolean ctrl, boolean alt, boolean shift) {
        return new KeyBinding(code, null, ctrl, alt, shift, false);
    }

    /**
     * Creates a binding for a character (case-sensitive).
     *
     * @param c the character to match
     * @return a binding that matches the exact character
     */
    public static KeyBinding ch(char c) {
        return new KeyBinding(KeyCode.CHAR, c, false, false, false, false);
    }

    /**
     * Creates a binding for a character (case-insensitive).
     *
     * @param c the character to match (case-insensitive)
     * @return a binding that matches the character regardless of case
     */
    public static KeyBinding chIgnoreCase(char c) {
        return new KeyBinding(KeyCode.CHAR, c, false, false, false, true);
    }

    /**
     * Creates a binding for Ctrl+character.
     *
     * @param c the character
     * @return a binding that matches Ctrl+c
     */
    public static KeyBinding ctrl(char c) {
        return new KeyBinding(KeyCode.CHAR, c, true, false, false, false);
    }

    /**
     * Creates a binding for Alt+character.
     *
     * @param c the character
     * @return a binding that matches Alt+c
     */
    public static KeyBinding alt(char c) {
        return new KeyBinding(KeyCode.CHAR, c, false, true, false, false);
    }

    /**
     * Returns true if this binding matches the given event.
     * <p>
     * Matching rules:
     * <ul>
     *   <li>Key code must match</li>
     *   <li>Ctrl and Alt modifiers must match exactly</li>
     *   <li>Shift is only checked if explicitly required by the binding</li>
     *   <li>For character bindings, the character must match (respecting ignoreCase)</li>
     * </ul>
     *
     * @param event the key event to match against
     * @return true if this binding matches the event
     */
    public boolean matches(KeyEvent event) {
        // Check key code first
        if (event.code() != keyCode) {
            return false;
        }

        // Check modifiers
        KeyModifiers mods = event.modifiers();
        if (ctrl != mods.ctrl() || alt != mods.alt()) {
            return false;
        }
        // Shift is only checked if explicitly required
        if (shift && !mods.shift()) {
            return false;
        }

        // Check character for CHAR events
        if (keyCode == KeyCode.CHAR && character != null) {
            char eventChar = event.character();
            if (ignoreCase) {
                return Character.toLowerCase(eventChar) == Character.toLowerCase(character);
            }
            return eventChar == character;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (ctrl) {
            sb.append("Ctrl+");
        }
        if (alt) {
            sb.append("Alt+");
        }
        if (shift) {
            sb.append("Shift+");
        }
        if (character != null) {
            if (character == ' ') {
                sb.append("Space");
            } else {
                sb.append(character);
            }
        } else {
            sb.append(formatKeyCode(keyCode));
        }
        return sb.toString();
    }

    private static String formatKeyCode(KeyCode code) {
        switch (code) {
            case UP:
                return "Up";
            case DOWN:
                return "Down";
            case LEFT:
                return "Left";
            case RIGHT:
                return "Right";
            case PAGE_UP:
                return "PageUp";
            case PAGE_DOWN:
                return "PageDown";
            case HOME:
                return "Home";
            case END:
                return "End";
            case ENTER:
                return "Enter";
            case ESCAPE:
                return "Escape";
            case TAB:
                return "Tab";
            case BACKSPACE:
                return "Backspace";
            case DELETE:
                return "Delete";
            case INSERT:
                return "Insert";
            default:
                return code.name();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyBinding)) {
            return false;
        }
        KeyBinding that = (KeyBinding) o;
        return ctrl == that.ctrl &&
               alt == that.alt &&
               shift == that.shift &&
               ignoreCase == that.ignoreCase &&
               keyCode == that.keyCode &&
               (character == null ? that.character == null : character.equals(that.character));
    }

    @Override
    public int hashCode() {
        int result = keyCode != null ? keyCode.hashCode() : 0;
        result = 31 * result + (character != null ? character.hashCode() : 0);
        result = 31 * result + (ctrl ? 1 : 0);
        result = 31 * result + (alt ? 1 : 0);
        result = 31 * result + (shift ? 1 : 0);
        result = 31 * result + (ignoreCase ? 1 : 0);
        return result;
    }
}

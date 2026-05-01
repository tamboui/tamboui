/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;

/**
 * An {@link InputTrigger} that matches {@link KeyEvent}s.
 * <p>
 * Key triggers can match:
 * <ul>
 *   <li>A specific {@link KeyCode} (e.g., UP, ENTER)</li>
 *   <li>A character with optional modifiers (e.g., 'k', Ctrl+'u')</li>
 * </ul>
 *
 * <pre>{@code
 * // Match the UP arrow key
 * KeyTrigger.key(KeyCode.UP)
 *
 * // Match lowercase 'j'
 * KeyTrigger.ch('j')
 *
 * // Match 'j' or 'J' (case-insensitive)
 * KeyTrigger.chIgnoreCase('j')
 *
 * // Match Ctrl+U
 * KeyTrigger.ctrl('u')
 *
 * // Match Alt+V
 * KeyTrigger.alt('v')
 *
 * // Match Shift+Tab
 * KeyTrigger.key(KeyCode.TAB, false, false, true)
 * }</pre>
 */
public final class KeyTrigger implements InputTrigger {

    private final KeyCode keyCode;
    private final Integer character;
    private final boolean ctrl;
    private final boolean alt;
    private final boolean shift;
    private final boolean ignoreCase;

    private KeyTrigger(KeyCode keyCode, Integer character,
                       boolean ctrl, boolean alt, boolean shift, boolean ignoreCase) {
        this.keyCode = keyCode;
        this.character = character;
        this.ctrl = ctrl;
        this.alt = alt;
        this.shift = shift;
        this.ignoreCase = ignoreCase;
    }

    /**
     * Creates a trigger for a special key code without modifiers.
     *
     * @param code the key code
     * @return a trigger that matches the specified key
     */
    public static KeyTrigger key(KeyCode code) {
        return new KeyTrigger(code, null, false, false, false, false);
    }

    /**
     * Creates a trigger for a special key code with modifiers.
     *
     * @param code  the key code
     * @param ctrl  true if Ctrl must be pressed
     * @param alt   true if Alt must be pressed
     * @param shift true if Shift must be pressed
     * @return a trigger that matches the specified key with modifiers
     */
    public static KeyTrigger key(KeyCode code, boolean ctrl, boolean alt, boolean shift) {
        return new KeyTrigger(code, null, ctrl, alt, shift, false);
    }

    /**
     * Creates a trigger for a character (case-sensitive).
     *
     * @param c the character to match
     * @return a trigger that matches the exact character
     */
    public static KeyTrigger ch(char c) {
        return ch((int) c);
    }

    /**
     * Creates a trigger for a Unicode code point (case-sensitive).
     *
     * @param codePoint the code point to match
     * @return a trigger that matches the exact code point
     */
    public static KeyTrigger ch(int codePoint) {
        return new KeyTrigger(KeyCode.CHAR, codePoint, false, false, false, false);
    }

    /**
     * Creates a trigger for a character (case-insensitive).
     *
     * @param c the character to match
     * @return a trigger that matches the character regardless of case
     */
    public static KeyTrigger chIgnoreCase(char c) {
        return chIgnoreCase((int) c);
    }

    /**
     * Creates a trigger for a Unicode code point (case-insensitive).
     *
     * @param codePoint the code point to match
     * @return a trigger that matches the code point regardless of case
     */
    public static KeyTrigger chIgnoreCase(int codePoint) {
        return new KeyTrigger(KeyCode.CHAR, codePoint, false, false, false, true);
    }

    /**
     * Creates a trigger for a Ctrl+key combination.
     *
     * @param c the character pressed with Ctrl
     * @return a trigger that matches Ctrl + the given character
     */
    public static KeyTrigger ctrl(char c) {
        return ctrl((int) c);
    }

    /**
     * Creates a trigger for a Ctrl+key combination.
     *
     * @param codePoint the code point of the key pressed with Ctrl
     * @return a trigger that matches Ctrl + the given code point
     */
    public static KeyTrigger ctrl(int codePoint) {
        return new KeyTrigger(KeyCode.CHAR, codePoint, true, false, false, false);
    }

    /**
     * Creates a trigger for an Alt+key combination.
     *
     * @param c the character pressed with Alt
     * @return a trigger that matches Alt + the given character
     */
    public static KeyTrigger alt(char c) {
        return alt((int) c);
    }

    /**
     * Creates a trigger for an Alt+key combination.
     *
     * @param codePoint the code point of the key pressed with Alt
     * @return a trigger that matches Alt + the given code point
     */
    public static KeyTrigger alt(int codePoint) {
        return new KeyTrigger(KeyCode.CHAR, codePoint, false, true, false, false);
    }

    @Override
    public boolean matches(Event event) {
        if (!(event instanceof KeyEvent)) {
            return false;
        }
        return matchesKey((KeyEvent) event);
    }

    /**
     * Returns true if this trigger matches the given key event.
     * <p>
     * Matching rules:
     * <ul>
     *   <li>Key code must match</li>
     *   <li>Ctrl and Alt modifiers must match exactly</li>
     *   <li>Shift is only checked if explicitly required by the trigger</li>
     *   <li>For character triggers, the character must match (respecting ignoreCase)</li>
     * </ul>
     *
     * @param event the key event to match against
     * @return true if this trigger matches the event
     */
    public boolean matchesKey(KeyEvent event) {
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
            int eventCodePoint = event.codePoint();
            if (ignoreCase) {
                return Character.toLowerCase(eventCodePoint) == Character.toLowerCase(character);
            }
            return eventCodePoint == character;
        }

        return true;
    }

    @Override
    public String describe() {
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
            if (character == (int) ' ') {
                sb.append("Space");
            } else {
                sb.appendCodePoint(character);
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
    public String toString() {
        return describe();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyTrigger)) {
            return false;
        }
        KeyTrigger that = (KeyTrigger) o;
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

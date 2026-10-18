/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyEventKind;
import dev.tamboui.tui.event.KeyModifiers;

/**
 * An {@link InputTrigger} that matches {@link KeyEvent}s.
 * <p>
 * Key triggers can match:
 * <ul>
 *   <li>A specific {@link KeyCode} (e.g., UP, ENTER)</li>
 *   <li>A character with optional modifiers (e.g., 'k', Ctrl+'u', Super+'k')</li>
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
 * // Match Super+K (Command/Windows)
 * KeyTrigger.superKey('k')
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
    private final boolean superKey;
    private final boolean hyper;
    private final boolean meta;
    private final boolean ignoreCase;
    private final KeyEventKind kind;

    private KeyTrigger(
            KeyCode keyCode,
            Integer character,
            boolean ctrl,
            boolean alt,
            boolean shift,
            boolean superKey,
            boolean hyper,
            boolean meta,
            boolean ignoreCase,
            KeyEventKind kind
    ) {
        this.keyCode = keyCode;
        this.character = character;
        this.ctrl = ctrl;
        this.alt = alt;
        this.shift = shift;
        this.superKey = superKey;
        this.hyper = hyper;
        this.meta = meta;
        this.ignoreCase = ignoreCase;
        this.kind = kind;
    }

    /**
     * Creates a trigger for a special key code without modifiers.
     *
     * @param code the key code
     * @return a trigger that matches the specified key
     */
    public static KeyTrigger key(KeyCode code) {
        return new KeyTrigger(code, null, false, false, false, false, false, false, false, null);
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
        return new KeyTrigger(code, null, ctrl, alt, shift, false, false, false, false, null);
    }

    /**
     * Creates a trigger for a special key code with all modifier flags.
     *
     * @param code     the key code
     * @param ctrl     true if Ctrl must be pressed
     * @param alt      true if Alt must be pressed
     * @param shift    true if Shift must be pressed
     * @param superKey true if Super (Command/Windows) must be pressed
     * @param hyper    true if Hyper must be pressed
     * @param meta     true if Meta must be pressed
     * @return a trigger that matches the specified key with modifiers
     */
    public static KeyTrigger key(
            KeyCode code,
            boolean ctrl,
            boolean alt,
            boolean shift,
            boolean superKey,
            boolean hyper,
            boolean meta
    ) {
        return new KeyTrigger(code, null, ctrl, alt, shift, superKey, hyper, meta, false, null);
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
        return new KeyTrigger(KeyCode.CHAR, codePoint, false, false, false, false, false, false, false, null);
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
        return new KeyTrigger(KeyCode.CHAR, codePoint, false, false, false, false, false, false, true, null);
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
        return new KeyTrigger(KeyCode.CHAR, codePoint, true, false, false, false, false, false, false, null);
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
        return new KeyTrigger(KeyCode.CHAR, codePoint, false, true, false, false, false, false, false, null);
    }

    /**
     * Creates a trigger for a Super+key combination (Command on macOS, Windows key on PC).
     *
     * @param c the character pressed with Super
     * @return a trigger that matches Super + the given character
     */
    public static KeyTrigger superKey(char c) {
        return superKey((int) c);
    }

    /**
     * Creates a trigger for a Super+key combination.
     *
     * @param codePoint the code point of the key pressed with Super
     * @return a trigger that matches Super + the given code point
     */
    public static KeyTrigger superKey(int codePoint) {
        return new KeyTrigger(KeyCode.CHAR, codePoint, false, false, false, true, false, false, false, null);
    }

    /**
     * Creates a trigger for a Super+special key combination.
     *
     * @param code the special key pressed with Super
     * @return a trigger that matches Super + the given key
     */
    public static KeyTrigger superKey(KeyCode code) {
        return new KeyTrigger(code, null, false, false, false, true, false, false, false, null);
    }

    /**
     * Returns a copy of this trigger configured to match a specific event kind.
     *
     * @param kind the event kind to match, or null to match press and repeat
     * @return a new trigger with the specified event kind
     */
    public KeyTrigger withKind(KeyEventKind kind) {
        return new KeyTrigger(keyCode, character, ctrl, alt, shift, superKey, hyper, meta, ignoreCase, kind);
    }

    /**
     * Returns a copy of this trigger with the Super modifier required.
     *
     * @return a new trigger with Super enabled
     */
    public KeyTrigger withSuper() {
        return new KeyTrigger(keyCode, character, ctrl, alt, shift, true, hyper, meta, ignoreCase, kind);
    }

    /**
     * Returns a copy of this trigger with the Hyper modifier required.
     *
     * @return a new trigger with Hyper enabled
     */
    public KeyTrigger withHyper() {
        return new KeyTrigger(keyCode, character, ctrl, alt, shift, superKey, true, meta, ignoreCase, kind);
    }

    /**
     * Returns a copy of this trigger with the Meta modifier required.
     *
     * @return a new trigger with Meta enabled
     */
    public KeyTrigger withMeta() {
        return new KeyTrigger(keyCode, character, ctrl, alt, shift, superKey, hyper, true, ignoreCase, kind);
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
     *   <li>Release events are ignored unless explicitly requested with {@link #withKind(KeyEventKind)}</li>
     *   <li>Key code must match (or character matches for character triggers)</li>
     *   <li>Ctrl, Alt, Super, Hyper, and Meta modifiers must match exactly</li>
     *   <li>Shift is only checked if explicitly required by the trigger</li>
     *   <li>For character triggers, matches logical, shifted, base-layout, or produced text</li>
     * </ul>
     *
     * @param event the key event to match against
     * @return true if this trigger matches the event
     */
    public boolean matchesKey(KeyEvent event) {
        // Event kind check: ignore release events by default
        if (kind == null) {
            if (event.isRelease()) {
                return false;
            }
        } else if (event.kind() != kind) {
            return false;
        }

        // Check key code first
        if (keyCode != KeyCode.CHAR && event.code() != keyCode) {
            return false;
        }

        // Check modifiers
        KeyModifiers mods = event.modifiers();
        if (ctrl != mods.ctrl()
                || alt != mods.alt()
                || superKey != mods.superKey()
                || hyper != mods.hyper()
                || meta != mods.meta()) {
            return false;
        }
        // Shift is only checked if explicitly required
        if (shift && !mods.shift()) {
            return false;
        }

        // Check character for CHAR events
        if (keyCode == KeyCode.CHAR && character != null) {
            return matchesCharacter(event, character, ignoreCase);
        }

        return true;
    }

    private static boolean matchesCharacter(KeyEvent event, int target, boolean ignoreCase) {
        if (ignoreCase) {
            int t = Character.toLowerCase(target);
            if (Character.toLowerCase(event.codePoint()) == t) {
                return true;
            }
            if (Character.toLowerCase(event.keyCodePoint()) == t) {
                return true;
            }
            if (event.shiftedKeyCodePoint().isPresent()
                    && Character.toLowerCase(event.shiftedKeyCodePoint().getAsInt()) == t) {
                return true;
            }
            if (event.baseLayoutKeyCodePoint().isPresent()
                    && Character.toLowerCase(event.baseLayoutKeyCodePoint().getAsInt()) == t) {
                return true;
            }
            String text = event.text();
            if (!text.isEmpty() && text.codePointCount(0, text.length()) == 1
                    && Character.toLowerCase(text.codePointAt(0)) == t) {
                return true;
            }
            return false;
        }

        // Case-sensitive matching
        if (event.codePoint() == target) {
            return true;
        }
        String text = event.text();
        if (!text.isEmpty() && text.codePointCount(0, text.length()) == 1 && text.codePointAt(0) == target) {
            return true;
        }
        if (event.shiftedKeyCodePoint().isPresent() && event.shiftedKeyCodePoint().getAsInt() == target) {
            return true;
        }
        if (event.baseLayoutKeyCodePoint().isPresent() && event.baseLayoutKeyCodePoint().getAsInt() == target) {
            return true;
        }
        if (event.keyCodePoint() == target && (!event.hasShift() || Character.isUpperCase(target))) {
            return true;
        }
        return false;
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (superKey) {
            sb.append("Super+");
        }
        if (hyper) {
            sb.append("Hyper+");
        }
        if (meta) {
            sb.append("Meta+");
        }
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
        return ctrl == that.ctrl
                && alt == that.alt
                && shift == that.shift
                && superKey == that.superKey
                && hyper == that.hyper
                && meta == that.meta
                && ignoreCase == that.ignoreCase
                && keyCode == that.keyCode
                && kind == that.kind
                && (character == null ? that.character == null : character.equals(that.character));
    }

    @Override
    public int hashCode() {
        int result = keyCode != null ? keyCode.hashCode() : 0;
        result = 31 * result + (character != null ? character.hashCode() : 0);
        result = 31 * result + (ctrl ? 1 : 0);
        result = 31 * result + (alt ? 1 : 0);
        result = 31 * result + (shift ? 1 : 0);
        result = 31 * result + (superKey ? 1 : 0);
        result = 31 * result + (hyper ? 1 : 0);
        result = 31 * result + (meta ? 1 : 0);
        result = 31 * result + (ignoreCase ? 1 : 0);
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        return result;
    }
}

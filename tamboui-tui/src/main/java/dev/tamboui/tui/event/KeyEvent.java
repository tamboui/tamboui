/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import java.util.Optional;
import java.util.OptionalInt;

import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;

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
    private final int character;
    private final Bindings bindings;
    private final KeyEventKind kind;
    private final KeyEventState state;
    private final int keyCodePoint;
    private final OptionalInt shiftedKeyCodePoint;
    private final OptionalInt baseLayoutKeyCodePoint;
    private final String text;

    /**
     * Creates a key event with the default bindings.
     *
     * @param code      the key code ({@link KeyCode#CHAR} for printable characters)
     * @param modifiers modifier state
     * @param character the character when {@code code} is {@link KeyCode#CHAR}, otherwise ignored
     */
    public KeyEvent(KeyCode code, KeyModifiers modifiers, int character) {
        this(code, modifiers, character, BindingSets.defaults());
    }

    /**
     * Creates a key event with specific bindings.
     *
     * @param code      the key code ({@link KeyCode#CHAR} for printable characters)
     * @param modifiers modifier state
     * @param character the Unicode code point when {@code code} is {@link KeyCode#CHAR}, otherwise ignored
     * @param bindings  the bindings for semantic action matching
     */
    public KeyEvent(KeyCode code, KeyModifiers modifiers, int character, Bindings bindings) {
        this(
                code,
                modifiers,
                character,
                bindings,
                KeyEventKind.PRESS,
                KeyEventState.NONE,
                code == KeyCode.CHAR ? character : 0,
                OptionalInt.empty(),
                OptionalInt.empty(),
                deriveText(code, modifiers, character)
        );
    }

    /**
     * Creates a complete key event with all attributes.
     *
     * @param code                   the key code
     * @param modifiers              modifier state
     * @param character              Unicode code point for printable character, or 0
     * @param bindings               bindings for semantic action matching
     * @param kind                   event kind (press, repeat, release)
     * @param state                  lock and keypad state
     * @param keyCodePoint           logical unshifted key codepoint, where applicable
     * @param shiftedKeyCodePoint    shifted key codepoint, if reported
     * @param baseLayoutKeyCodePoint base-layout key codepoint, if reported
     * @param text                   produced text, or empty string if none
     */
    public KeyEvent(
            KeyCode code,
            KeyModifiers modifiers,
            int character,
            Bindings bindings,
            KeyEventKind kind,
            KeyEventState state,
            int keyCodePoint,
            OptionalInt shiftedKeyCodePoint,
            OptionalInt baseLayoutKeyCodePoint,
            String text
    ) {
        this.code = code != null ? code : KeyCode.UNKNOWN;
        this.modifiers = modifiers != null ? modifiers : KeyModifiers.NONE;
        this.character = character;
        this.bindings = bindings != null ? bindings : BindingSets.defaults();
        this.kind = kind != null ? kind : KeyEventKind.PRESS;
        this.state = state != null ? state : KeyEventState.NONE;
        this.keyCodePoint = keyCodePoint;
        this.shiftedKeyCodePoint = shiftedKeyCodePoint != null ? shiftedKeyCodePoint : OptionalInt.empty();
        this.baseLayoutKeyCodePoint = baseLayoutKeyCodePoint != null ? baseLayoutKeyCodePoint : OptionalInt.empty();
        this.text = text != null ? text : "";
    }

    private static String deriveText(KeyCode code, KeyModifiers modifiers, int character) {
        if (code == KeyCode.CHAR && character >= 32 && character != 127) {
            if (!modifiers.ctrl() && !modifiers.alt()) {
                return new String(Character.toChars(character));
            }
        }
        return "";
    }

    /**
     * Returns a new builder for constructing {@link KeyEvent} instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a key event for a printable character with the default bindings.
     *
     * @param c the character
     * @return key event representing the character with no modifiers
     */
    public static KeyEvent ofChar(char c) {
        return ofChar((int) c);
    }

    /**
     * Creates a key event for a printable character with no modifiers and the default bindings.
     *
     * @param codePoint the Unicode code point
     * @return key event representing the character with no modifiers
     */
    public static KeyEvent ofChar(int codePoint) {
        return new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, codePoint);
    }

    /**
     * Creates a key event for a printable character with modifiers and the default bindings.
     *
     * @param c         the character
     * @param modifiers modifier state
     * @return key event representing the character
     */
    public static KeyEvent ofChar(char c, KeyModifiers modifiers) {
        return ofChar((int) c, modifiers);
    }

    /**
     * Creates a key event for a printable character with modifiers and the default bindings.
     *
     * @param codePoint the Unicode code point
     * @param modifiers modifier state
     * @return key event representing the character
     */
    public static KeyEvent ofChar(int codePoint, KeyModifiers modifiers) {
        return new KeyEvent(KeyCode.CHAR, modifiers, codePoint);
    }

    /**
     * Creates a key event for a printable character with event kind and the default bindings.
     *
     * @param codePoint the Unicode code point
     * @param modifiers modifier state
     * @param kind      the event kind
     * @return key event representing the character
     */
    public static KeyEvent ofChar(int codePoint, KeyModifiers modifiers, KeyEventKind kind) {
        return ofChar(codePoint, modifiers, kind, BindingSets.defaults());
    }

    /**
     * Creates a key event for a printable character with modifiers, event kind, and bindings.
     *
     * @param codePoint the Unicode code point
     * @param modifiers modifier state
     * @param kind      the event kind
     * @param bindings  the bindings for semantic action matching
     * @return key event representing the character
     */
    public static KeyEvent ofChar(int codePoint, KeyModifiers modifiers, KeyEventKind kind, Bindings bindings) {
        return builder()
                .code(KeyCode.CHAR)
                .modifiers(modifiers)
                .character(codePoint)
                .keyCodePoint(codePoint)
                .kind(kind)
                .bindings(bindings)
                .text(deriveText(KeyCode.CHAR, modifiers != null ? modifiers : KeyModifiers.NONE, codePoint))
                .build();
    }

    /**
     * Creates a key event for a printable character with specific bindings.
     *
     * @param c        the character
     * @param bindings the bindings for semantic action matching
     * @return key event representing the character with no modifiers
     */
    public static KeyEvent ofChar(char c, Bindings bindings) {
        return ofChar((int) c, bindings);
    }

    /**
     * Creates a key event for a printable character with specific bindings.
     *
     * @param codePoint the Unicode code point
     * @param bindings  the bindings for semantic action matching
     * @return key event representing the character with no modifiers
     */
    public static KeyEvent ofChar(int codePoint, Bindings bindings) {
        return new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, codePoint, bindings);
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
        return ofChar((int) c, modifiers, bindings);
    }

    /**
     * Creates a key event for a printable character with modifiers and specific bindings.
     *
     * @param codePoint the Unicode code point
     * @param modifiers modifier state
     * @param bindings  the bindings for semantic action matching
     * @return key event representing the character
     */
    public static KeyEvent ofChar(int codePoint, KeyModifiers modifiers, Bindings bindings) {
        return new KeyEvent(KeyCode.CHAR, modifiers, codePoint, bindings);
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
     * Creates a key event for a special key with event kind and the default bindings.
     *
     * @param code      the key code
     * @param modifiers modifier state
     * @param kind      the event kind
     * @return key event
     */
    public static KeyEvent ofKey(KeyCode code, KeyModifiers modifiers, KeyEventKind kind) {
        return ofKey(code, modifiers, kind, BindingSets.defaults());
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
     * Creates a key event for a special key with modifiers, event kind, and bindings.
     *
     * @param code      the key code
     * @param modifiers modifier state
     * @param kind      the event kind
     * @param bindings  the bindings for semantic action matching
     * @return key event
     */
    public static KeyEvent ofKey(KeyCode code, KeyModifiers modifiers, KeyEventKind kind, Bindings bindings) {
        return builder()
                .code(code)
                .modifiers(modifiers)
                .character('\0')
                .kind(kind)
                .bindings(bindings)
                .build();
    }

    /**
     * Returns true if this is a character event matching the given character (case-sensitive).
     *
     * @param c character to compare
     * @return true if matches
     */
    public boolean isChar(char c) {
        return isChar((int) c);
    }

    /**
     * Returns true if this is a character event matching the given code point.
     *
     * @param codePoint code point to compare
     * @return true if matches
     */
    public boolean isChar(int codePoint) {
        if (code == KeyCode.CHAR && (character == codePoint || keyCodePoint == codePoint)) {
            if (Character.isLowerCase(codePoint) && hasShift() && !text.isEmpty() && Character.isUpperCase(text.codePointAt(0))) {
                return false;
            }
            return true;
        }
        if (text != null && !text.isEmpty() && text.codePointCount(0, text.length()) == 1) {
            if (text.codePointAt(0) == codePoint) {
                return true;
            }
        }
        if (shiftedKeyCodePoint != null && shiftedKeyCodePoint.isPresent() && shiftedKeyCodePoint.getAsInt() == codePoint) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if this is a character event matching the given character (case-insensitive).
     *
     * @param c character to compare (case-insensitive)
     * @return true if matches ignoring case
     */
    public boolean isCharIgnoreCase(char c) {
        return isCharIgnoreCase((int) c);
    }

    /**
     * Returns true if this is a character event matching the given code point (case-insensitive).
     *
     * @param codePoint code point to compare (case-insensitive)
     * @return true if matches ignoring case
     */
    public boolean isCharIgnoreCase(int codePoint) {
        int target = Character.toLowerCase(codePoint);
        if (code == KeyCode.CHAR && (Character.toLowerCase(character) == target || Character.toLowerCase(keyCodePoint) == target)) {
            return true;
        }
        if (text != null && !text.isEmpty() && text.codePointCount(0, text.length()) == 1) {
            if (Character.toLowerCase(text.codePointAt(0)) == target) {
                return true;
            }
        }
        if (shiftedKeyCodePoint != null && shiftedKeyCodePoint.isPresent() && Character.toLowerCase(shiftedKeyCodePoint.getAsInt()) == target) {
            return true;
        }
        if (baseLayoutKeyCodePoint != null && baseLayoutKeyCodePoint.isPresent() && Character.toLowerCase(baseLayoutKeyCodePoint.getAsInt()) == target) {
            return true;
        }
        return false;
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
     * Returns true if this is a key press event.
     *
     * @return true if press
     */
    public boolean isPress() {
        return kind.isPress();
    }

    /**
     * Returns true if this is a key repeat event.
     *
     * @return true if repeat
     */
    public boolean isRepeat() {
        return kind.isRepeat();
    }

    /**
     * Returns true if this is a key release event.
     *
     * @return true if release
     */
    public boolean isRelease() {
        return kind.isRelease();
    }

    /**
     * Returns true if this is either a key press or repeat event.
     *
     * @return true if press or repeat
     */
    public boolean isPressOrRepeat() {
        return kind.isPressOrRepeat();
    }

    /**
     * Returns true if Ctrl modifier was pressed.
     *
     * @return true if Ctrl was held
     */
    public boolean hasCtrl() {
        return modifiers.ctrl();
    }

    /**
     * Returns true if Alt modifier was pressed.
     *
     * @return true if Alt was held
     */
    public boolean hasAlt() {
        return modifiers.alt();
    }

    /**
     * Returns true if Shift modifier was pressed.
     *
     * @return true if Shift was held
     */
    public boolean hasShift() {
        return modifiers.shift();
    }

    /**
     * Returns true if Super (Command/Windows) modifier was pressed.
     *
     * @return true if Super was held
     */
    public boolean hasSuper() {
        return modifiers.superKey();
    }

    /**
     * Returns true if Hyper modifier was pressed.
     *
     * @return true if Hyper was held
     */
    public boolean hasHyper() {
        return modifiers.hyper();
    }

    /**
     * Returns true if Meta modifier was pressed.
     *
     * @return true if Meta was held
     */
    public boolean hasMeta() {
        return modifiers.meta();
    }

    /**
     * Returns true if the key was on the numeric keypad.
     *
     * @return true if keypad key
     */
    public boolean isKeypad() {
        return state.keypad();
    }

    /**
     * Returns true if Caps Lock was active.
     *
     * @return true if Caps Lock is active
     */
    public boolean hasCapsLock() {
        return state.capsLock();
    }

    /**
     * Returns true if Num Lock was active.
     *
     * @return true if Num Lock is active
     */
    public boolean hasNumLock() {
        return state.numLock();
    }

    /**
     * Returns true if this is a Ctrl+C event (common quit signal).
     *
     * @return true if Ctrl+C was pressed
     */
    public boolean isCtrlC() {
        return hasCtrl() && isChar('c');
    }

    /**
     * Returns the key code.
     *
     * @return the key code
     */
    public KeyCode code() {
        return code;
    }

    /**
     * Returns the modifier state.
     *
     * @return the modifier state
     */
    public KeyModifiers modifiers() {
        return modifiers;
    }

    /**
     * Returns the event kind (press, repeat, release).
     *
     * @return the event kind
     */
    public KeyEventKind kind() {
        return kind;
    }

    /**
     * Returns the lock and keypad state.
     *
     * @return the event state
     */
    public KeyEventState state() {
        return state;
    }

    /**
     * Returns the logical/unshifted key Unicode code point, where applicable, or 0.
     *
     * @return the logical key code point
     */
    public int keyCodePoint() {
        return keyCodePoint;
    }

    /**
     * Returns the shifted key code point if reported by the terminal.
     *
     * @return the shifted key code point
     */
    public OptionalInt shiftedKeyCodePoint() {
        return shiftedKeyCodePoint;
    }

    /**
     * Returns the base-layout key code point (standard PC-101 layout) if reported by the terminal.
     *
     * @return the base-layout key code point
     */
    public OptionalInt baseLayoutKeyCodePoint() {
        return baseLayoutKeyCodePoint;
    }

    /**
     * Returns the text produced by this key event, or an empty string if none.
     * <p>
     * Under the Kitty keyboard protocol with associated text enabled, this contains
     * the actual text generated by the keystroke, which may consist of multiple code points,
     * combining characters, or layout transformations.
     *
     * @return the produced text
     */
    public String text() {
        return text;
    }

    /**
     * Returns the character for {@link KeyCode#CHAR} events as a {@code char}, or {@code '\0'}
     * otherwise. For BMP code points (U+0000–U+FFFF) this is the exact character. For
     * supplementary code points (emoji, rare scripts) this returns {@code '\uFFFD'} (replacement
     * character); use {@link #string()}, {@link #text()}, or {@link #codePoint()} in those cases.
     *
     * @return the character, or {@code '\0'} for non-{@link KeyCode#CHAR} events
     * @deprecated Prefer {@link #text()} or {@link #string()} for text insertion/concatenation or
     *             {@link #isChar} for comparisons. {@code character()} cannot represent supplementary code points.
     */
    @Deprecated
    public char character() {
        if (text != null && !text.isEmpty()) {
            int cp = text.codePointAt(0);
            return cp <= Character.MAX_VALUE ? (char) cp : '\uFFFD';
        }
        if (code != KeyCode.CHAR) {
            return '\0';
        }
        return character <= Character.MAX_VALUE ? (char) character : '\uFFFD';
    }

    /**
     * Returns the character for {@link KeyCode#CHAR} events as a {@link String}, or an empty
     * string otherwise. Correctly handles the full Unicode range including supplementary
     * characters (emoji, etc.).
     *
     * <p>Use this for text insertion, display, and string concatenation.
     * Use {@link #codePoint()} for numeric range checks.
     *
     * @return a {@code String} representing the Unicode character,
     *         or {@code ""} for non-{@link KeyCode#CHAR} events
     */
    public String string() {
        if (text != null && !text.isEmpty()) {
            return text;
        }
        return code == KeyCode.CHAR && character != 0 ? new String(Character.toChars(character)) : "";
    }

    /**
     * Returns the Unicode code point for {@link KeyCode#CHAR} events, or {@code 0} otherwise.
     * Use this when you need the numeric value for range checks or comparisons.
     *
     * @return the Unicode code point, or {@code 0} for non-{@link KeyCode#CHAR} events
     */
    public int codePoint() {
        if (text != null && !text.isEmpty() && text.codePointCount(0, text.length()) == 1) {
            return text.codePointAt(0);
        }
        return code == KeyCode.CHAR ? character : 0;
    }

    /**
     * Returns the bindings associated with this event.
     *
     * @return the bindings
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
     *
     * @return true if this event triggers the move-up action
     */
    public boolean isUp() {
        return matches(Actions.MOVE_UP);
    }

    /**
     * Returns true if this is a "down" navigation event according to the bindings.
     *
     * @return true if this event triggers the move-down action
     */
    public boolean isDown() {
        return matches(Actions.MOVE_DOWN);
    }

    /**
     * Returns true if this is a "left" navigation event according to the bindings.
     *
     * @return true if this event triggers the move-left action
     */
    public boolean isLeft() {
        return matches(Actions.MOVE_LEFT);
    }

    /**
     * Returns true if this is a "right" navigation event according to the bindings.
     *
     * @return true if this event triggers the move-right action
     */
    public boolean isRight() {
        return matches(Actions.MOVE_RIGHT);
    }

    /**
     * Returns true if this is a "page up" navigation event according to the bindings.
     *
     * @return true if this event triggers the page-up action
     */
    public boolean isPageUp() {
        return matches(Actions.PAGE_UP);
    }

    /**
     * Returns true if this is a "page down" navigation event according to the bindings.
     *
     * @return true if this event triggers the page-down action
     */
    public boolean isPageDown() {
        return matches(Actions.PAGE_DOWN);
    }

    /**
     * Returns true if this is a "home" navigation event according to the bindings.
     *
     * @return true if this event triggers the home action
     */
    public boolean isHome() {
        return matches(Actions.HOME);
    }

    /**
     * Returns true if this is an "end" navigation event according to the bindings.
     *
     * @return true if this event triggers the end action
     */
    public boolean isEnd() {
        return matches(Actions.END);
    }

    /**
     * Returns true if this is a "select" event (Enter or Space) according to the bindings.
     *
     * @return true if this event triggers the select action
     */
    public boolean isSelect() {
        return matches(Actions.SELECT);
    }

    /**
     * Returns true if this is a "confirm" event (Enter) according to the bindings.
     *
     * @return true if this event triggers the confirm action
     */
    public boolean isConfirm() {
        return matches(Actions.CONFIRM);
    }

    /**
     * Returns true if this is a "cancel" event (Escape) according to the bindings.
     *
     * @return true if this event triggers the cancel action
     */
    public boolean isCancel() {
        return matches(Actions.CANCEL);
    }

    /**
     * Returns true if this is a "quit" event according to the bindings.
     *
     * @return true if this event triggers the quit action
     */
    public boolean isQuit() {
        return matches(Actions.QUIT);
    }

    /**
     * Returns true if this is a "focus next" event (Tab) according to the bindings.
     *
     * @return true if this event triggers the focus-next action
     */
    public boolean isFocusNext() {
        return matches(Actions.FOCUS_NEXT);
    }

    /**
     * Returns true if this is a "focus previous" event (Shift+Tab) according to the bindings.
     *
     * @return true if this event triggers the focus-previous action
     */
    public boolean isFocusPrevious() {
        return matches(Actions.FOCUS_PREVIOUS);
    }

    /**
     * Returns true if this is a "delete backward" event (Backspace) according to the bindings.
     *
     * @return true if this event triggers the delete-backward action
     */
    public boolean isDeleteBackward() {
        return matches(Actions.DELETE_BACKWARD);
    }

    /**
     * Returns true if this is a "delete forward" event (Delete) according to the bindings.
     *
     * @return true if this event triggers the delete-forward action
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
                && keyCodePoint == keyEvent.keyCodePoint
                && code == keyEvent.code
                && kind == keyEvent.kind
                && modifiers.equals(keyEvent.modifiers)
                && state.equals(keyEvent.state)
                && text.equals(keyEvent.text)
                && shiftedKeyCodePoint.equals(keyEvent.shiftedKeyCodePoint)
                && baseLayoutKeyCodePoint.equals(keyEvent.baseLayoutKeyCodePoint);
    }

    @Override
    public int hashCode() {
        int result = code != null ? code.hashCode() : 0;
        result = 31 * result + modifiers.hashCode();
        result = 31 * result + kind.hashCode();
        result = 31 * result + state.hashCode();
        result = 31 * result + character;
        result = 31 * result + keyCodePoint;
        result = 31 * result + text.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format(
                "KeyEvent[code=%s, modifiers=%s, kind=%s, character=%s, text=%s]",
                code,
                modifiers,
                kind,
                character != 0 ? new String(Character.toChars(character)) : "",
                text
        );
    }

    /**
     * Builder for {@link KeyEvent}.
     */
    public static final class Builder {
        private KeyCode code = KeyCode.CHAR;
        private KeyModifiers modifiers = KeyModifiers.NONE;
        private int character = 0;
        private Bindings bindings = BindingSets.defaults();
        private KeyEventKind kind = KeyEventKind.PRESS;
        private KeyEventState state = KeyEventState.NONE;
        private int keyCodePoint = 0;
        private OptionalInt shiftedKeyCodePoint = OptionalInt.empty();
        private OptionalInt baseLayoutKeyCodePoint = OptionalInt.empty();
        private String text = "";

        private Builder() {
        }

        /**
         * Sets the key code.
         *
         * @param code the key code
         * @return this builder
         */
        public Builder code(KeyCode code) {
            this.code = code;
            return this;
        }

        /**
         * Sets the modifiers.
         *
         * @param modifiers the modifiers
         * @return this builder
         */
        public Builder modifiers(KeyModifiers modifiers) {
            this.modifiers = modifiers;
            return this;
        }

        /**
         * Sets the legacy character code point.
         *
         * @param character the character code point
         * @return this builder
         */
        public Builder character(int character) {
            this.character = character;
            return this;
        }

        /**
         * Sets the bindings.
         *
         * @param bindings the bindings
         * @return this builder
         */
        public Builder bindings(Bindings bindings) {
            this.bindings = bindings;
            return this;
        }

        /**
         * Sets the event kind.
         *
         * @param kind the event kind
         * @return this builder
         */
        public Builder kind(KeyEventKind kind) {
            this.kind = kind;
            return this;
        }

        /**
         * Sets the key state.
         *
         * @param state the key state
         * @return this builder
         */
        public Builder state(KeyEventState state) {
            this.state = state;
            return this;
        }

        /**
         * Sets the logical/unshifted key code point.
         *
         * @param keyCodePoint the logical code point
         * @return this builder
         */
        public Builder keyCodePoint(int keyCodePoint) {
            this.keyCodePoint = keyCodePoint;
            return this;
        }

        /**
         * Sets the shifted key code point.
         *
         * @param shiftedKeyCodePoint the shifted key code point
         * @return this builder
         */
        public Builder shiftedKeyCodePoint(OptionalInt shiftedKeyCodePoint) {
            this.shiftedKeyCodePoint = shiftedKeyCodePoint;
            return this;
        }

        /**
         * Sets the shifted key code point.
         *
         * @param shiftedKeyCodePoint the shifted key code point
         * @return this builder
         */
        public Builder shiftedKeyCodePoint(int shiftedKeyCodePoint) {
            this.shiftedKeyCodePoint = OptionalInt.of(shiftedKeyCodePoint);
            return this;
        }

        /**
         * Sets the base-layout key code point.
         *
         * @param baseLayoutKeyCodePoint the base-layout key code point
         * @return this builder
         */
        public Builder baseLayoutKeyCodePoint(OptionalInt baseLayoutKeyCodePoint) {
            this.baseLayoutKeyCodePoint = baseLayoutKeyCodePoint;
            return this;
        }

        /**
         * Sets the base-layout key code point.
         *
         * @param baseLayoutKeyCodePoint the base-layout key code point
         * @return this builder
         */
        public Builder baseLayoutKeyCodePoint(int baseLayoutKeyCodePoint) {
            this.baseLayoutKeyCodePoint = OptionalInt.of(baseLayoutKeyCodePoint);
            return this;
        }

        /**
         * Sets the produced text.
         *
         * @param text the produced text
         * @return this builder
         */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * Builds the key event.
         *
         * @return the constructed KeyEvent
         */
        public KeyEvent build() {
            return new KeyEvent(
                    code,
                    modifiers,
                    character,
                    bindings,
                    kind,
                    state,
                    keyCodePoint,
                    shiftedKeyCodePoint,
                    baseLayoutKeyCodePoint,
                    text
            );
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import java.util.function.Consumer;
import java.util.function.IntPredicate;

import dev.tamboui.style.Overflow;
import dev.tamboui.text.GraphemeClusters;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.input.TextInputState;

/**
 * Applies key and paste events to a {@link TextInputState} or a {@link TextAreaState}, the way
 * the toolkit's text inputs and text areas edit them.
 * <p>
 * In a {@code TuiRunner} app, pass the events a focused field should get, after checking the keys
 * the app handles itself, such as Enter to submit:
 * <pre>{@code
 * if (event instanceof KeyEvent && ((KeyEvent) event).isConfirm()) {
 *     submit(name.text());
 *     return true;
 * }
 * return TextEditing.handle(name, event);
 * }</pre>
 * <ul>
 *   <li>A typed character, a {@link KeyCode#CHAR} event without Ctrl or Alt, is inserted whole,
 *       also when it is outside the Basic Multilingual Plane. It is inserted even when the
 *       bindings bind it to an action, as vim binds {@code h} and {@code x}: in a text field,
 *       letters are text. A character sequence such as a ZWJ emoji arrives as one event per
 *       code point, and builds up in the text one code point at a time.</li>
 *   <li>Ctrl and Alt characters are never inserted. They edit when the event's bindings bind them
 *       to an editing action, such as the emacs Ctrl+B, Ctrl+F, Ctrl+A, Ctrl+E, Ctrl+H and
 *       Ctrl+D. Otherwise they are left to the app.</li>
 *   <li>{@link Actions#MOVE_LEFT} and {@link Actions#MOVE_RIGHT} move the cursor by one
 *       grapheme cluster, {@link Actions#DELETE_BACKWARD} and {@link Actions#DELETE_FORWARD}
 *       delete one, and {@link Actions#HOME} and {@link Actions#END} move to the start and end.
 *       The Left, Right, Backspace, Delete, Home and End keys always do these, with or
 *       without modifiers.</li>
 *   <li>A {@link PasteEvent} is inserted at the cursor.</li>
 * </ul>
 * Each method returns whether the event was handled, which a {@code TuiRunner} event handler can
 * return to redraw.
 */
public final class TextEditing {

    private TextEditing() {
    }

    /**
     * Applies a key or paste event to a single-line text input.
     * <p>
     * A paste is inserted on one line: its line breaks (CRLF, CR or LF) become spaces, and a line
     * break at its start or end is dropped. Other keys, such as Enter, Tab, Escape or Up, are not
     * handled.
     *
     * @param state the text input state to edit
     * @param event the event to apply
     * @return true if the event was handled
     */
    public static boolean handle(TextInputState state, Event event) {
        return handle(state, event, null);
    }

    /**
     * Applies a key or paste event to a single-line text input, inserting only the characters
     * {@code allowed} accepts, for example {@code Character::isDigit} for a number.
     * <p>
     * A typed character that {@code allowed} rejects is still handled, without being inserted,
     * so it does not reach the app's own keys while the field has the focus. A paste is
     * filtered by grapheme cluster: a cluster is inserted only when {@code allowed} accepts all
     * its code points. Otherwise this works as {@link #handle(TextInputState, Event)}.
     *
     * @param state the text input state to edit
     * @param event the event to apply
     * @param allowed the code points that may be inserted, or null to insert any
     * @return true if the event was handled
     */
    public static boolean handle(TextInputState state, Event event, IntPredicate allowed) {
        if (event instanceof PasteEvent) {
            state.insert(filter(oneLine(((PasteEvent) event).text()), allowed));
            return true;
        }
        if (!(event instanceof KeyEvent)) {
            return false;
        }
        KeyEvent key = (KeyEvent) event;
        if (isTyped(key)) {
            return insertTyped(key, allowed, state::insert);
        }
        if (isAction(key, KeyCode.BACKSPACE, Actions.DELETE_BACKWARD)) {
            state.deleteBackward();
        } else if (isAction(key, KeyCode.DELETE, Actions.DELETE_FORWARD)) {
            state.deleteForward();
        } else if (isAction(key, KeyCode.LEFT, Actions.MOVE_LEFT)) {
            state.moveCursorLeft();
        } else if (isAction(key, KeyCode.RIGHT, Actions.MOVE_RIGHT)) {
            state.moveCursorRight();
        } else if (isAction(key, KeyCode.HOME, Actions.HOME)) {
            state.moveCursorToStart();
        } else if (isAction(key, KeyCode.END, Actions.END)) {
            state.moveCursorToEnd();
        } else {
            return false;
        }
        return true;
    }

    /**
     * Applies a key or paste event to a multi-line text area, moving Up and Down by logical line.
     * <p>
     * Use {@link #handle(TextAreaState, Event, Overflow)} for a text area that wraps its lines.
     *
     * @param state the text area state to edit
     * @param event the event to apply
     * @return true if the event was handled
     */
    public static boolean handle(TextAreaState state, Event event) {
        return handle(state, event, Overflow.CLIP);
    }

    /**
     * Applies a key or paste event to a multi-line text area.
     * <p>
     * Besides the editing described for this class, Enter inserts a line break,
     * {@link Actions#MOVE_UP} and {@link Actions#MOVE_DOWN} (and the Up and Down keys) move the
     * cursor one row, and {@link Actions#HOME} and {@link Actions#END} move to the start and end
     * of the line. With a wrapping {@code overflow}, Up and Down move by the rows on screen, at
     * the width of the text area's last render. A paste keeps its line breaks, with CRLF and CR
     * turned into LF. Tab is not handled, as it moves the focus in the bindings.
     *
     * @param state the text area state to edit
     * @param event the event to apply
     * @param overflow the overflow the text area is rendered with, as given to
     *                 {@code TextArea.Builder.overflow}; null is {@link Overflow#CLIP}
     * @return true if the event was handled
     */
    public static boolean handle(TextAreaState state, Event event, Overflow overflow) {
        if (event instanceof PasteEvent) {
            state.insert(unixLineBreaks(((PasteEvent) event).text()));
            return true;
        }
        if (!(event instanceof KeyEvent)) {
            return false;
        }
        KeyEvent key = (KeyEvent) event;
        if (isTyped(key)) {
            return insertTyped(key, null, state::insert);
        }
        if (key.isKey(KeyCode.ENTER)) {
            state.insert('\n');
        } else if (isAction(key, KeyCode.BACKSPACE, Actions.DELETE_BACKWARD)) {
            state.deleteBackward();
        } else if (isAction(key, KeyCode.DELETE, Actions.DELETE_FORWARD)) {
            state.deleteForward();
        } else if (isAction(key, KeyCode.LEFT, Actions.MOVE_LEFT)) {
            state.moveCursorLeft();
        } else if (isAction(key, KeyCode.RIGHT, Actions.MOVE_RIGHT)) {
            state.moveCursorRight();
        } else if (isAction(key, KeyCode.UP, Actions.MOVE_UP)) {
            state.moveCursorUp(state.lastRenderedWidth(), overflow);
        } else if (isAction(key, KeyCode.DOWN, Actions.MOVE_DOWN)) {
            state.moveCursorDown(state.lastRenderedWidth(), overflow);
        } else if (isAction(key, KeyCode.HOME, Actions.HOME)) {
            state.moveCursorToLineStart();
        } else if (isAction(key, KeyCode.END, Actions.END)) {
            state.moveCursorToLineEnd();
        } else {
            return false;
        }
        return true;
    }

    private static boolean isTyped(KeyEvent key) {
        return key.code() == KeyCode.CHAR && !key.hasCtrl() && !key.hasAlt();
    }

    private static boolean insertTyped(KeyEvent key, IntPredicate allowed, Consumer<String> insert) {
        int codePoint = key.codePoint();
        if (codePoint < 32 || codePoint == 127) {
            return false;
        }
        if (allowed == null || allowed.test(codePoint)) {
            insert.accept(key.string());
        }
        return true;
    }

    private static boolean isAction(KeyEvent key, KeyCode code, String action) {
        return key.code() == code || key.matches(action);
    }

    private static String unixLineBreaks(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String oneLine(String text) {
        String lines = unixLineBreaks(text);
        int start = 0;
        int end = lines.length();
        while (start < end && lines.charAt(start) == '\n') {
            start++;
        }
        while (end > start && lines.charAt(end - 1) == '\n') {
            end--;
        }
        return lines.substring(start, end).replace('\n', ' ');
    }

    private static String filter(String text, IntPredicate allowed) {
        if (allowed == null) {
            return text;
        }
        StringBuilder kept = new StringBuilder(text.length());
        int start = 0;
        while (start < text.length()) {
            int end = GraphemeClusters.clusterEnd(text, start);
            String cluster = text.substring(start, end);
            if (cluster.codePoints().allMatch(allowed)) {
                kept.append(cluster);
            }
            start = end;
        }
        return kept.toString();
    }
}

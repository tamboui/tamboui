/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import dev.tamboui.terminal.Backend;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;

import java.io.IOException;

/**
 * Parses raw terminal input into typed {@link Event} objects.
 * <p>
 * Handles escape sequences for arrow keys, function keys, navigation keys,
 * and mouse events (SGR extended mode).
 */
public final class EventParser {

    private static final int ESC = 27;
    private static final int PEEK_TIMEOUT = 50;

    private EventParser() {}

    /**
     * Reads and parses the next event from the backend using the default bindings.
     *
     * @param backend the terminal backend
     * @param timeout timeout in milliseconds for the initial read
     * @return the parsed event, or null if no event was available
     * @throws IOException if an I/O error occurs
     */
    public static Event readEvent(Backend backend, int timeout) throws IOException {
        return readEvent(backend, timeout, BindingSets.defaults());
    }

    /**
     * Reads and parses the next event from the backend.
     *
     * @param backend  the terminal backend
     * @param timeout  timeout in milliseconds for the initial read
     * @param bindings the bindings for event semantic action matching
     * @return the parsed event, or null if no event was available
     * @throws IOException if an I/O error occurs
     */
    public static Event readEvent(Backend backend, int timeout, Bindings bindings) throws IOException {
        int c = backend.read(timeout);

        if (c == -2) {
            // Timeout - no input available
            return null;
        }

        if (c == -1) {
            // EOF
            return null;
        }

        return parseInput(c, backend, bindings);
    }

    private static Event parseInput(int c, Backend backend, Bindings bindings) throws IOException {
        if (c == ESC) {
            return parseEscapeSequence(backend, bindings);
        }

        // Control characters
        if (c < 32) {
            return parseControlChar(c, bindings);
        }

        // Regular printable character
        if (c < 127) {
            return KeyEvent.ofChar((char) c, bindings);
        }

        // DEL key
        if (c == 127) {
            return KeyEvent.ofKey(KeyCode.BACKSPACE, bindings);
        }

        // Extended ASCII / UTF-8 - treat as character
        return KeyEvent.ofChar((char) c, bindings);
    }

    private static Event parseControlChar(int c, Bindings bindings) {
        switch (c) {
            case 3:
                return KeyEvent.ofChar('c', KeyModifiers.CTRL, bindings);  // Ctrl+C
            case 9:
                return KeyEvent.ofKey(KeyCode.TAB, bindings);               // Tab
            case 10:
            case 13:
                return KeyEvent.ofKey(KeyCode.ENTER, bindings);        // Enter (LF or CR)
            case 27:
                return KeyEvent.ofKey(KeyCode.ESCAPE, bindings);           // Escape (standalone)
            default:
                if (c >= 1 && c <= 26) {
                    char letter = (char) ('a' + c - 1);
                    return KeyEvent.ofChar(letter, KeyModifiers.CTRL, bindings);
                }
                return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }
    }

    private static Event parseEscapeSequence(Backend backend, Bindings bindings) throws IOException {
        int next = backend.peek(PEEK_TIMEOUT);

        if (next == -2 || next == -1) {
            // Standalone ESC key
            return KeyEvent.ofKey(KeyCode.ESCAPE, bindings);
        }

        if (next == '[') {
            backend.read(PEEK_TIMEOUT); // consume '['
            return parseCSI(backend, bindings);
        }

        if (next == 'O') {
            backend.read(PEEK_TIMEOUT); // consume 'O'
            return parseSS3(backend, bindings);
        }

        // Alt+key
        backend.read(PEEK_TIMEOUT); // consume the character
        if (next >= 'a' && next <= 'z') {
            return KeyEvent.ofChar((char) next, KeyModifiers.ALT, bindings);
        }
        if (next >= 'A' && next <= 'Z') {
            return KeyEvent.ofChar((char) next, KeyModifiers.of(false, true, true), bindings); // Alt+Shift
        }

        return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
    }

    private static Event parseCSI(Backend backend, Bindings bindings) throws IOException {
        int c = backend.read(PEEK_TIMEOUT);
        if (c == -2 || c == -1) {
            return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }

        // Check for mouse event (SGR extended mode: ESC [ < ...)
        if (c == '<') {
            return parseMouseSGR(backend, bindings);
        }

        // Arrow keys and simple sequences
        switch (c) {
            case 'A':
                return KeyEvent.ofKey(KeyCode.UP, bindings);
            case 'B':
                return KeyEvent.ofKey(KeyCode.DOWN, bindings);
            case 'C':
                return KeyEvent.ofKey(KeyCode.RIGHT, bindings);
            case 'D':
                return KeyEvent.ofKey(KeyCode.LEFT, bindings);
            case 'H':
                return KeyEvent.ofKey(KeyCode.HOME, bindings);
            case 'F':
                return KeyEvent.ofKey(KeyCode.END, bindings);
            default:
                return parseExtendedCSI(c, backend, bindings);
        }
    }

    private static Event parseExtendedCSI(int first, Backend backend, Bindings bindings) throws IOException {
        // Parse numeric parameter(s)
        StringBuilder sb = new StringBuilder();
        sb.append((char) first);

        int c;
        while ((c = backend.read(PEEK_TIMEOUT)) != -2 && c != -1) {
            if (c >= '0' && c <= '9' || c == ';') {
                sb.append((char) c);
            } else {
                // End of sequence
                return parseCSIWithParams(sb.toString(), c, bindings);
            }
        }

        return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
    }

    private static Event parseCSIWithParams(String params, int terminator, Bindings bindings) {
        // Parse sequences like "1~" (Home), "4~" (End), "5~" (PgUp), etc.
        if (terminator == '~') {
            return parseVT(params, bindings);
        }

        // Parse sequences with modifiers like "1;5A" (Ctrl+Up)
        if (terminator >= 'A' && terminator <= 'Z') {
            return parseModifiedArrow(params, terminator, bindings);
        }

        return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
    }

    private static Event parseVT(String params, Bindings bindings) {
        String[] parts = params.split(";");
        int code;
        try {
            code = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }

        KeyModifiers mods = parts.length > 1 ? parseModifierCode(parts[1]) : KeyModifiers.NONE;

        switch (code) {
            case 1:
                return KeyEvent.ofKey(KeyCode.HOME, mods, bindings);
            case 2:
                return KeyEvent.ofKey(KeyCode.INSERT, mods, bindings);
            case 3:
                return KeyEvent.ofKey(KeyCode.DELETE, mods, bindings);
            case 4:
                return KeyEvent.ofKey(KeyCode.END, mods, bindings);
            case 5:
                return KeyEvent.ofKey(KeyCode.PAGE_UP, mods, bindings);
            case 6:
                return KeyEvent.ofKey(KeyCode.PAGE_DOWN, mods, bindings);
            case 11:
                return KeyEvent.ofKey(KeyCode.F1, mods, bindings);
            case 12:
                return KeyEvent.ofKey(KeyCode.F2, mods, bindings);
            case 13:
                return KeyEvent.ofKey(KeyCode.F3, mods, bindings);
            case 14:
                return KeyEvent.ofKey(KeyCode.F4, mods, bindings);
            case 15:
                return KeyEvent.ofKey(KeyCode.F5, mods, bindings);
            case 17:
                return KeyEvent.ofKey(KeyCode.F6, mods, bindings);
            case 18:
                return KeyEvent.ofKey(KeyCode.F7, mods, bindings);
            case 19:
                return KeyEvent.ofKey(KeyCode.F8, mods, bindings);
            case 20:
                return KeyEvent.ofKey(KeyCode.F9, mods, bindings);
            case 21:
                return KeyEvent.ofKey(KeyCode.F10, mods, bindings);
            case 23:
                return KeyEvent.ofKey(KeyCode.F11, mods, bindings);
            case 24:
                return KeyEvent.ofKey(KeyCode.F12, mods, bindings);
            default:
                return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }
    }

    private static Event parseModifiedArrow(String params, int terminator, Bindings bindings) {
        String[] parts = params.split(";");
        KeyModifiers mods = parts.length > 1 ? parseModifierCode(parts[1]) : KeyModifiers.NONE;

        KeyCode code;
        switch (terminator) {
            case 'A':
                code = KeyCode.UP;
                break;
            case 'B':
                code = KeyCode.DOWN;
                break;
            case 'C':
                code = KeyCode.RIGHT;
                break;
            case 'D':
                code = KeyCode.LEFT;
                break;
            case 'H':
                code = KeyCode.HOME;
                break;
            case 'F':
                code = KeyCode.END;
                break;
            default:
                code = KeyCode.UNKNOWN;
                break;
        }

        return KeyEvent.ofKey(code, mods, bindings);
    }

    private static KeyModifiers parseModifierCode(String code) {
        int mod;
        try {
            mod = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return KeyModifiers.NONE;
        }

        // Modifier encoding: 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0)
        mod = mod - 1;
        boolean shift = (mod & 1) != 0;
        boolean alt = (mod & 2) != 0;
        boolean ctrl = (mod & 4) != 0;

        return KeyModifiers.of(ctrl, alt, shift);
    }

    private static Event parseSS3(Backend backend, Bindings bindings) throws IOException {
        int c = backend.read(PEEK_TIMEOUT);
        if (c == -2 || c == -1) {
            return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }

        // SS3 sequences (typically function keys on some terminals)
        switch (c) {
            case 'P':
                return KeyEvent.ofKey(KeyCode.F1, bindings);
            case 'Q':
                return KeyEvent.ofKey(KeyCode.F2, bindings);
            case 'R':
                return KeyEvent.ofKey(KeyCode.F3, bindings);
            case 'S':
                return KeyEvent.ofKey(KeyCode.F4, bindings);
            case 'A':
                return KeyEvent.ofKey(KeyCode.UP, bindings);
            case 'B':
                return KeyEvent.ofKey(KeyCode.DOWN, bindings);
            case 'C':
                return KeyEvent.ofKey(KeyCode.RIGHT, bindings);
            case 'D':
                return KeyEvent.ofKey(KeyCode.LEFT, bindings);
            case 'H':
                return KeyEvent.ofKey(KeyCode.HOME, bindings);
            case 'F':
                return KeyEvent.ofKey(KeyCode.END, bindings);
            default:
                return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }
    }

    private static Event parseMouseSGR(Backend backend, Bindings bindings) throws IOException {
        // SGR mouse format: ESC [ < Cb ; Cx ; Cy M/m
        // where Cb is button code, Cx is column, Cy is row
        // M = press/drag, m = release

        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = backend.read(PEEK_TIMEOUT)) != -2 && c != -1) {
            if (c == 'M' || c == 'm') {
                return parseMouseParams(sb.toString(), c == 'm', bindings);
            }
            sb.append((char) c);
        }

        // Return UNKNOWN for incomplete mouse sequence
        return KeyEvent.ofKey(KeyCode.UNKNOWN);
    }

    private static Event parseMouseParams(String params, boolean isRelease, Bindings bindings) {
        String[] parts = params.split(";");
        if (parts.length < 3) {
            return KeyEvent.ofKey(KeyCode.UNKNOWN);
        }

        int buttonCode;
        int x;
        int y;
        try {
            buttonCode = Integer.parseInt(parts[0]);
            x = Integer.parseInt(parts[1]) - 1; // Convert to 0-indexed
            y = Integer.parseInt(parts[2]) - 1;
        } catch (NumberFormatException e) {
            return KeyEvent.ofKey(KeyCode.UNKNOWN);
        }

        // Parse modifiers from button code
        boolean shift = (buttonCode & 4) != 0;
        boolean alt = (buttonCode & 8) != 0;
        boolean ctrl = (buttonCode & 16) != 0;
        KeyModifiers mods = KeyModifiers.of(ctrl, alt, shift);

        // Clear modifier bits to get actual button
        int button = buttonCode & ~(4 | 8 | 16);

        // Determine event kind and button
        if (button >= 64 && button <= 65) {
            // Scroll wheel
            MouseEventKind kind = (button == 64) ? MouseEventKind.SCROLL_UP : MouseEventKind.SCROLL_DOWN;
            return new MouseEvent(kind, MouseButton.NONE, x, y, mods, bindings);
        }

        boolean isDrag = (button & 32) != 0;
        button = button & ~32;

        MouseButton mouseButton;
        switch (button) {
            case 0:
                mouseButton = MouseButton.LEFT;
                break;
            case 1:
                mouseButton = MouseButton.MIDDLE;
                break;
            case 2:
                mouseButton = MouseButton.RIGHT;
                break;
            default:
                mouseButton = MouseButton.NONE;
                break;
        }

        MouseEventKind kind;
        if (isRelease) {
            kind = MouseEventKind.RELEASE;
        } else if (isDrag) {
            kind = MouseEventKind.DRAG;
        } else if (mouseButton == MouseButton.NONE) {
            kind = MouseEventKind.MOVE;
        } else {
            kind = MouseEventKind.PRESS;
        }

        return new MouseEvent(kind, mouseButton, x, y, mods, bindings);
    }
}

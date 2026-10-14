/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.locks.LockSupport;

import dev.tamboui.terminal.Backend;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;

/**
 * Parses raw terminal input into typed {@link Event} objects.
 * <p>
 * Handles escape sequences for arrow keys, function keys, navigation keys,
 * and mouse events (SGR extended mode).
 */
public final class EventParser {

    private static final int ESC = 27;
    private static final int PEEK_TIMEOUT = 50;
    private static final String PASTE_END = "\u001b[201~";

    private EventParser() {
    }

    /**
     * Reads and parses the next event from the backend using the default bindings.
     *
     * @param backend the terminal backend
     * @param timeout timeout in milliseconds for the initial read
     * @return the parsed event, or null if no event was available
     * @throws EOFException if terminal input ends
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
     * @throws EOFException if terminal input ends
     * @throws IOException if an I/O error occurs
     */
    public static Event readEvent(Backend backend, int timeout, Bindings bindings) throws IOException {
        int c = backend.read(timeout);

        if (c == -2) {
            // Timeout - no input available
            return null;
        }

        if (c == -1) {
            throw new EOFException("Terminal input closed");
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

        // DEL key
        if (c == 127) {
            return KeyEvent.ofKey(KeyCode.BACKSPACE, bindings);
        }

        // Any printable character (ASCII or full Unicode code point from backend)
        return KeyEvent.ofChar(c, bindings);
    }

    private static KeyEvent parseControlChar(int c, Bindings bindings) {
        switch (c) {
            case 0:
                return KeyEvent.ofChar(' ', KeyModifiers.CTRL, bindings); // Ctrl+Space (NUL)
            case 3:
                return KeyEvent.ofChar('c', KeyModifiers.CTRL, bindings); // Ctrl+C
            case 8:
                return KeyEvent.ofKey(KeyCode.BACKSPACE, bindings); // Backspace (BS on Windows)
            case 9:
                return KeyEvent.ofKey(KeyCode.TAB, bindings); // Tab
            case 10:
            case 13:
                return KeyEvent.ofKey(KeyCode.ENTER, bindings); // Enter (LF or CR)
            case 27:
                return KeyEvent.ofKey(KeyCode.ESCAPE, bindings); // Escape (standalone)
            case 28:
                return KeyEvent.ofChar('\\', KeyModifiers.CTRL, bindings); // Ctrl+\ (FS)
            case 29:
                return KeyEvent.ofChar(']', KeyModifiers.CTRL, bindings); // Ctrl+] (GS)
            case 30:
                return KeyEvent.ofChar('^', KeyModifiers.CTRL, bindings); // Ctrl+^ (RS)
            case 31:
                return KeyEvent.ofChar('/', KeyModifiers.CTRL, bindings); // Ctrl+/ (US)
            default:
                if (c >= 1 && c <= 26) {
                    char letter = (char) ('a' + c - 1);
                    return KeyEvent.ofChar(letter, KeyModifiers.CTRL, bindings);
                }
                return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }
    }

    private static KeyEvent withAlt(KeyEvent event, Bindings bindings) {
        KeyModifiers modifiers = KeyModifiers.of(event.hasCtrl(), true, event.hasShift());
        if (event.code() == KeyCode.CHAR) {
            return KeyEvent.ofChar(event.codePoint(), modifiers, bindings);
        }
        return KeyEvent.ofKey(event.code(), modifiers, bindings);
    }

    private static Event parseEscapeSequence(Backend backend, Bindings bindings) throws IOException {
        int next = backend.peek(PEEK_TIMEOUT);

        if (next == -2 || next == -1) {
            // Standalone ESC key
            return KeyEvent.ofKey(KeyCode.ESCAPE, bindings);
        }

        if (next == ESC) {
            // The second ESC starts the next key: an Escape of its own, or a sequence such as ESC [ A
            // (Alt+Up from a terminal that sends Alt as an ESC prefix, or Escape followed quickly by Up).
            // Leave it unread for the next event, so the rest of the sequence is not typed as text.
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

        if (next > 0 && next < 32) {
            // Alt with a control key, sent as an ESC prefix: ESC CR is Alt+Enter, ESC 0x01 is Ctrl+Alt+A
            backend.read(PEEK_TIMEOUT); // consume the control character
            return withAlt(parseControlChar(next, bindings), bindings);
        }

        // Alt+key
        backend.read(PEEK_TIMEOUT); // consume the character
        if (next >= 32 && next != 127) {
            return KeyEvent.ofChar(next, KeyModifiers.ALT, bindings);
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
            case 'Z':
                // Shift+Tab (backtab) - ESC[Z
                return KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, bindings);
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
                return parseCSIWithParams(sb.toString(), c, backend, bindings);
            }
        }

        return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
    }

    private static Event parseCSIWithParams(String params, int terminator, Backend backend, Bindings bindings) throws IOException {
        // Bracketed paste start: ESC[200~
        if (terminator == '~' && "200".equals(params)) {
            return readPasteContent(backend);
        }

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

    private static Event readPasteContent(Backend backend) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedIOException("Interrupted while reading bracketed paste");
            }
            int c = backend.read(PEEK_TIMEOUT);
            if (c == -1) {
                throw new EOFException("Terminal input closed during bracketed paste");
            }
            if (c == -2) {
                // A backend may return early; avoid spinning until the next chunk arrives.
                LockSupport.parkNanos(1_000_000L);
                continue;
            }
            sb.appendCodePoint(c);
            if (endsWith(sb, PASTE_END)) {
                sb.setLength(sb.length() - PASTE_END.length());
                return new PasteEvent(sb.toString());
            }
        }
    }

    private static boolean endsWith(StringBuilder sb, String suffix) {
        int offset = sb.length() - suffix.length();
        if (offset < 0) {
            return false;
        }
        for (int i = 0; i < suffix.length(); i++) {
            if (sb.charAt(offset + i) != suffix.charAt(i)) {
                return false;
            }
        }
        return true;
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
            case 'P':
                code = KeyCode.F1;
                break;
            case 'Q':
                code = KeyCode.F2;
                break;
            case 'R':
                code = KeyCode.F3;
                break;
            case 'S':
                code = KeyCode.F4;
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
        if (button >= 64 && button <= 67) {
            // Scroll wheel: 64=up, 65=down, 66=left, 67=right
            MouseEventKind kind;
            switch (button) {
                case 64:
                    kind = MouseEventKind.SCROLL_UP;
                    break;
                case 65:
                    kind = MouseEventKind.SCROLL_DOWN;
                    break;
                case 66:
                    kind = MouseEventKind.SCROLL_LEFT;
                    break;
                default:
                    kind = MouseEventKind.SCROLL_RIGHT;
                    break;
            }
            return new MouseEvent(kind, MouseButton.NONE, x, y, mods, bindings);
        }

        boolean motion = (button & 32) != 0;
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
        } else if (motion && mouseButton == MouseButton.NONE) {
            // Motion with no button held is a hover (any-event tracking, DECSET 1003)
            kind = MouseEventKind.MOVE;
        } else if (motion) {
            // Motion with a button held is a drag
            kind = MouseEventKind.DRAG;
        } else {
            kind = MouseEventKind.PRESS;
        }

        return new MouseEvent(kind, mouseButton, x, y, mods, bindings);
    }
}

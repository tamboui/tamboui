/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.OptionalInt;
import java.util.concurrent.locks.LockSupport;

import dev.tamboui.terminal.Backend;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;

/**
 * Parses raw terminal input into typed {@link Event} objects.
 * <p>
 * Handles escape sequences for standard terminal input, mouse events (SGR mode),
 * and progressive keyboard enhancements (Kitty keyboard protocol {@code CSI u}).
 */
public final class EventParser {

    private static final int ESC = 27;
    private static final int PEEK_TIMEOUT = 50;
    private static final int MAX_CSI_LENGTH = 128;
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
     * @throws IOException  if an I/O error occurs
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
     * @throws IOException  if an I/O error occurs
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

    private static Event parseControlChar(int c, Bindings bindings) {
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

        // Single-character terminators without parameters
        if (c >= 0x40 && c <= 0x7E) {
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
                case 'E':
                    return KeyEvent.ofKey(KeyCode.KP_BEGIN, bindings);
                case 'Z':
                    // Shift+Tab (backtab) - ESC[Z
                    return KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, bindings);
                default:
                    return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
            }
        }

        // Sequence with parameters: collect atomically until final terminator byte (0x40..0x7E)
        StringBuilder sb = new StringBuilder();
        sb.append((char) c);
        int terminator = -1;

        while (sb.length() < MAX_CSI_LENGTH) {
            int next = backend.read(PEEK_TIMEOUT);
            if (next == -1 || next == -2) {
                break;
            }
            if (next >= 0x40 && next <= 0x7E) {
                terminator = next;
                break;
            }
            sb.append((char) next);
        }

        if (terminator == -1) {
            // Drain remaining bytes of oversized sequence up to terminator or EOF
            while (true) {
                int next = backend.read(PEEK_TIMEOUT);
                if (next == -1 || next == -2) {
                    break;
                }
                if (next >= 0x40 && next <= 0x7E) {
                    break;
                }
            }
            return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }

        return parseCSIWithParams(sb.toString(), terminator, backend, bindings);
    }

    private static Event parseCSIWithParams(String params, int terminator, Backend backend, Bindings bindings) throws IOException {
        // Bracketed paste start: ESC[200~
        if (terminator == '~' && "200".equals(params)) {
            return readPasteContent(backend);
        }

        // Kitty CSI u sequence: ESC [ <params> u
        if (terminator == 'u') {
            return parseKittyCsiU(params, bindings);
        }

        // Parse sequences like "1~" (Home), "4~" (End), "5~" (PgUp), etc.
        if (terminator == '~') {
            return parseVT(params, bindings);
        }

        // Parse sequences with modifiers like "1;5A" (Ctrl+Up) or "1;2P" (Shift+F1)
        if ((terminator >= 'A' && terminator <= 'Z') || terminator == 'E') {
            return parseModifiedKey(params, terminator, bindings);
        }

        return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
    }

    private static Event parseKittyCsiU(String params, Bindings bindings) {
        if (params.startsWith("?")) {
            // Kitty mode query response (e.g. CSI ? 5 u) - consumed, ignore
            return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }

        String[] fields = params.split(";", -1);
        String field1 = fields.length > 0 ? fields[0] : "";
        String field2 = fields.length > 1 ? fields[1] : "";
        String field3 = fields.length > 2 ? fields[2] : "";

        String[] keyParts = field1.split(":", -1);
        int unicodeKeyCode;
        try {
            unicodeKeyCode = Integer.parseInt(keyParts[0]);
        } catch (NumberFormatException e) {
            return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }

        OptionalInt shiftedKeyCode = OptionalInt.empty();
        if (keyParts.length > 1 && !keyParts[1].isEmpty()) {
            try {
                shiftedKeyCode = OptionalInt.of(Integer.parseInt(keyParts[1]));
            } catch (NumberFormatException ignored) {
            }
        }

        OptionalInt baseLayoutKeyCode = OptionalInt.empty();
        if (keyParts.length > 2 && !keyParts[2].isEmpty()) {
            try {
                baseLayoutKeyCode = OptionalInt.of(Integer.parseInt(keyParts[2]));
            } catch (NumberFormatException ignored) {
            }
        }

        ParsedModifiers pm = parseModifierAndKind(field2);

        String text = "";
        String textField = "";
        for (int i = 2; i < fields.length; i++) {
            if (!fields[i].isEmpty()) {
                textField = fields[i];
                break;
            }
        }
        if (!textField.isEmpty()) {
            String[] textParts = textField.split(":", -1);
            StringBuilder textSb = new StringBuilder();
            for (String cpStr : textParts) {
                if (!cpStr.isEmpty()) {
                    try {
                        int cp = Integer.parseInt(cpStr);
                        textSb.append(Character.toChars(cp));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            text = textSb.toString();
        }

        KeyCode code;
        int character = 0;
        int keyCodePoint = unicodeKeyCode;

        if (unicodeKeyCode == 0) {
            code = KeyCode.CHAR;
        } else if (unicodeKeyCode == 27) {
            code = KeyCode.ESCAPE;
        } else if (unicodeKeyCode == 13) {
            code = KeyCode.ENTER;
        } else if (unicodeKeyCode == 9) {
            code = KeyCode.TAB;
        } else if (unicodeKeyCode == 127) {
            code = KeyCode.BACKSPACE;
        } else {
            code = mapKittyFunctionalKey(unicodeKeyCode);
            if (code == null) {
                if (unicodeKeyCode >= 32) {
                    code = KeyCode.CHAR;
                    character = unicodeKeyCode;
                } else {
                    code = KeyCode.UNKNOWN;
                }
            }
        }

        boolean keypad = isKeypadKeyCode(code);
        KeyEventState state = KeyEventState.of(pm.capsLock, pm.numLock, keypad);

        if (text.isEmpty()) {
            if (code == KeyCode.CHAR && !pm.modifiers.ctrl() && !pm.modifiers.alt()) {
                if (shiftedKeyCode.isPresent() && pm.modifiers.shift()) {
                    text = new String(Character.toChars(shiftedKeyCode.getAsInt()));
                    character = shiftedKeyCode.getAsInt();
                } else if (character != 0) {
                    text = new String(Character.toChars(character));
                }
            }
        } else if (code == KeyCode.CHAR && character == 0) {
            if (text.codePointCount(0, text.length()) == 1) {
                character = text.codePointAt(0);
            }
        }

        return new KeyEvent(
                code,
                pm.modifiers,
                character,
                bindings,
                pm.kind,
                state,
                keyCodePoint,
                shiftedKeyCode,
                baseLayoutKeyCode,
                text
        );
    }

    private static KeyCode mapKittyFunctionalKey(int code) {
        switch (code) {
            case 57358: return KeyCode.CAPS_LOCK;
            case 57359: return KeyCode.SCROLL_LOCK;
            case 57360: return KeyCode.NUM_LOCK;
            case 57361: return KeyCode.PRINT_SCREEN;
            case 57362: return KeyCode.PAUSE;
            case 57363: return KeyCode.MENU;
            case 57364: return KeyCode.F1;
            case 57365: return KeyCode.F2;
            case 57366: return KeyCode.F3;
            case 57367: return KeyCode.F4;
            case 57368: return KeyCode.F5;
            case 57369: return KeyCode.F6;
            case 57370: return KeyCode.F7;
            case 57371: return KeyCode.F8;
            case 57372: return KeyCode.F9;
            case 57373: return KeyCode.F10;
            case 57374: return KeyCode.F11;
            case 57375: return KeyCode.F12;
            case 57376: return KeyCode.F13;
            case 57377: return KeyCode.F14;
            case 57378: return KeyCode.F15;
            case 57379: return KeyCode.F16;
            case 57380: return KeyCode.F17;
            case 57381: return KeyCode.F18;
            case 57382: return KeyCode.F19;
            case 57383: return KeyCode.F20;
            case 57384: return KeyCode.F21;
            case 57385: return KeyCode.F22;
            case 57386: return KeyCode.F23;
            case 57387: return KeyCode.F24;
            case 57388: return KeyCode.F25;
            case 57389: return KeyCode.F26;
            case 57390: return KeyCode.F27;
            case 57391: return KeyCode.F28;
            case 57392: return KeyCode.F29;
            case 57393: return KeyCode.F30;
            case 57394: return KeyCode.F31;
            case 57395: return KeyCode.F32;
            case 57396: return KeyCode.F33;
            case 57397: return KeyCode.F34;
            case 57398: return KeyCode.F35;
            case 57399: return KeyCode.KP_0;
            case 57400: return KeyCode.KP_1;
            case 57401: return KeyCode.KP_2;
            case 57402: return KeyCode.KP_3;
            case 57403: return KeyCode.KP_4;
            case 57404: return KeyCode.KP_5;
            case 57405: return KeyCode.KP_6;
            case 57406: return KeyCode.KP_7;
            case 57407: return KeyCode.KP_8;
            case 57408: return KeyCode.KP_9;
            case 57409: return KeyCode.KP_DECIMAL;
            case 57410: return KeyCode.KP_DIVIDE;
            case 57411: return KeyCode.KP_MULTIPLY;
            case 57412: return KeyCode.KP_SUBTRACT;
            case 57413: return KeyCode.KP_ADD;
            case 57414: return KeyCode.KP_ENTER;
            case 57415: return KeyCode.KP_EQUAL;
            case 57416: return KeyCode.KP_SEPARATOR;
            case 57417: return KeyCode.KP_LEFT;
            case 57418: return KeyCode.KP_RIGHT;
            case 57419: return KeyCode.KP_UP;
            case 57420: return KeyCode.KP_DOWN;
            case 57421: return KeyCode.KP_PAGE_UP;
            case 57422: return KeyCode.KP_PAGE_DOWN;
            case 57423: return KeyCode.KP_HOME;
            case 57424: return KeyCode.KP_END;
            case 57425: return KeyCode.KP_INSERT;
            case 57426: return KeyCode.KP_DELETE;
            case 57427: return KeyCode.KP_BEGIN;
            case 57428: return KeyCode.MEDIA_PLAY;
            case 57429: return KeyCode.MEDIA_PAUSE;
            case 57430: return KeyCode.MEDIA_PLAY_PAUSE;
            case 57431: return KeyCode.MEDIA_REVERSE;
            case 57432: return KeyCode.MEDIA_STOP;
            case 57433: return KeyCode.MEDIA_FAST_FORWARD;
            case 57434: return KeyCode.MEDIA_REWIND;
            case 57435: return KeyCode.MEDIA_TRACK_NEXT;
            case 57436: return KeyCode.MEDIA_TRACK_PREVIOUS;
            case 57437: return KeyCode.MEDIA_RECORD;
            case 57438: return KeyCode.LOWER_VOLUME;
            case 57439: return KeyCode.RAISE_VOLUME;
            case 57440: return KeyCode.MUTE_VOLUME;
            case 57441: return KeyCode.LEFT_SHIFT;
            case 57442: return KeyCode.LEFT_CONTROL;
            case 57443: return KeyCode.LEFT_ALT;
            case 57444: return KeyCode.LEFT_SUPER;
            case 57445: return KeyCode.LEFT_HYPER;
            case 57446: return KeyCode.LEFT_META;
            case 57447: return KeyCode.RIGHT_SHIFT;
            case 57448: return KeyCode.RIGHT_CONTROL;
            case 57449: return KeyCode.RIGHT_ALT;
            case 57450: return KeyCode.RIGHT_SUPER;
            case 57451: return KeyCode.RIGHT_HYPER;
            case 57452: return KeyCode.RIGHT_META;
            case 57453: return KeyCode.ISO_LEVEL3_SHIFT;
            case 57454: return KeyCode.ISO_LEVEL5_SHIFT;
            default: return null;
        }
    }

    private static boolean isKeypadKeyCode(KeyCode code) {
        if (code == null) {
            return false;
        }
        switch (code) {
            case KP_0:
            case KP_1:
            case KP_2:
            case KP_3:
            case KP_4:
            case KP_5:
            case KP_6:
            case KP_7:
            case KP_8:
            case KP_9:
            case KP_DECIMAL:
            case KP_DIVIDE:
            case KP_MULTIPLY:
            case KP_SUBTRACT:
            case KP_ADD:
            case KP_ENTER:
            case KP_EQUAL:
            case KP_SEPARATOR:
            case KP_LEFT:
            case KP_RIGHT:
            case KP_UP:
            case KP_DOWN:
            case KP_PAGE_UP:
            case KP_PAGE_DOWN:
            case KP_HOME:
            case KP_END:
            case KP_INSERT:
            case KP_DELETE:
            case KP_BEGIN:
                return true;
            default:
                return false;
        }
    }

    private static final class ParsedModifiers {
        final KeyModifiers modifiers;
        final KeyEventKind kind;
        final boolean capsLock;
        final boolean numLock;

        ParsedModifiers(KeyModifiers modifiers, KeyEventKind kind, boolean capsLock, boolean numLock) {
            this.modifiers = modifiers;
            this.kind = kind;
            this.capsLock = capsLock;
            this.numLock = numLock;
        }
    }

    private static ParsedModifiers parseModifierAndKind(String modString) {
        if (modString == null || modString.isEmpty()) {
            return new ParsedModifiers(KeyModifiers.NONE, KeyEventKind.PRESS, false, false);
        }
        String[] parts = modString.split(":", -1);
        int mod = 0;
        try {
            mod = Integer.parseInt(parts[0]) - 1;
        } catch (NumberFormatException ignored) {
        }

        boolean shift = (mod & 1) != 0;
        boolean alt = (mod & 2) != 0;
        boolean ctrl = (mod & 4) != 0;
        boolean superKey = (mod & 8) != 0;
        boolean hyper = (mod & 16) != 0;
        boolean meta = (mod & 32) != 0;
        boolean capsLock = (mod & 64) != 0;
        boolean numLock = (mod & 128) != 0;
        KeyModifiers modifiers = KeyModifiers.of(ctrl, alt, shift, superKey, hyper, meta);

        KeyEventKind kind = KeyEventKind.PRESS;
        if (parts.length > 1 && !parts[1].isEmpty()) {
            try {
                int eventType = Integer.parseInt(parts[1]);
                switch (eventType) {
                    case 1:
                        kind = KeyEventKind.PRESS;
                        break;
                    case 2:
                        kind = KeyEventKind.REPEAT;
                        break;
                    case 3:
                        kind = KeyEventKind.RELEASE;
                        break;
                    default:
                        kind = KeyEventKind.PRESS;
                        break;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return new ParsedModifiers(modifiers, kind, capsLock, numLock);
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
        String[] parts = params.split(";", -1);
        int code;
        try {
            code = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return KeyEvent.ofKey(KeyCode.UNKNOWN, bindings);
        }

        ParsedModifiers pm = parts.length > 1
                ? parseModifierAndKind(parts[1])
                : new ParsedModifiers(KeyModifiers.NONE, KeyEventKind.PRESS, false, false);

        KeyCode keyCode;
        switch (code) {
            case 1:
            case 7:
                keyCode = KeyCode.HOME;
                break;
            case 2:
                keyCode = KeyCode.INSERT;
                break;
            case 3:
                keyCode = KeyCode.DELETE;
                break;
            case 4:
            case 8:
                keyCode = KeyCode.END;
                break;
            case 5:
                keyCode = KeyCode.PAGE_UP;
                break;
            case 6:
                keyCode = KeyCode.PAGE_DOWN;
                break;
            case 11:
                keyCode = KeyCode.F1;
                break;
            case 12:
                keyCode = KeyCode.F2;
                break;
            case 13:
                keyCode = KeyCode.F3;
                break;
            case 14:
                keyCode = KeyCode.F4;
                break;
            case 15:
                keyCode = KeyCode.F5;
                break;
            case 17:
                keyCode = KeyCode.F6;
                break;
            case 18:
                keyCode = KeyCode.F7;
                break;
            case 19:
                keyCode = KeyCode.F8;
                break;
            case 20:
                keyCode = KeyCode.F9;
                break;
            case 21:
                keyCode = KeyCode.F10;
                break;
            case 23:
                keyCode = KeyCode.F11;
                break;
            case 24:
                keyCode = KeyCode.F12;
                break;
            case 29:
                keyCode = KeyCode.MENU;
                break;
            case 57427:
                keyCode = KeyCode.KP_BEGIN;
                break;
            default:
                keyCode = KeyCode.UNKNOWN;
                break;
        }

        boolean keypad = isKeypadKeyCode(keyCode);
        KeyEventState state = KeyEventState.of(pm.capsLock, pm.numLock, keypad);
        return new KeyEvent(
                keyCode,
                pm.modifiers,
                0,
                bindings,
                pm.kind,
                state,
                0,
                OptionalInt.empty(),
                OptionalInt.empty(),
                ""
        );
    }

    private static Event parseModifiedKey(String params, int terminator, Bindings bindings) {
        String[] parts = params.split(";", -1);
        ParsedModifiers pm = parts.length > 1
                ? parseModifierAndKind(parts[1])
                : new ParsedModifiers(KeyModifiers.NONE, KeyEventKind.PRESS, false, false);

        KeyCode code;
        KeyModifiers mods = pm.modifiers;

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
            case 'E':
                code = KeyCode.KP_BEGIN;
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
            case 'Z':
                code = KeyCode.TAB;
                mods = mods.shift()
                        ? mods
                        : KeyModifiers.of(mods.ctrl(), mods.alt(), true, mods.superKey(), mods.hyper(), mods.meta());
                break;
            default:
                code = KeyCode.UNKNOWN;
                break;
        }

        boolean keypad = isKeypadKeyCode(code);
        KeyEventState state = KeyEventState.of(pm.capsLock, pm.numLock, keypad);
        return new KeyEvent(
                code,
                mods,
                0,
                bindings,
                pm.kind,
                state,
                0,
                OptionalInt.empty(),
                OptionalInt.empty(),
                ""
        );
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
            case 'E':
                return KeyEvent.ofKey(KeyCode.KP_BEGIN, bindings);
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
        String[] parts = params.split(";", -1);
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

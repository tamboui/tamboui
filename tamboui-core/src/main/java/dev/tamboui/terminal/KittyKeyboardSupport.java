/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Constants, escape-sequence builders and optional terminal-side detection for the Kitty keyboard
 * protocol (progressive keyboard enhancements).
 * <p>
 * TamboUI pushes keyboard enhancement flags unconditionally on startup via
 * {@link Backend#enableKeyboardProtocol(KeyboardProtocol)} and pops them on shutdown via
 * {@link Backend#disableKeyboardProtocol()}. Terminals that do not implement the protocol ignore
 * both sequences; terminals that do implement it preserve any flags set by a parent process
 * (shell integration, wrapping TUI, tmux) via the protocol's wire-level stack.
 * <p>
 * Apps that need to detect whether the current terminal actually supports the protocol (for UI
 * adaptation, feature gating, or diagnostics) can call {@link #query(Backend, int)}. This is
 * opt-in and never invoked by the runner; the runner's default behaviour is push-unconditionally.
 *
 * <p><b>Escape sequences used:</b></p>
 * <ul>
 *   <li>Push flags: {@code CSI > <flags> u}</li>
 *   <li>Pop flags: {@code CSI < u}</li>
 *   <li>Query flags: {@code CSI ? u}</li>
 *   <li>Query response: {@code CSI ? <flags> u}</li>
 * </ul>
 *
 * @see <a href="https://sw.kovidgoyal.net/kitty/keyboard-protocol/">Kitty Keyboard Protocol</a>
 */
public final class KittyKeyboardSupport {

    /** Flag 1 (0b1): Disambiguate escape codes. */
    public static final int DISAMBIGUATE_ESCAPE_CODES = 1;

    /** Flag 2 (0b10): Report event types (press, repeat, release). */
    public static final int REPORT_EVENT_TYPES = 2;

    /** Flag 4 (0b100): Report alternate keys (shifted and base-layout keys). */
    public static final int REPORT_ALTERNATE_KEYS = 4;

    /** Flag 8 (0b1000): Report all keys as escape codes. */
    public static final int REPORT_ALL_KEYS_AS_ESCAPE_CODES = 8;

    /** Flag 16 (0b10000): Report associated text. */
    public static final int REPORT_ASSOCIATED_TEXT = 16;

    /**
     * Default flags for {@link KeyboardProtocol#AUTO} mode:
     * disambiguates escape codes and reports alternate key layouts.
     */
    public static final int AUTO_FLAGS = DISAMBIGUATE_ESCAPE_CODES | REPORT_ALTERNATE_KEYS;

    /**
     * Flags for {@link KeyboardProtocol#FULL} mode:
     * enables all progressive protocol enhancements.
     */
    public static final int FULL_FLAGS = DISAMBIGUATE_ESCAPE_CODES
            | REPORT_EVENT_TYPES
            | REPORT_ALTERNATE_KEYS
            | REPORT_ALL_KEYS_AS_ESCAPE_CODES
            | REPORT_ASSOCIATED_TEXT;

    private static final String CSI = "\033[";
    private static final String QUERY = CSI + "?u";
    private static final String POP_ONE = CSI + "<u";

    // Response parsing states for query()
    private static final int STATE_INITIAL = 0;
    private static final int STATE_ESC = 1;
    private static final int STATE_CSI = 2;
    private static final int STATE_QUESTION = 3;
    private static final int STATE_FLAGS = 4;

    private KittyKeyboardSupport() {
    }

    /**
     * Returns the push escape sequence for the given flags ({@code CSI > <flags> u}).
     *
     * @param flags the enhancement flags
     * @return the push escape sequence
     */
    public static String pushSequence(int flags) {
        return CSI + ">" + flags + "u";
    }

    /**
     * Returns the pop escape sequence ({@code CSI < u}).
     *
     * @return the pop escape sequence
     */
    public static String popSequence() {
        return POP_ONE;
    }

    /**
     * Returns the query escape sequence ({@code CSI ? u}).
     *
     * @return the query sequence
     */
    public static String querySequence() {
        return QUERY;
    }

    /**
     * Queries the terminal for Kitty keyboard protocol support.
     * <p>
     * Sends the {@code CSI ? u} query sequence and parses the {@code CSI ? <flags> u} response.
     * Returns the reported flags bitmask on success, {@code 0} if the terminal responded with no
     * flags enabled, or {@code -1} if the terminal did not respond within the timeout (treat as
     * "protocol not supported" for feature detection).
     * <p>
     * This method is opt-in: TamboUI's runner never calls it. Apps that want to detect support
     * for UI adaptation or diagnostics can call it directly, typically before invoking
     * {@link Backend#enableKeyboardProtocol(KeyboardProtocol)}.
     * <p>
     * <b>Warning:</b> query consumes bytes from the backend's input stream. Do not call it
     * concurrently with other code reading input (such as the TuiRunner's input reader).
     * Call it before starting the runner, or on a backend that is not yet attached to one.
     *
     * @param backend   the terminal backend
     * @param timeoutMs timeout in milliseconds to wait for response
     * @return the reported flags bitmask, or {@code -1} if unsupported or timed out
     * @throws IOException if an I/O error occurs
     */
    public static int query(Backend backend, int timeoutMs) throws IOException {
        backend.writeRaw(QUERY);
        backend.flush();
        return parseResponse(backend, timeoutMs);
    }

    private static int parseResponse(Backend backend, int timeoutMs) throws IOException {
        int state = STATE_INITIAL;
        int flags = 0;
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);

        while (System.nanoTime() < deadlineNanos) {
            int remainingTime = (int) TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
            if (remainingTime <= 0) {
                break;
            }

            int ch = backend.read(remainingTime);
            if (ch == -1 || ch == -2) {
                break;
            }

            switch (state) {
                case STATE_INITIAL:
                    if (ch == '\033') {
                        state = STATE_ESC;
                    }
                    break;

                case STATE_ESC:
                    if (ch == '[') {
                        state = STATE_CSI;
                    } else {
                        state = STATE_INITIAL;
                    }
                    break;

                case STATE_CSI:
                    if (ch == '?') {
                        state = STATE_QUESTION;
                        flags = 0;
                    } else {
                        state = STATE_INITIAL;
                    }
                    break;

                case STATE_QUESTION:
                    if (ch >= '0' && ch <= '9') {
                        flags = ch - '0';
                        state = STATE_FLAGS;
                    } else {
                        state = STATE_INITIAL;
                    }
                    break;

                case STATE_FLAGS:
                    if (ch >= '0' && ch <= '9') {
                        flags = flags * 10 + (ch - '0');
                    } else if (ch == 'u') {
                        return flags;
                    } else {
                        state = STATE_INITIAL;
                    }
                    break;

                default:
                    state = STATE_INITIAL;
                    break;
            }
        }

        return -1;
    }
}

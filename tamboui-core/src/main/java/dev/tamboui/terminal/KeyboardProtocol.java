/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

/**
 * Keyboard enhancement protocol mode for terminal input.
 * <p>
 * Controls which Kitty keyboard protocol flags (progressive keyboard enhancements) are pushed to
 * the terminal on startup. TamboUI does not negotiate with the terminal: it pushes the flags
 * unconditionally and pops them on shutdown. Terminals that do not implement the protocol ignore
 * both sequences; terminals that do implement it preserve any flags set by a parent process (shell
 * integration, wrapping TUI, tmux) via the protocol's wire-level stack.
 *
 * @see KittyKeyboardSupport
 * @see Backend#enableKeyboardProtocol(KeyboardProtocol)
 */
public enum KeyboardProtocol {

    /**
     * Never enable Kitty keyboard enhancements.
     * <p>
     * Conventional legacy terminal input encoding is used; nothing is pushed to the terminal.
     */
    LEGACY(0),

    /**
     * Default mode: enables the safe subset of enhancements that preserves ordinary text input.
     * <p>
     * Pushes {@link KittyKeyboardSupport#DISAMBIGUATE_ESCAPE_CODES} and
     * {@link KittyKeyboardSupport#REPORT_ALTERNATE_KEYS}. Disambiguates Escape, Tab vs Ctrl+I,
     * Enter vs Ctrl+M, and reports shifted and base-layout keys without altering ordinary
     * character delivery.
     */
    AUTO(KittyKeyboardSupport.AUTO_FLAGS),

    /**
     * Full protocol mode.
     * <p>
     * Pushes every flag the protocol defines: disambiguation, event types (press, repeat,
     * release), alternate keys, all keys reported as escape codes, and associated text. Enables
     * the richest input stream the terminal can provide; apps opting in should be prepared for
     * release events on every key.
     */
    FULL(KittyKeyboardSupport.FULL_FLAGS);

    private final int flags;

    KeyboardProtocol(int flags) {
        this.flags = flags;
    }

    /**
     * Returns the Kitty keyboard protocol flags bitmask associated with this protocol mode.
     *
     * @return the flags bitmask, or 0 for {@link #LEGACY}
     */
    public int flags() {
        return flags;
    }
}

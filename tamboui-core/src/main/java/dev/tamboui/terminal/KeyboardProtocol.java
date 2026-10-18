/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

/**
 * Keyboard enhancement protocol mode for terminal input.
 * <p>
 * Controls whether and how the Kitty keyboard protocol (progressive keyboard enhancements)
 * is negotiated with the terminal.
 *
 * @see KittyKeyboardSupport
 */
public enum KeyboardProtocol {

    /**
     * Never enable Kitty keyboard enhancements.
     * <p>
     * Conventional legacy terminal input encoding is used.
     */
    LEGACY(0),

    /**
     * Default mode. Enables safe enhancements supported by the terminal.
     * <p>
     * Enables disambiguation of escape codes and reporting of alternate key layouts
     * without altering ordinary text input delivery.
     */
    AUTO(KittyKeyboardSupport.AUTO_FLAGS),

    /**
     * Full protocol mode.
     * <p>
     * Requests all enhancements: event types (press, repeat, release),
     * all keys reported as escape codes, and associated text.
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

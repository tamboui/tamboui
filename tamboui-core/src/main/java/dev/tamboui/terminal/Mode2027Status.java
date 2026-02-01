/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

/**
 * Represents the status of Mode 2027 (grapheme cluster mode) support in a terminal.
 * <p>
 * Mode 2027 is a terminal feature that coordinates grapheme cluster handling between
 * applications and terminals. When enabled, the terminal signals that it can handle
 * complex grapheme clusters (ZWJ emoji sequences, regional indicators) as single
 * display units.
 */
public enum Mode2027Status {

    /**
     * Terminal doesn't recognize Mode 2027.
     * This typically means the terminal is older or doesn't support
     * the DECRQM query for mode 2027.
     */
    NOT_SUPPORTED,

    /**
     * Terminal recognizes Mode 2027 but has it disabled (Ps=2 or Ps=4 in DECRPM response).
     */
    SUPPORTED_DISABLED,

    /**
     * Mode 2027 is currently enabled (Ps=1 or Ps=3 in DECRPM response).
     */
    ENABLED;

    /**
     * Returns whether the terminal supports Mode 2027.
     *
     * @return true if the terminal recognizes Mode 2027 (either enabled or disabled),
     *         false if the terminal doesn't support it at all
     */
    public boolean isSupported() {
        return this != NOT_SUPPORTED;
    }
}

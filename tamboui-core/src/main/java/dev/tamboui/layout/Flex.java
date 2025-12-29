/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * Flex mode for distributing remaining space in layouts.
 */
public enum Flex {
    /**
     * Original tui-rs behavior for backwards compatibility.
     */
    LEGACY,

    /**
     * Pack elements to the start.
     */
    START,

    /**
     * Center elements.
     */
    CENTER,

    /**
     * Pack elements to the end.
     */
    END,

    /**
     * Distribute space between elements.
     */
    SPACE_BETWEEN,

    /**
     * Distribute space around elements.
     */
    SPACE_AROUND,

    /**
     * Distribute space evenly (uniform gaps including edges).
     */
    SPACE_EVENLY
}

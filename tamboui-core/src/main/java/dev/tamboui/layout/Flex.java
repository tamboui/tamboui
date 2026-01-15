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
     * Distribute space evenly between and around elements.
     * Unlike SPACE_AROUND which gives half-size gaps at edges,
     * SPACE_EVENLY gives equal gaps everywhere.
     */
    SPACE_EVENLY
}

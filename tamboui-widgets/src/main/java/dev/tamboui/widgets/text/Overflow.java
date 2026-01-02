/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.text;

/**
 * Text overflow handling mode for paragraphs.
 * Determines what happens when text doesn't fit in the available space.
 */
public enum Overflow {
    /**
     * Silent truncation - text is clipped at the boundary with no indicator.
     */
    CLIP,

    /**
     * Wrap at character boundaries.
     */
    WRAP_CHARACTER,

    /**
     * Wrap at word boundaries.
     */
    WRAP_WORD,

    /**
     * Truncate with ellipsis at the end: "Long text..."
     */
    ELLIPSIS,

    /**
     * Truncate with ellipsis at the start: "...ong text"
     */
    ELLIPSIS_START,

    /**
     * Truncate with ellipsis in the middle: "Long...text"
     */
    ELLIPSIS_MIDDLE
}

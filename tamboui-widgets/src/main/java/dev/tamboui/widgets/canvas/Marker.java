/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas;

/**
 * Marker types for rendering points on a {@link Canvas}.
 * <p>
 * Different markers provide different resolutions and visual styles.
 */
public enum Marker {
    /**
     * One point per cell in shape of a dot (•).
     */
    DOT,

    /**
     * One point per cell in shape of a block (█).
     */
    BLOCK,

    /**
     * One point per cell in shape of a bar (▄).
     */
    BAR,

    /**
     * Uses Unicode Braille Patterns for high-resolution rendering.
     * <p>
     * Each cell represents a 2x4 grid of dots that can be individually toggled on
     * or off, providing 8 points per character cell.
     * <p>
     * Note: Requires terminal and font support for Braille patterns.
     */
    BRAILLE,

    /**
     * Uses Unicode block and half-block characters (█, ▄, ▀).
     * <p>
     * Provides double the vertical resolution by using half-block characters, with
     * 2 points per character cell.
     */
    HALF_BLOCK
}

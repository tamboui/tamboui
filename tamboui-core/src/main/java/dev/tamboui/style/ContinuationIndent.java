/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

/**
 * Strategy for continuing wrapped lines of text.
 * <p>
 * Determines where continuation rows begin when a logical line wraps across multiple rows.
 * Sits alongside {@link Overflow} in text-layout options as two facets of the same mechanism.
 */
public enum ContinuationIndent {
    /**
     * Continuation rows align under the first non-blank character of the wrapped line.
     * <p>
     * An indented line (such as a list item or a key-value pair) keeps its indent when it wraps.
     * If the indent leaves no room for text on continuation rows, wrapping falls back to
     * restarting at the left edge.
     */
    HANGING,

    /**
     * Continuation rows restart at the left edge (column 0).
     * <p>
     * Used for ASCII art, layouts drawn with leading spaces, or anywhere leading indentation
     * should not be preserved across wrapped rows.
     */
    FLUSH_LEFT
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.symbols.merge;

/**
 * A visual style defining the appearance of a single line making up a block border.
 * <p>
 * This is an internal type used to represent the different styles of lines that can be used in
 * border symbols.
 */
enum LineStyle {
    /** Represents the absence of a line. */
    NOTHING,

    /** A single line (e.g. `─`, `│`). */
    PLAIN,

    /** A rounded line style, only applicable in corner symbols (e.g. `╭`, `╯`). */
    ROUNDED,

    /** A double line (e.g. `═`, `║`). */
    DOUBLE,

    /** A thickened line (e.g. `━`, `┃`). */
    THICK,

    /** A dashed line with a double dash pattern (e.g. `╌`, `╎`). */
    DOUBLE_DASH,

    /** A thicker variant of the double dash (e.g. `╍`, `╏`). */
    DOUBLE_DASH_THICK,

    /** A dashed line with a triple dash pattern (e.g. `┄`, `┆`). */
    TRIPLE_DASH,

    /** A thicker variant of the triple dash (e.g. `┅`, `┇`). */
    TRIPLE_DASH_THICK,

    /** A dashed line with four dashes (e.g. `┈`, `┊`). */
    QUADRUPLE_DASH,

    /** A thicker variant of the quadruple dash (e.g. `┉`, `┋`). */
    QUADRUPLE_DASH_THICK;

    /**
     * Merges line styles.
     * <p>
     * If the other style is NOTHING, returns this style. Otherwise, returns the other style.
     */
    LineStyle merge(LineStyle other) {
        return other == NOTHING ? this : other;
    }
}

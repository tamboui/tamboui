/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import java.util.EnumSet;

/**
 * Which borders to draw on a block.
 */
public enum Borders {
    /** Top border. */
    TOP,
    /** Right border. */
    RIGHT,
    /** Bottom border. */
    BOTTOM,
    /** Left border. */
    LEFT;

    /** No borders. */
    public static final EnumSet<Borders> NONE = EnumSet.noneOf(Borders.class);
    /** All borders. */
    public static final EnumSet<Borders> ALL = EnumSet.allOf(Borders.class);
    /** Top border only. */
    public static final EnumSet<Borders> TOP_ONLY = EnumSet.of(TOP);
    /** Right border only. */
    public static final EnumSet<Borders> RIGHT_ONLY = EnumSet.of(RIGHT);
    /** Bottom border only. */
    public static final EnumSet<Borders> BOTTOM_ONLY = EnumSet.of(BOTTOM);
    /** Left border only. */
    public static final EnumSet<Borders> LEFT_ONLY = EnumSet.of(LEFT);
}

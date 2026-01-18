/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import java.util.EnumSet;

/// Which borders to draw on a block.
public enum Borders {
    TOP,
    RIGHT,
    BOTTOM,
    LEFT;

    public static final EnumSet<Borders> NONE = EnumSet.noneOf(Borders.class);
    public static final EnumSet<Borders> ALL = EnumSet.allOf(Borders.class);
    public static final EnumSet<Borders> TOP_ONLY = EnumSet.of(TOP);
    public static final EnumSet<Borders> RIGHT_ONLY = EnumSet.of(RIGHT);
    public static final EnumSet<Borders> BOTTOM_ONLY = EnumSet.of(BOTTOM);
    public static final EnumSet<Borders> LEFT_ONLY = EnumSet.of(LEFT);
}


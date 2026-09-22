/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.util.EnumSet;

import dev.tamboui.widgets.block.Borders;

/** Border rendering mode for toast panels. */
public enum BorderMode {
    /** Left and right border rails only. */
    SIDE_RAILS(EnumSet.of(Borders.LEFT, Borders.RIGHT)),
    /** Full border on all sides. */
    FULL(Borders.ALL);

    private final EnumSet<Borders> borders;

    BorderMode(EnumSet<Borders> borders) {
        this.borders = borders;
    }

    /**
     * Returns the {@link Borders} set for this mode.
     *
     * @return a copy of the borders to render
     */
    public EnumSet<Borders> borders() {
        return EnumSet.copyOf(borders);
    }
}

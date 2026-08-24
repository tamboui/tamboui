/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import dev.tamboui.style.Color;

/** Semantic toast category with a default accent color. */
public enum ToastType {
    /** Informational toast with blue accent. */
    INFO(Color.BLUE),
    /** Success toast with green accent. */
    SUCCESS(Color.GREEN),
    /** Warning toast with yellow accent. */
    WARNING(Color.YELLOW),
    /** Error toast with red accent. */
    ERROR(Color.RED);

    private final Color accentColor;

    ToastType(Color accentColor) {
        this.accentColor = accentColor;
    }

    /**
     * Returns the default accent color for this toast type.
     *
     * @return the accent color
     */
    public Color accentColor() {
        return accentColor;
    }
}

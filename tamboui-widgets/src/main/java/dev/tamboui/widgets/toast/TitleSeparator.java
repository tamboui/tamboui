/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

/** Separator rendered between toast title segments. */
public enum TitleSeparator {
    /** Dot separator between title segments. */
    DOT(" · "),
    /** Line separator between title segments. */
    LINE(" ─ "),
    /** No separator between title segments. */
    NONE("");

    private final String separatorText;

    TitleSeparator(String separatorText) {
        this.separatorText = separatorText;
    }

    String separatorText() {
        return separatorText;
    }
}

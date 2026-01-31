/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.text;

import dev.tamboui.export.ExportOptions;
import dev.tamboui.export.Formats;

/**
 * Options for text export (plain or ANSI-styled). All values have reasonable defaults.
 * Used by the fluent export API; see {@link Formats#TEXT}.
 */
public final class TextOptions implements ExportOptions {

    boolean styles = false;

    /** Creates default text export options (plain text, no ANSI). */
    public TextOptions() {
    }

    /**
     * Sets whether to include ANSI escape codes (true) or plain text only (false).
     *
     * @param styles true for ANSI-styled output, false for plain text
     * @return this options instance
     */
    public TextOptions styles(boolean styles) {
        this.styles = styles;
        return this;
    }
}

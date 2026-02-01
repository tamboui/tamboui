/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.html;

import dev.tamboui.export.ExportOptions;
import dev.tamboui.export.Formats;
import dev.tamboui.export.ThemeColors;

/**
 * Options for HTML export. All values have reasonable defaults.
 * Used by the fluent export API; see {@link Formats#HTML}.
 */
public final class HtmlOptions implements ExportOptions {

    ThemeColors theme = ThemeColors.defaultTheme();
    boolean inlineStyles = false;

    /** Creates default HTML export options. */
    public HtmlOptions() {
    }

    /**
     * Sets the color theme.
     *
     * @param theme the HTML color theme
     * @return this options instance
     */
    public HtmlOptions theme(ThemeColors theme) {
        this.theme = theme;
        return this;
    }

    /**
     * Sets whether styles are inlined in spans (true) or in a stylesheet (false).
     *
     * @param inlineStyles true for inline styles
     * @return this options instance
     */
    public HtmlOptions inlineStyles(boolean inlineStyles) {
        this.inlineStyles = inlineStyles;
        return this;
    }
}

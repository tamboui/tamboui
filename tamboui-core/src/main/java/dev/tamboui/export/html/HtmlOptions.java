/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.html;

import dev.tamboui.export.ExportOptions;
import dev.tamboui.export.Formats;
import dev.tamboui.style.StylePropertyResolver;

/**
 * Options for HTML export. All values have reasonable defaults.
 * Used by the fluent export API; see {@link Formats#HTML}.
 */
public final class HtmlOptions implements ExportOptions {

    StylePropertyResolver styles;
    boolean inlineStyles = false;

    /** Creates default HTML export options. */
    public HtmlOptions() {
    }

    /**
     * Sets the style property resolver for default export foreground/background.
     * When {@code null} (default), export uses {@link dev.tamboui.export.ExportProperties}
     * defaults (dark-theme). When set (e.g. from the toolkit style engine), the resolver
     * supplies values for {@code export-foreground} and {@code export-background}.
     *
     * @param styles the style resolver, or {@code null} to use property defaults
     * @return this options instance
     */
    public HtmlOptions styles(StylePropertyResolver styles) {
        this.styles = styles;
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

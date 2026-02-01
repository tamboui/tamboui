/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.svg;

import dev.tamboui.export.ExportOptions;
import dev.tamboui.export.Formats;
import dev.tamboui.export.ThemeColors;

/**
 * Options for SVG export. All values have reasonable defaults.
 * Used by the fluent export API; see {@link Formats#SVG}.
 */
public final class SvgOptions implements ExportOptions {

    String title = "TamboUI";
    boolean chrome = true;
    ThemeColors theme = ThemeColors.defaultTheme();
    double fontAspectRatio = 0.61;
    String uniqueId;

    /** Creates default SVG export options. */
    public SvgOptions() {
    }

    /**
     * Sets whether to include window chrome (frame, title, traffic-light buttons).
     * When {@code false}, the SVG contains only the terminal content with minimal padding.
     *
     * @param chrome {@code true} to include chrome (default), {@code false} for content only
     * @return this options instance
     */
    public SvgOptions chrome(boolean chrome) {
        this.chrome = chrome;
        return this;
    }

    /**
     * Sets the window title.
     *
     * @param title the SVG window title
     * @return this options instance
     */
    public SvgOptions title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the color theme.
     *
     * @param theme the SVG color theme
     * @return this options instance
     */
    public SvgOptions theme(ThemeColors theme) {
        this.theme = theme;
        return this;
    }

    /**
     * Sets the font aspect ratio used for width calculations.
     *
     * @param fontAspectRatio the character width-to-height ratio
     * @return this options instance
     */
    public SvgOptions fontAspectRatio(double fontAspectRatio) {
        this.fontAspectRatio = fontAspectRatio;
        return this;
    }

    /**
     * Sets a custom unique ID prefix for CSS classes and clip paths.
     *
     * @param uniqueId the unique ID prefix, or {@code null} for auto-generated
     * @return this options instance
     */
    public SvgOptions uniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
        return this;
    }
}

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
    ThemeColors theme = ThemeColors.defaultTheme();
    String codeFormat = SvgExporter.DEFAULT_SVG_FORMAT;
    double fontAspectRatio = 0.61;
    String uniqueId;

    /** Creates default SVG export options. */
    public SvgOptions() {
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
     * Sets the SVG template format string.
     *
     * @param codeFormat the SVG format template with placeholders
     * @return this options instance
     */
    public SvgOptions codeFormat(String codeFormat) {
        this.codeFormat = codeFormat;
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

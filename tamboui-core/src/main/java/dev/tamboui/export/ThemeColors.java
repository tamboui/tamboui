/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.style.Color;

import java.util.Objects;

/**
 * Immutable default foreground and background colors for export (e.g. when a style has RESET).
 * Use {@link #defaultTheme()} for the built-in dark theme, or construct with custom RGB values.
 */
public final class ThemeColors {

    private final Color.Rgb foreground;
    private final Color.Rgb background;

    /**
     * Built-in dark theme aligned with Rich's SVG export theme.
     *
     * @return theme colors with dark background and light foreground
     */
    public static ThemeColors defaultTheme() {
        return new ThemeColors(new Color.Rgb(197, 200, 198), new Color.Rgb(41, 41, 41));
    }

    /**
     * Creates theme colors.
     *
     * @param foreground the default foreground (text) color
     * @param background the default background color
     */
    public ThemeColors(Color.Rgb foreground, Color.Rgb background) {
        this.foreground = Objects.requireNonNull(foreground, "foreground");
        this.background = Objects.requireNonNull(background, "background");
    }

    /**
     * Returns the default foreground color.
     *
     * @return the foreground color
     */
    public Color.Rgb foreground() {
        return foreground;
    }

    /**
     * Returns the default background color.
     *
     * @return the background color
     */
    public Color.Rgb background() {
        return background;
    }
}

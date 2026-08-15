/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.theme;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dev.tamboui.style.Color;
import dev.tamboui.style.StylePropertyResolver;

/**
 * Defines a theme with semantic colors and generation parameters.
 * <p>
 * A theme consists of:
 * <ul>
 *   <li>Semantic colors (primary, secondary, error, success, warning, etc.)</li>
 *   <li>Generation parameters (luminositySpread, textAlpha)</li>
 *   <li>Optional override variables for specific colors/properties</li>
 * </ul>
 * <p>
 * Themes are immutable. Use {@link Builder} to construct instances.
 *
 * @see ColorSystem
 * @see ThemeRegistry
 */
public final class Theme {

    private final String name;
    private final boolean dark;

    // Semantic colors
    private final Color primary;
    private final Color secondary;
    private final Color error;
    private final Color success;
    private final Color warning;
    private final Color accent;
    private final Color info;

    // Surface colors
    private final Color background;
    private final Color surface;
    private final Color panel;
    private final Color foreground;

    // Generation parameters
    private final float luminositySpread;
    private final float textAlpha;

    // Override variables
    private final Map<String, String> variables;

    private Theme(Builder builder) {
        this.name = builder.name;
        this.dark = builder.dark;
        this.primary = builder.primary;
        this.secondary = builder.secondary;
        this.error = builder.error;
        this.success = builder.success;
        this.warning = builder.warning;
        this.accent = builder.accent;
        this.info = builder.info;
        this.background = builder.background;
        this.surface = builder.surface;
        this.panel = builder.panel;
        this.foreground = builder.foreground;
        this.luminositySpread = builder.luminositySpread;
        this.textAlpha = builder.textAlpha;
        this.variables = Collections.unmodifiableMap(new HashMap<>(builder.variables));
    }

    /**
     * Gets the theme name.
     * @return the theme name
     */
    public String name() { return name; }

    /**
     * Checks whether this is a dark theme.
     * @return whether this is a dark theme
     */
    public boolean dark() { return dark; }

    /**
     * Gets the primary semantic color.
     * @return primary semantic color
     */
    public Color primary() { return primary; }

    /**
     * Gets the secondary semantic color.
     * @return secondary semantic color
     */
    public Color secondary() { return secondary; }

    /**
     * Gets the error semantic color.
     * @return error semantic color
     */
    public Color error() { return error; }

    /**
     * Gets the success semantic color.
     * @return success semantic color
     */
    public Color success() { return success; }

    /**
     * Gets the warning semantic color.
     * @return warning semantic color
     */
    public Color warning() { return warning; }

    /**
     * Gets the accent semantic color.
     * @return accent semantic color
     */
    public Color accent() { return accent; }

    /**
     * Gets the info semantic color.
     * @return info semantic color
     */
    public Color info() { return info; }

    /**
     * Gets the background surface color.
     * @return background surface color
     */
    public Color background() { return background; }

    /**
     * Gets the surface color.
     * @return surface color
     */
    public Color surface() { return surface; }

    /**
     * Gets the panel color.
     * @return panel color
     */
    public Color panel() { return panel; }

    /**
     * Gets the foreground color.
     * @return foreground color
     */
    public Color foreground() { return foreground; }

    /**
     * Gets the luminosity spread for color derivation.
     * @return luminosity spread for color derivation (0.0-1.0)
     */
    public float luminositySpread() { return luminositySpread; }

    /**
     * Gets the text alpha for muted text.
     * @return text alpha for muted text (0.0-1.0)
     */
    public float textAlpha() { return textAlpha; }

    /**
     * Gets the custom CSS variable overrides.
     * @return custom CSS variable overrides
     */
    public Map<String, String> variables() { return variables; }

    /**
     * Generates a ColorSystem from this theme.
     *
     * @return generated color system with ~20-30 derived colors
     */
    public ColorSystem generate() {
        Map<String, Color> colors = new HashMap<>();

        // 1. Apply defaults for nullable colors
        Color effectiveBg = background != null ? background :
            (dark ? Color.hex("#121212") : Color.hex("#efefef"));
        Color effectiveSurface = surface != null ? surface :
            (dark ? Color.hex("#1e1e1e") : Color.hex("#f5f5f5"));
        Color effectiveFg = foreground != null ? foreground :
            effectiveBg.inverse();
        Color effectivePanel = panel != null ? panel :
            effectiveSurface.blend(primary, 0.1f);

        // 2. Store base semantic colors
        colors.put("primary", primary);
        colors.put("secondary", secondary);
        colors.put("error", error);
        colors.put("success", success);
        colors.put("warning", warning);
        if (accent != null) colors.put("accent", accent);
        if (info != null) colors.put("info", info);

        // 3. Store surface hierarchy
        colors.put("background", effectiveBg);
        colors.put("surface", effectiveSurface);
        colors.put("panel", effectivePanel);
        colors.put("foreground", effectiveFg);

        // 4. Generate shades (light/dark/muted variants)
        colors.put("primary-light", getOrVariable("primary-light",
            primary.lighten(luminositySpread)));
        colors.put("primary-dark", getOrVariable("primary-dark",
            primary.darken(luminositySpread)));
        colors.put("primary-muted", getOrVariable("primary-muted",
            primary.blend(effectiveBg, 0.7f)));

        colors.put("secondary-light", getOrVariable("secondary-light",
            secondary.lighten(luminositySpread)));
        colors.put("secondary-dark", getOrVariable("secondary-dark",
            secondary.darken(luminositySpread)));
        colors.put("secondary-muted", getOrVariable("secondary-muted",
            secondary.blend(effectiveBg, 0.7f)));

        colors.put("error-light", getOrVariable("error-light",
            error.lighten(luminositySpread)));
        colors.put("error-dark", getOrVariable("error-dark",
            error.darken(luminositySpread)));

        // TODO: Add success/warning shades when widgets need them

        // 5. Generate text colors
        // Use the theme's foreground color as the base text color rather than
        // a pure black/white contrast.  Themes like Nord (#eceff4) and
        // Catppuccin (#cdd6f4) intentionally use softer foreground tones.
        colors.put("text", getOrVariable("text", effectiveFg));
        colors.put("text-muted", getOrVariable("text-muted",
            effectiveFg.blend(effectiveBg, 0.4f)));
        colors.put("text-disabled", getOrVariable("text-disabled",
            effectiveFg.blend(effectiveBg, 0.62f)));
        colors.put("text-on-primary", getOrVariable("text-on-primary",
            primary.getContrastText()));

        // 6. Generate UI colors
        colors.put("border", getOrVariable("border",
            effectiveSurface.darken(0.025f)));
        colors.put("border-focus", getOrVariable("border-focus", primary));
        colors.put("border-muted", getOrVariable("border-muted",
            effectiveSurface.darken(0.01f)));

        colors.put("selection-bg", getOrVariable("selection-bg",
            primary.blend(effectiveBg, 0.7f)));
        colors.put("hover-bg", getOrVariable("hover-bg",
            effectiveFg.blend(effectiveBg, 0.96f)));

        // TODO: scrollbar colors when ScrollBar widget implemented
        // TODO: link colors when Link widget implemented

        return new ColorSystem(colors);
    }

    private Color getOrVariable(String name, Color generated) {
        if (variables.containsKey(name)) {
            return Color.hex(variables.get(name));
        }
        return generated;
    }

    /**
     * Creates a StylePropertyResolver from this theme.
     *
     * @return property resolver for querying theme colors
     */
    public StylePropertyResolver toResolver() {
        ColorSystem system = generate();
        return new ThemePropertyResolver(system, this.variables);
    }

    /**
     * Builder for constructing Theme instances.
     */
    public static final class Builder {
        private String name;
        private boolean dark = true;
        private Color primary;
        private Color secondary;
        private Color error;
        private Color success;
        private Color warning;
        private Color accent;
        private Color info;
        private Color background;
        private Color surface;
        private Color panel;
        private Color foreground;
        private float luminositySpread = 0.15f;
        private float textAlpha = 0.95f;
        private Map<String, String> variables = new HashMap<>();

        /**
         * Creates a new builder.
         */
        public Builder() {
        }

        /**
         * Sets the theme name.
         * @param name theme name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets whether this is a dark theme.
         * @param dark whether this is a dark theme
         * @return this builder
         */
        public Builder dark(boolean dark) {
            this.dark = dark;
            return this;
        }

        /**
         * Sets the primary color from hex string.
         * @param hex primary color as hex string
         * @return this builder
         */
        public Builder primary(String hex) {
            this.primary = Color.hex(hex);
            return this;
        }

        /**
         * Sets the primary color.
         * @param color primary color
         * @return this builder
         */
        public Builder primary(Color color) {
            this.primary = color;
            return this;
        }

        /**
         * Sets the secondary color from hex string.
         * @param hex secondary color as hex string
         * @return this builder
         */
        public Builder secondary(String hex) {
            this.secondary = Color.hex(hex);
            return this;
        }

        /**
         * Sets the secondary color.
         * @param color secondary color
         * @return this builder
         */
        public Builder secondary(Color color) {
            this.secondary = color;
            return this;
        }

        /**
         * Sets the error color from hex string.
         * @param hex error color as hex string
         * @return this builder
         */
        public Builder error(String hex) {
            this.error = Color.hex(hex);
            return this;
        }

        /**
         * Sets the error color.
         * @param color error color
         * @return this builder
         */
        public Builder error(Color color) {
            this.error = color;
            return this;
        }

        /**
         * Sets the success color from hex string.
         * @param hex success color as hex string
         * @return this builder
         */
        public Builder success(String hex) {
            this.success = Color.hex(hex);
            return this;
        }

        /**
         * Sets the success color.
         * @param color success color
         * @return this builder
         */
        public Builder success(Color color) {
            this.success = color;
            return this;
        }

        /**
         * Sets the warning color from hex string.
         * @param hex warning color as hex string
         * @return this builder
         */
        public Builder warning(String hex) {
            this.warning = Color.hex(hex);
            return this;
        }

        /**
         * Sets the warning color.
         * @param color warning color
         * @return this builder
         */
        public Builder warning(Color color) {
            this.warning = color;
            return this;
        }

        /**
         * Sets the accent color from hex string.
         * @param hex accent color as hex string
         * @return this builder
         */
        public Builder accent(String hex) {
            this.accent = Color.hex(hex);
            return this;
        }

        /**
         * Sets the accent color.
         * @param color accent color
         * @return this builder
         */
        public Builder accent(Color color) {
            this.accent = color;
            return this;
        }

        /**
         * Sets the info color from hex string.
         * @param hex info color as hex string
         * @return this builder
         */
        public Builder info(String hex) {
            this.info = Color.hex(hex);
            return this;
        }

        /**
         * Sets the info color.
         * @param color info color
         * @return this builder
         */
        public Builder info(Color color) {
            this.info = color;
            return this;
        }

        /**
         * Sets the background color from hex string.
         * @param hex background color as hex string
         * @return this builder
         */
        public Builder background(String hex) {
            this.background = Color.hex(hex);
            return this;
        }

        /**
         * Sets the background color.
         * @param color background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.background = color;
            return this;
        }

        /**
         * Sets the surface color from hex string.
         * @param hex surface color as hex string
         * @return this builder
         */
        public Builder surface(String hex) {
            this.surface = Color.hex(hex);
            return this;
        }

        /**
         * Sets the surface color.
         * @param color surface color
         * @return this builder
         */
        public Builder surface(Color color) {
            this.surface = color;
            return this;
        }

        /**
         * Sets the panel color from hex string.
         * @param hex panel color as hex string
         * @return this builder
         */
        public Builder panel(String hex) {
            this.panel = Color.hex(hex);
            return this;
        }

        /**
         * Sets the panel color.
         * @param color panel color
         * @return this builder
         */
        public Builder panel(Color color) {
            this.panel = color;
            return this;
        }

        /**
         * Sets the foreground color from hex string.
         * @param hex foreground color as hex string
         * @return this builder
         */
        public Builder foreground(String hex) {
            this.foreground = Color.hex(hex);
            return this;
        }

        /**
         * Sets the foreground color.
         * @param color foreground color
         * @return this builder
         */
        public Builder foreground(Color color) {
            this.foreground = color;
            return this;
        }

        /**
         * Sets the luminosity spread for color derivation.
         * @param spread luminosity spread for color derivation (0.0-1.0)
         * @return this builder
         */
        public Builder luminositySpread(float spread) {
            this.luminositySpread = spread;
            return this;
        }

        /**
         * Sets the text alpha for muted text.
         * @param alpha text alpha for muted text (0.0-1.0)
         * @return this builder
         */
        public Builder textAlpha(float alpha) {
            this.textAlpha = alpha;
            return this;
        }

        /**
         * Adds a custom CSS variable override.
         * @param key variable name
         * @param value variable value
         * @return this builder
         */
        public Builder variable(String key, String value) {
            this.variables.put(key, value);
            return this;
        }

        /**
         * Builds the theme.
         * @return the constructed theme
         * @throws IllegalStateException if required fields are missing
         */
        public Theme build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalStateException("Theme name is required");
            }
            if (primary == null) {
                throw new IllegalStateException("Primary color is required");
            }

            // Set defaults for required semantic colors if not provided
            if (secondary == null) secondary = primary;
            if (error == null) error = Color.RED;
            if (success == null) success = Color.GREEN;
            if (warning == null) warning = Color.YELLOW;

            return new Theme(this);
        }
    }
}

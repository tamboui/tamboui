/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.theme;

import java.util.HashMap;
import java.util.Map;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.style.Color;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.theme.ColorSystem;
import dev.tamboui.theme.Theme;
import dev.tamboui.theme.ThemeRegistry;

/**
 * Integrates themes with the CSS engine.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Generate ColorSystem from Theme</li>
 *   <li>Inject theme colors as CSS variables ($primary, $error, etc.)</li>
 *   <li>Provide fallback resolver for non-CSS contexts</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>{@code
 * ThemeEngine themeEngine = new ThemeEngine();
 * themeEngine.setTheme("nord");
 * themeEngine.injectVariables(styleEngine);  // One-time setup
 *
 * // Per-element during rendering:
 * CssStyleResolver css = styleEngine.resolve(element);
 * StylePropertyResolver composed = themeEngine.composeWithCss(css);
 * }</pre>
 */
public final class ThemeEngine {

    private Theme currentTheme;
    private ColorSystem colorSystem;
    private StylePropertyResolver themeResolver;

    /**
     * Creates a ThemeEngine with the auto-terminal theme.
     */
    public ThemeEngine() {
        this(ThemeRegistry.get("auto-terminal")
            .orElseThrow(() -> new IllegalStateException("auto-terminal theme not found")));
    }

    /**
     * Creates a ThemeEngine with the specified theme.
     */
    public ThemeEngine(Theme theme) {
        setTheme(theme);
    }

    /**
     * Sets the active theme and regenerates colors.
     */
    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        this.colorSystem = theme.generate();
        this.themeResolver = theme.toResolver();
    }

    /**
     * Sets the active theme by name.
     */
    public void setTheme(String themeName) {
        Theme theme = ThemeRegistry.get(themeName)
            .orElseThrow(() -> new IllegalArgumentException("Unknown theme: " + themeName));
        setTheme(theme);
    }

    /**
     * Gets the current theme.
     */
    public Theme getTheme() {
        return currentTheme;
    }

    /**
     * Gets the generated color system.
     */
    public ColorSystem getColorSystem() {
        return colorSystem;
    }

    /**
     * Gets the theme property resolver.
     * Use this as fallback when no CSS is available.
     */
    public StylePropertyResolver getResolver() {
        return themeResolver;
    }

    /**
     * Injects theme colors as TCSS variables into the given style engine.
     * <p>
     * Generated variables (automatically available in all TCSS files):
     * <ul>
     *   <li>$primary, $secondary, $error, $success, $warning, $accent, $info</li>
     *   <li>$primary-light, $primary-dark, $primary-muted (similar for secondary, error)</li>
     *   <li>$background, $surface, $panel, $foreground</li>
     *   <li>$text, $text-muted, $text-disabled, $text-on-primary</li>
     *   <li>$border, $border-focus, $border-muted</li>
     *   <li>$selection-bg, $hover-bg</li>
     * </ul>
     * <p>
     * Call this once at application startup.
     */
    public void injectVariables(StyleEngine styleEngine) {
        Map<String, String> cssVariables = new HashMap<>();

        // Convert all ColorSystem colors to TCSS variables
        for (String colorName : colorSystem.colorNames()) {
            Color color = colorSystem.get(colorName);
            String cssValue = toCssValue(color);
            cssVariables.put(colorName, cssValue);
        }

        // Inject into style engine's global variable context
        styleEngine.setThemeVariables(cssVariables);
    }

    /**
     * Creates a composed resolver: CSS with theme fallback.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>CSS rules (highest specificity)</li>
     *   <li>Theme-generated colors</li>
     *   <li>Property defaults</li>
     * </ol>
     * <p>
     * Call this per-element during rendering.
     */
    public StylePropertyResolver composeWithCss(CssStyleResolver cssResolver) {
        // TODO: Implement proper composition in Task 11
        // For now, just return the theme resolver
        return themeResolver;
    }

    private String toCssValue(Color color) {
        // Convert Color to CSS-compatible string
        if (color instanceof Color.Rgb) {
            Color.Rgb rgb = (Color.Rgb) color;
            return String.format("#%02x%02x%02x", rgb.r(), rgb.g(), rgb.b());
        } else if (color instanceof Color.Named) {
            Color.Named named = (Color.Named) color;
            // For ANSI colors, use the name (e.g., "blue" for terminal adaptation)
            return named.name();
        } else if (color instanceof Color.Ansi) {
            Color.Ansi ansi = (Color.Ansi) color;
            return ansi.color().name().toLowerCase().replace('_', '-');
        }
        // Fallback: convert to RGB
        Color.Rgb rgb = color.toRgb();
        return String.format("#%02x%02x%02x", rgb.r(), rgb.g(), rgb.b());
    }
}

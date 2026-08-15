/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.theme;

import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;

/**
 * Standard property definitions for theme colors.
 * <p>
 * These properties can be queried from StylePropertyResolver to get
 * theme-generated colors in widgets that don't use CSS.
 * <p>
 * Example usage:
 * <pre>{@code
 * StylePropertyResolver resolver = theme.toResolver();
 * Color primary = resolver.get(ThemeProperties.PRIMARY).orElse(Color.BLUE);
 * }</pre>
 */
public final class ThemeProperties {

    private ThemeProperties() {
        // Utility class
    }

    // ===== Base Semantic Colors =====

    /** Primary semantic color property. */
    public static final PropertyDefinition<Color> PRIMARY =
        PropertyDefinition.of("primary", ColorConverter.INSTANCE);

    /** Secondary semantic color property. */
    public static final PropertyDefinition<Color> SECONDARY =
        PropertyDefinition.of("secondary", ColorConverter.INSTANCE);

    /** Error semantic color property. */
    public static final PropertyDefinition<Color> ERROR =
        PropertyDefinition.of("error", ColorConverter.INSTANCE);

    /** Success semantic color property. */
    public static final PropertyDefinition<Color> SUCCESS =
        PropertyDefinition.of("success", ColorConverter.INSTANCE);

    /** Warning semantic color property. */
    public static final PropertyDefinition<Color> WARNING =
        PropertyDefinition.of("warning", ColorConverter.INSTANCE);

    /** Accent semantic color property. */
    public static final PropertyDefinition<Color> ACCENT =
        PropertyDefinition.of("accent", ColorConverter.INSTANCE);

    /** Info semantic color property. */
    public static final PropertyDefinition<Color> INFO =
        PropertyDefinition.of("info", ColorConverter.INSTANCE);

    // ===== Surface Hierarchy =====

    /** Background surface color property. */
    public static final PropertyDefinition<Color> BACKGROUND =
        PropertyDefinition.of("background", ColorConverter.INSTANCE);

    /** Surface color property. */
    public static final PropertyDefinition<Color> SURFACE =
        PropertyDefinition.of("surface", ColorConverter.INSTANCE);

    /** Panel color property. */
    public static final PropertyDefinition<Color> PANEL =
        PropertyDefinition.of("panel", ColorConverter.INSTANCE);

    /** Foreground color property. */
    public static final PropertyDefinition<Color> FOREGROUND =
        PropertyDefinition.of("foreground", ColorConverter.INSTANCE);

    // ===== Derived Colors - Primary =====

    /** Lightened variant of primary color. */
    public static final PropertyDefinition<Color> PRIMARY_LIGHT =
        PropertyDefinition.of("primary-light", ColorConverter.INSTANCE);

    /** Darkened variant of primary color. */
    public static final PropertyDefinition<Color> PRIMARY_DARK =
        PropertyDefinition.of("primary-dark", ColorConverter.INSTANCE);

    /** Muted/desaturated variant of primary color. */
    public static final PropertyDefinition<Color> PRIMARY_MUTED =
        PropertyDefinition.of("primary-muted", ColorConverter.INSTANCE);

    // ===== Derived Colors - Secondary =====

    /** Lightened variant of secondary color. */
    public static final PropertyDefinition<Color> SECONDARY_LIGHT =
        PropertyDefinition.of("secondary-light", ColorConverter.INSTANCE);

    /** Darkened variant of secondary color. */
    public static final PropertyDefinition<Color> SECONDARY_DARK =
        PropertyDefinition.of("secondary-dark", ColorConverter.INSTANCE);

    /** Muted/desaturated variant of secondary color. */
    public static final PropertyDefinition<Color> SECONDARY_MUTED =
        PropertyDefinition.of("secondary-muted", ColorConverter.INSTANCE);

    // ===== Derived Colors - Error =====

    /** Lightened variant of error color. */
    public static final PropertyDefinition<Color> ERROR_LIGHT =
        PropertyDefinition.of("error-light", ColorConverter.INSTANCE);

    /** Darkened variant of error color. */
    public static final PropertyDefinition<Color> ERROR_DARK =
        PropertyDefinition.of("error-dark", ColorConverter.INSTANCE);

    // TODO: Add success/warning shades when widgets need them

    // ===== Text Colors =====

    /** Default text color property. */
    public static final PropertyDefinition<Color> TEXT =
        PropertyDefinition.of("text", ColorConverter.INSTANCE);

    /** Muted/subtle text color property. */
    public static final PropertyDefinition<Color> TEXT_MUTED =
        PropertyDefinition.of("text-muted", ColorConverter.INSTANCE);

    /** Disabled text color property. */
    public static final PropertyDefinition<Color> TEXT_DISABLED =
        PropertyDefinition.of("text-disabled", ColorConverter.INSTANCE);

    /** Text color for content on primary colored backgrounds. */
    public static final PropertyDefinition<Color> TEXT_ON_PRIMARY =
        PropertyDefinition.of("text-on-primary", ColorConverter.INSTANCE);

    // ===== UI Colors =====

    /** Default border color property. */
    public static final PropertyDefinition<Color> BORDER =
        PropertyDefinition.of("border", ColorConverter.INSTANCE);

    /** Focused border color property. */
    public static final PropertyDefinition<Color> BORDER_FOCUS =
        PropertyDefinition.of("border-focus", ColorConverter.INSTANCE);

    /** Muted border color property. */
    public static final PropertyDefinition<Color> BORDER_MUTED =
        PropertyDefinition.of("border-muted", ColorConverter.INSTANCE);

    /** Selection background color property. */
    public static final PropertyDefinition<Color> SELECTION_BG =
        PropertyDefinition.of("selection-bg", ColorConverter.INSTANCE);

    /** Hover background color property. */
    public static final PropertyDefinition<Color> HOVER_BG =
        PropertyDefinition.of("hover-bg", ColorConverter.INSTANCE);

    // TODO: SCROLLBAR, SCROLLBAR_HOVER when ScrollBar widget implemented
    // TODO: LINK, LINK_HOVER when Link widget exists
}

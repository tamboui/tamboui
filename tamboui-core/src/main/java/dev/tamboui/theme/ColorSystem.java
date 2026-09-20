/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.theme;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.tamboui.style.Color;

/**
 * Generated color palette from a theme.
 * <p>
 * Contains ~20-30 derived colors organized into categories:
 * <ul>
 *   <li>Base semantic: primary, secondary, error, success, warning, accent, info</li>
 *   <li>Surface hierarchy: background, surface, panel, foreground</li>
 *   <li>Shades: primary-light, primary-dark, primary-muted (similar for others)</li>
 *   <li>Text: text, text-muted, text-disabled, text-on-primary</li>
 *   <li>UI: border, border-focus, border-muted, selection-bg, hover-bg</li>
 * </ul>
 * <p>
 * Access colors by name via {@link #get(String)}.
 */
public final class ColorSystem {

    private final Map<String, Color> colors;

    ColorSystem(Map<String, Color> colors) {
        this.colors = Collections.unmodifiableMap(new HashMap<>(colors));
    }

    /**
     * Gets a color by name.
     *
     * @param name the color name (e.g., "primary", "border-focus")
     * @return the color, or null if not found
     */
    public Color get(String name) {
        return colors.get(name);
    }

    /**
     * Returns all color names in this system.
     *
     * @return set of color names
     */
    public Set<String> colorNames() {
        return colors.keySet();
    }

    /**
     * Checks if a color exists in this system.
     *
     * @param name the color name
     * @return true if the color exists
     */
    public boolean has(String name) {
        return colors.containsKey(name);
    }
}

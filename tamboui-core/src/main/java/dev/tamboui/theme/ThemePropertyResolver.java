/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.theme;

import java.util.Map;
import java.util.Optional;

import dev.tamboui.style.Color;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.StylePropertyResolver;

/**
 * StylePropertyResolver implementation backed by a ColorSystem.
 * <p>
 * Provides property-based access to theme colors. Used as fallback
 * when CSS doesn't define a property.
 */
final class ThemePropertyResolver implements StylePropertyResolver {

    private final ColorSystem colorSystem;
    private final Map<String, String> rawVariables;

    ThemePropertyResolver(ColorSystem colorSystem, Map<String, String> rawVariables) {
        this.colorSystem = colorSystem;
        this.rawVariables = rawVariables;
    }

    @Override
    public <T> Optional<T> get(PropertyDefinition<T> property) {
        String name = property.name();

        // 1. Check raw variables for overrides (non-color properties)
        if (rawVariables.containsKey(name)) {
            String rawValue = rawVariables.get(name);
            return property.convert(rawValue);
        }

        // 2. Check ColorSystem for color properties
        Color color = colorSystem.get(name);
        if (color != null) {
            // Try to cast - if this is a Color property, it will work
            try {
                @SuppressWarnings("unchecked")
                T value = (T) color;
                return Optional.of(value);
            } catch (ClassCastException e) {
                // Not a color property, continue
            }
        }

        // 3. Not found
        return Optional.empty();
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.style.Color;

import java.util.Map;
import java.util.Optional;

/**
 * CSS adapter for the core ColorConverter.
 * <p>
 * This class adapts the core {@link dev.tamboui.style.ColorConverter} to the
 * CSS module's {@link PropertyConverter} interface, adding variable resolution.
 *
 * @deprecated Use {@link dev.tamboui.style.ColorConverter} directly when possible.
 *             This adapter is maintained for CSS cascade resolution compatibility.
 */
@Deprecated
public final class ColorConverter implements PropertyConverter<Color> {

    /**
     * Singleton instance.
     */
    public static final ColorConverter INSTANCE = new ColorConverter();

    private static final dev.tamboui.style.ColorConverter CORE_CONVERTER =
        dev.tamboui.style.ColorConverter.INSTANCE;

    private ColorConverter() {
    }

    @Override
    public Optional<Color> convert(String value, Map<String, String> variables) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        String resolved = PropertyConverter.resolveVariables(value.trim(), variables);
        return CORE_CONVERTER.convert(resolved);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.spinner;

import dev.tamboui.style.PropertyConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts string values to {@link SpinnerStyle} enum values.
 * <p>
 * Supported values (case-insensitive, hyphens or underscores):
 * <ul>
 *   <li>{@code dots} - braille dot pattern</li>
 *   <li>{@code line} - classic -\|/</li>
 *   <li>{@code arc} - quarter-circle characters</li>
 *   <li>{@code circle} - clock-position characters</li>
 *   <li>{@code bouncing-bar} - bouncing bar [=== ]</li>
 *   <li>{@code toggle} - two-state toggle</li>
 *   <li>{@code gauge} - horizontal block fill</li>
 *   <li>{@code vertical-gauge} - vertical bar growth</li>
 *   <li>{@code arrows} - rotating arrow directions</li>
 *   <li>{@code clock} - clock face emoji</li>
 *   <li>{@code moon} - moon phase emoji</li>
 *   <li>{@code square-corners} - rotating square corners</li>
 *   <li>{@code growing-dots} - braille dots filling</li>
 *   <li>{@code bouncing-ball} - bouncing ball in braille</li>
 * </ul>
 */
public final class SpinnerStyleConverter implements PropertyConverter<SpinnerStyle> {

    /**
     * Singleton instance of the spinner style converter.
     */
    public static final SpinnerStyleConverter INSTANCE = new SpinnerStyleConverter();

    private static final Map<String, SpinnerStyle> VALUES = new HashMap<>();

    static {
        for (SpinnerStyle style : SpinnerStyle.values()) {
            // Add lowercase with hyphens (e.g., "bouncing-bar")
            String hyphenated = style.name().toLowerCase().replace('_', '-');
            VALUES.put(hyphenated, style);
            // Also support underscores (e.g., "bouncing_bar")
            VALUES.put(style.name().toLowerCase(), style);
        }
    }

    private SpinnerStyleConverter() {
    }

    @Override
    public Optional<SpinnerStyle> convert(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(VALUES.get(value.trim().toLowerCase()));
    }
}

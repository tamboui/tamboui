/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.style.Width;

import java.util.Map;
import java.util.Optional;

/**
 * Converts CSS width values to Width.
 * <p>
 * Supports the following values:
 * <ul>
 *   <li>{@code fill} - element fills container width (default)</li>
 *   <li>{@code fit} - element width fits its content</li>
 *   <li>Percentage values: {@code 50%}, {@code 100%}</li>
 *   <li>Decimal percentages: {@code 0.5}, {@code 0.25}</li>
 *   <li>Fixed character counts: {@code 20}, {@code 20ch}</li>
 * </ul>
 */
public final class WidthConverter implements PropertyConverter<Width> {

    /** Singleton instance. */
    public static final WidthConverter INSTANCE = new WidthConverter();

    private WidthConverter() {
    }

    @Override
    public Optional<Width> convert(String value, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim(), variables).toLowerCase();

        // Named values
        switch (resolved) {
            case "fill":
                return Optional.of(Width.FILL);
            case "fit":
                return Optional.of(Width.FIT);
        }

        // Percentage with % suffix
        if (resolved.endsWith("%")) {
            try {
                double percent = Double.parseDouble(resolved.substring(0, resolved.length() - 1));
                return Optional.of(Width.percent(percent));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        // Fixed with ch suffix (character units)
        if (resolved.endsWith("ch")) {
            try {
                int chars = Integer.parseInt(resolved.substring(0, resolved.length() - 2));
                return Optional.of(Width.fixed(chars));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        // Plain number - could be decimal (0.5 = 50%) or integer (20 = 20 chars)
        try {
            double num = Double.parseDouble(resolved);
            if (num > 0 && num < 1) {
                // Decimal between 0 and 1 is treated as percentage
                return Optional.of(Width.percent(num));
            } else {
                // Integer treated as fixed character width
                return Optional.of(Width.fixed((int) num));
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

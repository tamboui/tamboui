/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts string color values to TamboUI Color objects.
 * <p>
 * Supported formats:
 * <ul>
 *   <li>Named colors: red, green, blue, etc.</li>
 *   <li>Hex colors: #RGB, #RRGGBB</li>
 *   <li>RGB function: rgb(r, g, b)</li>
 *   <li>Indexed colors: indexed(0-255)</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Standalone usage
 * Optional<Color> color = ColorConverter.INSTANCE.convert("#FF0000");
 *
 * // With PropertyKey
 * PropertyKey<Color> BORDER_COLOR = PropertyKey.of("border-color", ColorConverter.INSTANCE);
 * }</pre>
 */
public final class ColorConverter implements PropertyConverter<Color> {

    /**
     * Singleton instance of the color converter.
     */
    public static final ColorConverter INSTANCE = new ColorConverter();

    private static final Map<String, Color> NAMED_COLORS = new HashMap<>();
    private static final Pattern HEX_PATTERN = Pattern.compile("#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})");
    private static final Pattern RGB_PATTERN = Pattern.compile("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");
    private static final Pattern INDEXED_PATTERN = Pattern.compile("indexed\\s*\\(\\s*(\\d+)\\s*\\)");

    static {
        // Standard ANSI colors
        NAMED_COLORS.put("black", Color.BLACK);
        NAMED_COLORS.put("red", Color.RED);
        NAMED_COLORS.put("green", Color.GREEN);
        NAMED_COLORS.put("yellow", Color.YELLOW);
        NAMED_COLORS.put("blue", Color.BLUE);
        NAMED_COLORS.put("magenta", Color.MAGENTA);
        NAMED_COLORS.put("cyan", Color.CYAN);
        NAMED_COLORS.put("white", Color.WHITE);
        NAMED_COLORS.put("gray", Color.GRAY);
        NAMED_COLORS.put("grey", Color.GRAY);
        NAMED_COLORS.put("dark-gray", Color.DARK_GRAY);
        NAMED_COLORS.put("dark-grey", Color.DARK_GRAY);
        NAMED_COLORS.put("light-gray", Color.GRAY);
        NAMED_COLORS.put("light-grey", Color.GRAY);

        // Bright variants
        NAMED_COLORS.put("bright-black", Color.GRAY);
        NAMED_COLORS.put("bright-red", Color.LIGHT_RED);
        NAMED_COLORS.put("bright-green", Color.LIGHT_GREEN);
        NAMED_COLORS.put("bright-yellow", Color.LIGHT_YELLOW);
        NAMED_COLORS.put("bright-blue", Color.LIGHT_BLUE);
        NAMED_COLORS.put("bright-magenta", Color.LIGHT_MAGENTA);
        NAMED_COLORS.put("bright-cyan", Color.LIGHT_CYAN);
        NAMED_COLORS.put("bright-white", Color.WHITE);

        // Light variants (aliases)
        NAMED_COLORS.put("light-red", Color.LIGHT_RED);
        NAMED_COLORS.put("light-green", Color.LIGHT_GREEN);
        NAMED_COLORS.put("light-yellow", Color.LIGHT_YELLOW);
        NAMED_COLORS.put("light-blue", Color.LIGHT_BLUE);
        NAMED_COLORS.put("light-magenta", Color.LIGHT_MAGENTA);
        NAMED_COLORS.put("light-cyan", Color.LIGHT_CYAN);
    }

    private ColorConverter() {
    }

    @Override
    public Optional<Color> convert(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase();

        // Named color
        Color named = NAMED_COLORS.get(normalized);
        if (named != null) {
            return Optional.of(named);
        }

        // Hex color
        Matcher hexMatcher = HEX_PATTERN.matcher(normalized);
        if (hexMatcher.matches()) {
            return Optional.of(parseHex(hexMatcher.group(1)));
        }

        // RGB function
        Matcher rgbMatcher = RGB_PATTERN.matcher(normalized);
        if (rgbMatcher.matches()) {
            int r = Integer.parseInt(rgbMatcher.group(1));
            int g = Integer.parseInt(rgbMatcher.group(2));
            int b = Integer.parseInt(rgbMatcher.group(3));
            return Optional.of(Color.rgb(r, g, b));
        }

        // Indexed color
        Matcher indexedMatcher = INDEXED_PATTERN.matcher(normalized);
        if (indexedMatcher.matches()) {
            int index = Integer.parseInt(indexedMatcher.group(1));
            if (index >= 0 && index <= 255) {
                return Optional.of(Color.indexed(index));
            }
        }

        return Optional.empty();
    }

    private Color parseHex(String hex) {
        if (hex.length() == 3) {
            // #RGB -> #RRGGBB
            char r = hex.charAt(0);
            char g = hex.charAt(1);
            char b = hex.charAt(2);
            hex = "" + r + r + g + g + b + b;
        }

        int rgb = Integer.parseInt(hex, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return Color.rgb(r, g, b);
    }
}

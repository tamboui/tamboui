/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Optional;

/**
 * Converter for border character properties.
 * <p>
 * Border characters can be:
 * <ul>
 * <li>Quoted strings (any content, including empty) - quotes are stripped</li>
 * <li>Single characters or short unicode sequences (up to 4 chars)</li>
 * </ul>
 * <p>
 * Invalid values are rejected:
 * <ul>
 * <li>Empty unquoted strings</li>
 * <li>Long strings that look like parser errors (e.g., "height: 3")</li>
 * </ul>
 */
public final class BorderCharConverter implements PropertyConverter<String> {

    /**
     * Singleton instance.
     */
    public static final BorderCharConverter INSTANCE = new BorderCharConverter();

    private BorderCharConverter() {
    }

    @Override
    public Optional<String> convert(String value) {
        if (value == null) {
            return Optional.empty();
        }

        String trimmed = value.trim();

        // Check if quoted
        if (isQuoted(trimmed)) {
            // Quoted strings are always valid - strip quotes and return
            return Optional.of(parseQuotedChar(trimmed));
        }

        // Unquoted: must be non-empty and short
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        // Reject long strings or strings with colons (likely parser errors)
        if (trimmed.length() > 4 || trimmed.contains(":")) {
            return Optional.empty();
        }

        return Optional.of(trimmed);
    }

    private boolean isQuoted(String value) {
        if (value.length() < 2) {
            return false;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        return (first == '"' || first == '\'') && first == last;
    }

    private String parseQuotedChar(String value) {
        // Strip the surrounding quotes
        return value.substring(1, value.length() - 1);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import dev.tamboui.style.PropertyConverter;

import java.util.Optional;

/**
 * Converts string values to {@link ContentAlignment} enum values.
 * <p>
 * Supported values (case-insensitive):
 * <ul>
 *   <li>{@code top-left} — top-left corner</li>
 *   <li>{@code top-center} — top center</li>
 *   <li>{@code top-right} — top-right corner</li>
 *   <li>{@code center-left} — center-left</li>
 *   <li>{@code center} — centered (both axes)</li>
 *   <li>{@code center-right} — center-right</li>
 *   <li>{@code bottom-left} — bottom-left corner</li>
 *   <li>{@code bottom-center} — bottom center</li>
 *   <li>{@code bottom-right} — bottom-right corner</li>
 *   <li>{@code stretch} — fill the entire container</li>
 * </ul>
 */
public final class ContentAlignmentConverter implements PropertyConverter<ContentAlignment> {

    /**
     * Singleton instance of the content alignment converter.
     */
    public static final ContentAlignmentConverter INSTANCE = new ContentAlignmentConverter();

    private ContentAlignmentConverter() {
    }

    @Override
    public Optional<ContentAlignment> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase();

        switch (normalized) {
            case "top-left":
                return Optional.of(ContentAlignment.TOP_LEFT);
            case "top-center":
                return Optional.of(ContentAlignment.TOP_CENTER);
            case "top-right":
                return Optional.of(ContentAlignment.TOP_RIGHT);
            case "center-left":
                return Optional.of(ContentAlignment.CENTER_LEFT);
            case "center":
                return Optional.of(ContentAlignment.CENTER);
            case "center-right":
                return Optional.of(ContentAlignment.CENTER_RIGHT);
            case "bottom-left":
                return Optional.of(ContentAlignment.BOTTOM_LEFT);
            case "bottom-center":
                return Optional.of(ContentAlignment.BOTTOM_CENTER);
            case "bottom-right":
                return Optional.of(ContentAlignment.BOTTOM_RIGHT);
            case "stretch":
                return Optional.of(ContentAlignment.STRETCH);
            default:
                return Optional.empty();
        }
    }
}

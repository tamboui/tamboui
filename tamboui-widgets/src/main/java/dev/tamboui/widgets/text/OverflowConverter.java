/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.text;

import dev.tamboui.style.PropertyConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts string values to {@link Overflow} enum values.
 * <p>
 * Supported values:
 * <ul>
 *   <li>{@code clip} - silent truncation at boundary</li>
 *   <li>{@code wrap}, {@code wrap-character} - wrap at character boundaries</li>
 *   <li>{@code wrap-word} - wrap at word boundaries</li>
 *   <li>{@code ellipsis} - truncate with "..." at end</li>
 *   <li>{@code ellipsis-start} - truncate with "..." at start</li>
 *   <li>{@code ellipsis-middle} - truncate with "..." in middle</li>
 * </ul>
 */
public final class OverflowConverter implements PropertyConverter<Overflow> {

    /**
     * Singleton instance of the overflow converter.
     */
    public static final OverflowConverter INSTANCE = new OverflowConverter();

    private static final Map<String, Overflow> VALUES = new HashMap<>();

    static {
        VALUES.put("clip", Overflow.CLIP);
        VALUES.put("wrap", Overflow.WRAP_CHARACTER);
        VALUES.put("wrap-character", Overflow.WRAP_CHARACTER);
        VALUES.put("wrap-word", Overflow.WRAP_WORD);
        VALUES.put("ellipsis", Overflow.ELLIPSIS);
        VALUES.put("ellipsis-start", Overflow.ELLIPSIS_START);
        VALUES.put("ellipsis-middle", Overflow.ELLIPSIS_MIDDLE);
    }

    private OverflowConverter() {
    }

    @Override
    public Optional<Overflow> convert(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(VALUES.get(value.trim().toLowerCase()));
    }
}

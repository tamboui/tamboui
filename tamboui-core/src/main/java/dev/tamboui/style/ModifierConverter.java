/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Converts string values to sets of {@link Modifier} values.
 * <p>
 * Supported values (case-insensitive, space-separated for multiple):
 * <ul>
 *   <li>{@code bold}</li>
 *   <li>{@code dim}</li>
 *   <li>{@code italic}</li>
 *   <li>{@code underlined} or {@code underline}</li>
 *   <li>{@code reversed} or {@code reverse}</li>
 *   <li>{@code crossed-out} or {@code strikethrough}</li>
 *   <li>{@code hidden}</li>
 *   <li>{@code blink} or {@code slow-blink}</li>
 *   <li>{@code rapid-blink}</li>
 * </ul>
 * Multiple values can be space-separated: "bold italic underlined"
 */
public final class ModifierConverter implements PropertyConverter<Set<Modifier>> {

    /**
     * Singleton instance of the modifier converter.
     */
    public static final ModifierConverter INSTANCE = new ModifierConverter();

    private static final Map<String, Modifier> MODIFIER_MAP = new HashMap<>();

    static {
        MODIFIER_MAP.put("bold", Modifier.BOLD);
        MODIFIER_MAP.put("dim", Modifier.DIM);
        MODIFIER_MAP.put("italic", Modifier.ITALIC);
        MODIFIER_MAP.put("underlined", Modifier.UNDERLINED);
        MODIFIER_MAP.put("underline", Modifier.UNDERLINED);
        MODIFIER_MAP.put("reversed", Modifier.REVERSED);
        MODIFIER_MAP.put("reverse", Modifier.REVERSED);
        MODIFIER_MAP.put("crossed-out", Modifier.CROSSED_OUT);
        MODIFIER_MAP.put("strikethrough", Modifier.CROSSED_OUT);
        MODIFIER_MAP.put("hidden", Modifier.HIDDEN);
        MODIFIER_MAP.put("blink", Modifier.SLOW_BLINK);
        MODIFIER_MAP.put("slow-blink", Modifier.SLOW_BLINK);
        MODIFIER_MAP.put("rapid-blink", Modifier.RAPID_BLINK);
    }

    private ModifierConverter() {
    }

    @Override
    public Optional<Set<Modifier>> convert(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        String[] parts = value.trim().toLowerCase().split("\\s+");

        Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        for (String part : parts) {
            Modifier modifier = MODIFIER_MAP.get(part);
            if (modifier != null) {
                modifiers.add(modifier);
            }
        }

        return modifiers.isEmpty() ? Optional.empty() : Optional.of(modifiers);
    }
}

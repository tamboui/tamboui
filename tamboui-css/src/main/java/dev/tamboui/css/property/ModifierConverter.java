/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.style.Modifier;

import java.util.*;

/**
 * Converts CSS text-style values to TamboUI Modifier sets.
 * <p>
 * Supported values:
 * <ul>
 *   <li>bold</li>
 *   <li>dim</li>
 *   <li>italic</li>
 *   <li>underlined / underline</li>
 *   <li>reversed / reverse</li>
 *   <li>crossed-out / strikethrough</li>
 *   <li>hidden</li>
 * </ul>
 * Multiple values can be space-separated: "bold italic underlined"
 */
public final class ModifierConverter implements PropertyConverter<Set<Modifier>> {

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

    @Override
    public Optional<Set<Modifier>> convert(String value, Map<String, String> variables) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim().toLowerCase(), variables);
        String[] parts = resolved.split("\\s+");

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

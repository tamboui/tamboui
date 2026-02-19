/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.common;

import java.util.Optional;

import dev.tamboui.style.PropertyConverter;

/**
 * Policy for displaying a scrollbar.
 */
public enum ScrollBarPolicy {
    /** Never show the scrollbar. */
    NONE,
    /** Always show the scrollbar. */
    ALWAYS,
    /** Show the scrollbar only when content exceeds the viewport. */
    AS_NEEDED;

    /**
     * CSS property converter for scrollbar policy.
     * Accepts values: "none", "always", "as-needed".
     */
    public static final PropertyConverter<ScrollBarPolicy> CONVERTER = value -> {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase();
        for (ScrollBarPolicy policy : ScrollBarPolicy.values()) {
            if (policy.name().toLowerCase().replace('_', '-').equals(normalized)) {
                return Optional.of(policy);
            }
        }
        return Optional.empty();
    };
}

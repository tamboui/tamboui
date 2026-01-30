/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

/**
 * Direction for bidirectional expansion effects.
 * <p>
 * ExpandDirection specifies whether an expansion effect grows horizontally
 * (left and right simultaneously) or vertically (up and down simultaneously)
 * from the center.
 */
public enum ExpandDirection {
    /**
     * Expands horizontally from center (left and right simultaneously).
     */
    HORIZONTAL,

    /**
     * Expands vertically from center (up and down simultaneously).
     */
    VERTICAL
}

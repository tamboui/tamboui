/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

/**
 * Scroll mode for list widgets.
 */
public enum ScrollMode {
    /** No automatic scrolling. */
    NONE,
    /** Automatically scroll to keep the selected item visible. */
    AUTO_SCROLL,
    /** Always scroll to show the last items. */
    SCROLL_TO_END,
    /** Auto-scroll to end, but pause when the user scrolls away. */
    STICKY_SCROLL
}

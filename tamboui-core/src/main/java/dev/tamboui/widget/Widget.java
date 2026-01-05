/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widget;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

/**
 * A widget that can render itself to a buffer.
 */
@FunctionalInterface
public interface Widget {

    /**
     * Renders this widget to the given buffer area.
     */
    void render(Rect area, Buffer buffer);
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

/**
 * A piece of rendered markdown that knows its own height for a given width and
 * how to draw itself into a buffer at a target rectangle. Two concrete kinds:
 * {@link LinesChunk} for prose-like content already wrapped to lines, and
 * {@link WidgetChunk} for sub-widgets (code blocks, tables) that delegate to
 * existing TamboUI widgets.
 */
public abstract class RenderedChunk {

    RenderedChunk() {
    }

    /**
     * Returns the height in rows this chunk needs at the given width.
     *
     * @param width available width in columns
     * @return height in rows
     */
    public abstract int height(int width);

    /**
     * Renders this chunk into {@code area}.
     *
     * @param area target rectangle
     * @param buffer buffer to draw into
     */
    public abstract void render(Rect area, Buffer buffer);
}

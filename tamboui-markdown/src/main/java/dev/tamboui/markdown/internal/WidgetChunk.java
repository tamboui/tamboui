/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.Widget;

/**
 * A chunk that delegates rendering to an existing {@link Widget} (for example
 * a {@code Block}-wrapped {@code Paragraph} for fenced code blocks, or a
 * {@code Table} for GFM tables). The chunk knows its own fixed height; layout
 * is responsible for computing it at construction time.
 */
public final class WidgetChunk extends RenderedChunk {

    private final Widget widget;
    private final int height;

    /**
     * Creates a widget-backed chunk.
     *
     * @param widget the widget to render
     * @param height the height in rows the widget needs
     */
    public WidgetChunk(Widget widget, int height) {
        this.widget = widget;
        this.height = height;
    }

    @Override
    public int height(int width) {
        return height;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        widget.render(area, buffer);
    }
}

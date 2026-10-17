/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
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

    /**
     * Draws the widget into a buffer {@code width} columns wide and reads its rows back as lines, keeping the style of
     * each cell, for places that can only hold lines, such as a block quote or a list item. The blank cells at the end
     * of a row are left out.
     */
    List<Line> toLines(int width) {
        List<Line> lines = new ArrayList<>(Math.max(0, height));
        if (width <= 0 || height <= 0) {
            return lines;
        }
        Buffer buffer = Buffer.empty(new Rect(0, 0, width, height));
        widget.render(buffer.area(), buffer);
        for (int y = 0; y < height; y++) {
            int end = width;
            while (end > 0 && buffer.get(end - 1, y).isEmpty()) {
                end--;
            }
            List<Span> spans = new ArrayList<>();
            StringBuilder run = new StringBuilder();
            Style runStyle = null;
            for (int x = 0; x < end; x++) {
                Cell cell = buffer.get(x, y);
                if (cell.isContinuation()) {
                    continue;
                }
                if (runStyle != null && !runStyle.equals(cell.style())) {
                    spans.add(Span.styled(run.toString(), runStyle));
                    run.setLength(0);
                }
                runStyle = cell.style();
                run.append(cell.symbol());
            }
            if (runStyle != null) {
                spans.add(Span.styled(run.toString(), runStyle));
            }
            lines.add(Line.from(spans));
        }
        return lines;
    }
}

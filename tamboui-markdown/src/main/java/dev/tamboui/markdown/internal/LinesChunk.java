/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.text.Line;

/**
 * A chunk made of pre-wrapped lines. The lines are assumed to already fit the
 * target width (the layout pass is responsible for wrapping). Each line is
 * drawn at the chunk's left edge.
 */
public final class LinesChunk extends RenderedChunk {

    private final List<Line> lines;

    /**
     * Creates a chunk wrapping the given lines.
     *
     * @param lines the lines to render; must already fit the target width
     */
    public LinesChunk(List<Line> lines) {
        this.lines = lines;
    }

    /**
     * Returns the lines this chunk will render.
     *
     * @return the lines
     */
    public List<Line> lines() {
        return lines;
    }

    @Override
    public int height(int width) {
        return lines.size();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        int maxRows = Math.min(lines.size(), area.height());
        for (int i = 0; i < maxRows; i++) {
            buffer.setLine(area.left(), area.top() + i, lines.get(i));
        }
    }
}

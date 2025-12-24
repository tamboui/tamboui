/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.paragraph;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.Widget;
import dev.tamboui.widgets.block.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A paragraph widget for displaying styled text.
 */
public final class Paragraph implements Widget {

    private final Text text;
    private final Block block;
    private final Style style;
    private final Alignment alignment;
    private final Wrap wrap;
    private final int scroll;

    private Paragraph(Builder builder) {
        this.text = builder.text;
        this.block = builder.block;
        this.style = builder.style;
        this.alignment = builder.alignment;
        this.wrap = builder.wrap;
        this.scroll = builder.scroll;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Paragraph from(String text) {
        return builder().text(Text.from(text)).build();
    }

    public static Paragraph from(Text text) {
        return builder().text(text).build();
    }

    public static Paragraph from(Line line) {
        return builder().text(Text.from(line)).build();
    }

    public static Paragraph from(Span span) {
        return builder().text(Text.from(span)).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        // Apply style to area
        buffer.setStyle(area, style);

        // Render block if present
        Rect textArea = area;
        if (block != null) {
            block.render(area, buffer);
            textArea = block.inner(area);
        }

        if (textArea.isEmpty()) {
            return;
        }

        // Apply style to text area (after block renders, matching ratatui behavior)
        buffer.setStyle(textArea, style);

        // Get lines to render (wrapped if needed)
        List<Line> lines = wrap == Wrap.NONE
            ? text.lines()
            : wrapLines(text.lines(), textArea.width());

        // Apply scroll
        int startLine = Math.min(scroll, lines.size());
        int visibleLines = Math.min(lines.size() - startLine, textArea.height());

        for (int i = 0; i < visibleLines; i++) {
            Line line = lines.get(startLine + i);
            int y = textArea.top() + i;

            // Calculate x position based on alignment
            int lineWidth = line.width();
            int x;
            switch (alignment) {
                case LEFT:
                    x = textArea.left();
                    break;
                case CENTER:
                    x = textArea.left() + (textArea.width() - lineWidth) / 2;
                    break;
                case RIGHT:
                default:
                    x = textArea.right() - lineWidth;
                    break;
            }

            buffer.setLine(x, y, line);
        }
    }

    private List<Line> wrapLines(List<Line> lines, int maxWidth) {
        if (maxWidth <= 0) {
            return Collections.emptyList();
        }

        List<Line> wrapped = new ArrayList<>();

        for (Line line : lines) {
            if (line.width() <= maxWidth) {
                wrapped.add(line);
                continue;
            }

            // Simple character-based wrapping
            List<Span> currentSpans = new ArrayList<>();
            int currentWidth = 0;

            for (Span span : line.spans()) {
                String content = span.content();
                Style spanStyle = span.style();

                int i = 0;
                while (i < content.length()) {
                    int remainingWidth = maxWidth - currentWidth;

                    if (remainingWidth <= 0) {
                        // Start new line
                        wrapped.add(Line.from(currentSpans));
                        currentSpans = new ArrayList<>();
                        currentWidth = 0;
                        remainingWidth = maxWidth;
                    }

                    // Find how many characters fit
                    int end = Math.min(i + remainingWidth, content.length());
                    String chunk = content.substring(i, end);

                    currentSpans.add(new Span(chunk, spanStyle));
                    currentWidth += chunk.length();
                    i = end;
                }
            }

            if (!currentSpans.isEmpty()) {
                wrapped.add(Line.from(currentSpans));
            }
        }

        return wrapped;
    }

    public static final class Builder {
        private Text text = Text.empty();
        private Block block;
        private Style style = Style.EMPTY;
        private Alignment alignment = Alignment.LEFT;
        private Wrap wrap = Wrap.NONE;
        private int scroll = 0;

        private Builder() {}

        public Builder text(Text text) {
            this.text = text;
            return this;
        }

        public Builder text(String text) {
            this.text = Text.from(text);
            return this;
        }

        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder alignment(Alignment alignment) {
            this.alignment = alignment;
            return this;
        }

        public Builder left() {
            return alignment(Alignment.LEFT);
        }

        public Builder centered() {
            return alignment(Alignment.CENTER);
        }

        public Builder right() {
            return alignment(Alignment.RIGHT);
        }

        public Builder wrap(Wrap wrap) {
            this.wrap = wrap;
            return this;
        }

        public Builder scroll(int scroll) {
            this.scroll = Math.max(0, scroll);
            return this;
        }

        public Paragraph build() {
            return new Paragraph(this);
        }
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.figlet;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

import java.util.List;

/**
 * A widget that renders FIGlet (ASCII art) text using bundled fonts.
 *
 * <p>No external {@code figlet} binary is required; fonts are loaded from resources.</p>
 */
public final class Figlet implements Widget {

    private final String text;
    private final FigletFont font;
    private final boolean kerning;
    private final int letterSpacing;
    private final Alignment alignment;
    private final Block block;
    private final Style style;

    private Figlet(Builder builder) {
        this.text = builder.text;
        this.font = builder.font;
        this.kerning = builder.kerning;
        this.letterSpacing = builder.letterSpacing;
        this.alignment = builder.alignment;
        this.block = builder.block;
        this.style = builder.style;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Figlet of(String text) {
        return builder().text(text).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        buffer.setStyle(area, style);

        Rect textArea = area;
        if (block != null) {
            block.render(area, buffer);
            textArea = block.inner(area);
        }
        if (textArea.isEmpty()) {
            return;
        }

        List<String> lines = font.render(text, kerning, letterSpacing, true);
        int visibleLines = Math.min(lines.size(), textArea.height());
        int maxWidth = textArea.width();
        for (int i = 0; i < visibleLines; i++) {
            String line = lines.get(i);
            // Truncate line to fit within the text area
            if (line.length() > maxWidth) {
                line = line.substring(0, maxWidth);
            }
            int y = textArea.top() + i;

            int x;
            switch (alignment) {
                case CENTER:
                    x = textArea.left() + (maxWidth - line.length()) / 2;
                    break;
                case RIGHT:
                    x = textArea.left() + maxWidth - line.length();
                    break;
                case LEFT:
                default:
                    x = textArea.left();
                    break;
            }

            // Ensure x is within bounds
            x = Math.max(textArea.left(), Math.min(x, textArea.right() - line.length()));
            buffer.setString(x, y, line, style);
        }
    }

    public static final class Builder {
        private String text = "";
        private FigletFont font = FigletFont.bundled(BundledFigletFont.STANDARD);
        private boolean kerning = true;
        private int letterSpacing = 0;
        private Alignment alignment = Alignment.LEFT;
        private Block block;
        private Style style = Style.EMPTY;

        private Builder() {}

        public Builder text(String text) {
            this.text = text == null ? "" : text;
            return this;
        }

        public Builder font(FigletFont font) {
            this.font = font;
            return this;
        }

        public Builder font(BundledFigletFont font) {
            this.font = FigletFont.bundled(font);
            return this;
        }

        public Builder kerning(boolean kerning) {
            this.kerning = kerning;
            return this;
        }

        public Builder letterSpacing(int letterSpacing) {
            this.letterSpacing = Math.max(0, letterSpacing);
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

        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Figlet build() {
            return new Figlet(this);
        }
    }
}


/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.paragraph;

import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ContinuationIndent;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.text.TextWrapOptions;
import dev.tamboui.text.TextWrapper;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

/**
 * A paragraph widget for displaying styled text.
 * <p>
 * Supports style-aware properties: {@code text-overflow}, {@code text-align},
 * {@code background}, and {@code color}.
 */
public final class Paragraph implements Widget {

    private final Text text;
    private final Block block;
    private final Style style;
    private final Alignment alignment;
    private final Overflow overflow;
    private final int scroll;
    private final ContinuationIndent continuationIndent;

    private Paragraph(Builder builder) {
        this.text = builder.text;
        this.block = builder.block;
        this.alignment = builder.resolveAlignment();
        this.overflow = builder.resolveOverflow();
        this.continuationIndent = builder.continuationIndent;
        this.scroll = builder.scroll;

        Color resolvedBackground = builder.resolveBackground();
        Color resolvedForeground = builder.resolveForeground();
        Style baseStyle = builder.style;
        if (resolvedBackground != null) {
            baseStyle = baseStyle.bg(resolvedBackground);
        }
        if (resolvedForeground != null) {
            baseStyle = baseStyle.fg(resolvedForeground);
        }
        this.style = baseStyle;
    }

    /**
     * Creates a new paragraph builder.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a paragraph from a string.
     *
     * @param text the text content
     * @return a new Paragraph
     */
    public static Paragraph from(String text) {
        return builder().text(Text.from(text)).build();
    }

    /**
     * Creates a paragraph from a text.
     *
     * @param text the text content
     * @return a new Paragraph
     */
    public static Paragraph from(Text text) {
        return builder().text(text).build();
    }

    /**
     * Creates a paragraph from a line.
     *
     * @param line the line content
     * @return a new Paragraph
     */
    public static Paragraph from(Line line) {
        return builder().text(Text.from(line)).build();
    }

    /**
     * Creates a paragraph from a span.
     *
     * @param span the span content
     * @return a new Paragraph
     */
    public static Paragraph from(Span span) {
        return builder().text(Text.from(span)).build();
    }

    /**
     * Returns the continuation indent strategy used when wrapping lines.
     *
     * @return the continuation indent strategy
     */
    public ContinuationIndent continuationIndent() {
        return continuationIndent;
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

        // Get lines to render based on overflow mode
        List<Line> lines = processLines(text.lines(), textArea.width());

        // Apply scroll, clamped so the furthest position keeps the last page of
        // content visible instead of scrolling everything above the viewport
        int startLine = Math.min(scroll, Math.max(0, lines.size() - textArea.height()));
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

    private List<Line> processLines(List<Line> lines, int maxWidth) {
        TextWrapOptions options = TextWrapOptions.of(overflow, continuationIndent);
        return TextWrapper.wrap(lines, maxWidth, options);
    }

    /**
     * Builder for {@link Paragraph}.
     */
    public static final class Builder {
        private Text text = Text.empty();
        private Block block;
        private Style style = Style.EMPTY;
        private int scroll = 0;
        private ContinuationIndent continuationIndent = ContinuationIndent.HANGING;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties (resolved via styleResolver in build())
        private Alignment alignment;
        private Overflow overflow;
        private Color background;
        private Color foreground;

        private Builder() {}

        /**
         * Sets the text content.
         *
         * @param text the text content
         * @return this builder
         */
        public Builder text(Text text) {
            this.text = text;
            return this;
        }

        /**
         * Sets the text content from a string.
         *
         * @param text the text content
         * @return this builder
         */
        public Builder text(String text) {
            this.text = Text.from(text);
            return this;
        }

        /**
         * Wraps the paragraph in a block.
         *
         * @param block the block to wrap in
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the base style.
         *
         * @param style the base style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the text alignment.
         *
         * @param alignment the text alignment
         * @return this builder
         */
        public Builder alignment(Alignment alignment) {
            this.alignment = alignment;
            return this;
        }

        /**
         * Sets left alignment.
         *
         * @return this builder
         */
        public Builder left() {
            return alignment(Alignment.LEFT);
        }

        /**
         * Sets center alignment.
         *
         * @return this builder
         */
        public Builder centered() {
            return alignment(Alignment.CENTER);
        }

        /**
         * Sets right alignment.
         *
         * @return this builder
         */
        public Builder right() {
            return alignment(Alignment.RIGHT);
        }

        /**
         * Sets the overflow mode.
         *
         * @param overflow the overflow mode
         * @return this builder
         */
        public Builder overflow(Overflow overflow) {
            this.overflow = overflow;
            return this;
        }

        /**
         * Sets the background color.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.background = color;
            return this;
        }

        /**
         * Sets the foreground (text) color.
         *
         * @param color the foreground color
         * @return this builder
         */
        public Builder foreground(Color color) {
            this.foreground = color;
            return this;
        }

        /**
         * Sets the scroll offset.
         *
         * @param scroll the number of lines to skip from the top
         * @return this builder
         */
        public Builder scroll(int scroll) {
            this.scroll = Math.max(0, scroll);
            return this;
        }

        /**
         * Sets the continuation indent strategy used when wrapping lines.
         *
         * @param continuationIndent the continuation indent strategy, or null for default ({@link ContinuationIndent#HANGING})
         * @return this builder
         */
        public Builder continuationIndent(ContinuationIndent continuationIndent) {
            this.continuationIndent = continuationIndent != null ? continuationIndent : ContinuationIndent.HANGING;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code text-overflow} and {@code text-align}
         * will fall back to resolved values if not set programmatically.
         *
         * @param resolver the property resolver
         * @return this builder
         */
        public Builder styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
            return this;
        }

        /**
         * Builds the paragraph.
         *
         * @return a new Paragraph
         */
        public Paragraph build() {
            return new Paragraph(this);
        }

        // Resolution helpers
        private Alignment resolveAlignment() {
            return styleResolver.resolve(StandardProperties.TEXT_ALIGN, alignment);
        }

        private Overflow resolveOverflow() {
            return styleResolver.resolve(StandardProperties.TEXT_OVERFLOW, overflow);
        }

        private Color resolveBackground() {
            return styleResolver.resolve(StandardProperties.BACKGROUND, background);
        }

        private Color resolveForeground() {
            return styleResolver.resolve(StandardProperties.COLOR, foreground);
        }
    }
}

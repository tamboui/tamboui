/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.paragraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.PropertyKey;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardPropertyKeys;
import dev.tamboui.style.Style;
import dev.tamboui.style.StyledProperty;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.text.Overflow;
import dev.tamboui.widgets.text.OverflowConverter;

/**
 * A paragraph widget for displaying styled text.
 * <p>
 * Supports style-aware properties: {@code text-overflow}, {@code text-align},
 * {@code background}, and {@code color}.
 */
public final class Paragraph implements Widget {

    /**
     * Property key for text-overflow property.
     */
    public static final PropertyKey<Overflow> TEXT_OVERFLOW =
            PropertyKey.of("text-overflow", OverflowConverter.INSTANCE);

    private static final String ELLIPSIS = "...";

    private final Text text;
    private final Block block;
    private final Style style;
    private final Alignment alignment;
    private final Overflow overflow;
    private final int scroll;

    private Paragraph(Builder builder) {
        this.text = builder.text;
        this.block = builder.block;
        this.alignment = builder.alignment.resolve();
        this.overflow = builder.overflow.resolve();
        this.scroll = builder.scroll;

        Color resolvedBackground = builder.background.resolve();
        Color resolvedForeground = builder.foreground.resolve();
        Style baseStyle = builder.style;
        if (resolvedBackground != null) {
            baseStyle = baseStyle.bg(resolvedBackground);
        }
        if (resolvedForeground != null) {
            baseStyle = baseStyle.fg(resolvedForeground);
        }
        this.style = baseStyle;
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

        // Get lines to render based on overflow mode
        List<Line> lines = processLines(text.lines(), textArea.width());

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

    private List<Line> processLines(List<Line> lines, int maxWidth) {
        if (maxWidth <= 0) {
            return Collections.emptyList();
        }

        switch (overflow) {
            case CLIP:
                return lines;
            case WRAP_CHARACTER:
            case WRAP_WORD:
                return wrapLines(lines, maxWidth);
            case ELLIPSIS:
                return truncateWithEllipsis(lines, maxWidth, EllipsisPosition.END);
            case ELLIPSIS_START:
                return truncateWithEllipsis(lines, maxWidth, EllipsisPosition.START);
            case ELLIPSIS_MIDDLE:
                return truncateWithEllipsis(lines, maxWidth, EllipsisPosition.MIDDLE);
            default:
                return lines;
        }
    }

    private enum EllipsisPosition { START, MIDDLE, END }

    private List<Line> truncateWithEllipsis(List<Line> lines, int maxWidth, EllipsisPosition position) {
        List<Line> result = new ArrayList<>();

        for (Line line : lines) {
            if (line.width() <= maxWidth) {
                result.add(line);
                continue;
            }

            // Need to truncate - extract full text content and style from first span
            String fullText = lineToString(line);
            Style lineStyle = getLineStyle(line);

            if (maxWidth <= ELLIPSIS.length()) {
                // Not enough room for ellipsis, just clip
                result.add(Line.from(new Span(fullText.substring(0, Math.min(fullText.length(), maxWidth)), lineStyle)));
                continue;
            }

            String truncated;
            switch (position) {
                case END:
                    truncated = truncateEnd(fullText, maxWidth);
                    break;
                case START:
                    truncated = truncateStart(fullText, maxWidth);
                    break;
                case MIDDLE:
                    truncated = truncateMiddle(fullText, maxWidth);
                    break;
                default:
                    truncated = truncateEnd(fullText, maxWidth);
            }

            result.add(Line.from(new Span(truncated, lineStyle)));
        }

        return result;
    }

    private String truncateEnd(String text, int maxWidth) {
        int availableChars = maxWidth - ELLIPSIS.length();
        return text.substring(0, availableChars) + ELLIPSIS;
    }

    private String truncateStart(String text, int maxWidth) {
        int availableChars = maxWidth - ELLIPSIS.length();
        return ELLIPSIS + text.substring(text.length() - availableChars);
    }

    private String truncateMiddle(String text, int maxWidth) {
        int availableChars = maxWidth - ELLIPSIS.length();
        int leftChars = (availableChars + 1) / 2;
        int rightChars = availableChars / 2;
        return text.substring(0, leftChars) + ELLIPSIS + text.substring(text.length() - rightChars);
    }

    private String lineToString(Line line) {
        StringBuilder sb = new StringBuilder();
        for (Span span : line.spans()) {
            sb.append(span.content());
        }
        return sb.toString();
    }

    private Style getLineStyle(Line line) {
        // Use the style of the first span, or empty if no spans
        List<Span> spans = line.spans();
        return spans.isEmpty() ? Style.EMPTY : spans.get(0).style();
    }

    private List<Line> wrapLines(List<Line> lines, int maxWidth) {
        List<Line> wrapped = new ArrayList<>();

        for (Line line : lines) {
            if (line.width() <= maxWidth) {
                wrapped.add(line);
                continue;
            }

            if (overflow == Overflow.WRAP_WORD) {
                wrapped.addAll(wrapLineByWord(line, maxWidth));
            } else {
                wrapped.addAll(wrapLineByCharacter(line, maxWidth));
            }
        }

        return wrapped;
    }

    private List<Line> wrapLineByCharacter(Line line, int maxWidth) {
        List<Line> wrapped = new ArrayList<>();
        List<Span> currentSpans = new ArrayList<>();
        int currentWidth = 0;

        for (Span span : line.spans()) {
            String content = span.content();
            Style spanStyle = span.style();

            int i = 0;
            while (i < content.length()) {
                int remainingWidth = maxWidth - currentWidth;

                if (remainingWidth <= 0) {
                    wrapped.add(Line.from(currentSpans));
                    currentSpans = new ArrayList<>();
                    currentWidth = 0;
                    remainingWidth = maxWidth;
                }

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

        return wrapped;
    }

    private List<Line> wrapLineByWord(Line line, int maxWidth) {
        List<Line> wrapped = new ArrayList<>();
        String fullText = lineToString(line);
        Style lineStyle = getLineStyle(line);

        String[] words = fullText.split("(?<=\\s)|(?=\\s)");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() <= maxWidth) {
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    wrapped.add(Line.from(new Span(stripTrailing(currentLine.toString()), lineStyle)));
                    currentLine = new StringBuilder();
                }
                // Handle words longer than maxWidth
                if (word.length() > maxWidth) {
                    // Break long word by character
                    String remaining = word;
                    while (remaining.length() > maxWidth) {
                        wrapped.add(Line.from(new Span(remaining.substring(0, maxWidth), lineStyle)));
                        remaining = remaining.substring(maxWidth);
                    }
                    currentLine.append(remaining);
                } else {
                    currentLine.append(stripLeading(word));
                }
            }
        }

        if (currentLine.length() > 0) {
            wrapped.add(Line.from(new Span(stripTrailing(currentLine.toString()), lineStyle)));
        }

        return wrapped;
    }

    // Java 8 compatible alternatives to String.stripLeading() and stripTrailing()
    // These implementations use code points to properly handle UTF-16 surrogate pairs
    private static String stripLeading(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        int len = s.length();
        int start = 0;
        while (start < len) {
            int codePoint = Character.codePointAt(s, start);
            if (!Character.isWhitespace(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        return start == 0 ? s : s.substring(start);
    }

    private static String stripTrailing(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        int len = s.length();
        int end = len;
        while (end > 0) {
            int codePoint = Character.codePointBefore(s, end);
            if (!Character.isWhitespace(codePoint)) {
                break;
            }
            end -= Character.charCount(codePoint);
        }
        return end == len ? s : s.substring(0, end);
    }

    public static final class Builder {
        private Text text = Text.empty();
        private Block block;
        private Style style = Style.EMPTY;
        private int scroll = 0;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties bound to this builder's resolver
        private final StyledProperty<Alignment> alignment =
                StyledProperty.of(StandardPropertyKeys.TEXT_ALIGN, Alignment.LEFT, () -> styleResolver);
        private final StyledProperty<Overflow> overflow =
                StyledProperty.of(TEXT_OVERFLOW, Overflow.CLIP, () -> styleResolver);
        private final StyledProperty<Color> background =
                StyledProperty.of(StandardPropertyKeys.BACKGROUND, null, () -> styleResolver);
        private final StyledProperty<Color> foreground =
                StyledProperty.of(StandardPropertyKeys.COLOR, null, () -> styleResolver);

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
            this.alignment.set(alignment);
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

        public Builder overflow(Overflow overflow) {
            this.overflow.set(overflow);
            return this;
        }

        /**
         * Sets the background color.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.background.set(color);
            return this;
        }

        /**
         * Sets the foreground (text) color.
         *
         * @param color the foreground color
         * @return this builder
         */
        public Builder foreground(Color color) {
            this.foreground.set(color);
            return this;
        }

        public Builder scroll(int scroll) {
            this.scroll = Math.max(0, scroll);
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

        public Paragraph build() {
            return new Paragraph(this);
        }
    }
}

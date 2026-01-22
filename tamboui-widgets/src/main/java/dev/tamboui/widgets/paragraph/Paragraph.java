/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.paragraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Hyperlink;
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

        // Apply style to text area (after block renders, matching ratatui behavior)
        buffer.setStyle(textArea, style);

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
            case WRAP_CHARACTER:
            case WRAP_WORD:
                return wrapLines(lines, maxWidth);
            case ELLIPSIS:
                return truncateWithEllipsis(lines, maxWidth, EllipsisPosition.END);
            case ELLIPSIS_START:
                return truncateWithEllipsis(lines, maxWidth, EllipsisPosition.START);
            case ELLIPSIS_MIDDLE:
                return truncateWithEllipsis(lines, maxWidth, EllipsisPosition.MIDDLE);
            case CLIP:
            default:
                // Always clip as safety measure to prevent rendering outside bounds
                return clipLines(lines, maxWidth);
        }
    }

    private List<Line> clipLines(List<Line> lines, int maxWidth) {
        List<Line> result = new ArrayList<>();

        for (Line line : lines) {
            if (line.width() <= maxWidth) {
                result.add(line);
                continue;
            }

            // Need to clip - preserve spans up to maxWidth
            List<Span> clippedSpans = new ArrayList<>();
            int remainingWidth = maxWidth;

            for (Span span : line.spans()) {
                if (remainingWidth <= 0) {
                    break;
                }

                String content = span.content();
                if (content.length() <= remainingWidth) {
                    clippedSpans.add(span);
                    remainingWidth -= content.length();
                } else {
                    // Partial span - truncate content
                    clippedSpans.add(new Span(content.substring(0, remainingWidth), span.style()));
                    break;
                }
            }

            result.add(Line.from(clippedSpans));
        }

        return result;
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
        // Track hyperlinks that need IDs for multi-line wrapping
        Map<String, String> hyperlinkIds = new HashMap<>();

        for (Span span : line.spans()) {
            String content = span.content();
            Style spanStyle = span.style();
            
            // Ensure hyperlinks have IDs when wrapping across lines
            Style wrappedStyle = ensureHyperlinkIdForWrapping(spanStyle, hyperlinkIds);

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

                currentSpans.add(new Span(chunk, wrappedStyle));
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
        // Build a character-to-style mapping to preserve span information
        List<Span> spans = line.spans();
        if (spans.isEmpty()) {
            return Collections.emptyList();
        }

        // Build full text and track which span each character belongs to
        StringBuilder fullText = new StringBuilder();
        List<Style> charStyles = new ArrayList<>();
        Map<String, String> hyperlinkIds = new HashMap<>();

        for (Span span : spans) {
            String content = span.content();
            Style spanStyle = span.style();
            
            // Ensure hyperlinks have IDs when wrapping across lines
            Style wrappedStyle = ensureHyperlinkIdForWrapping(spanStyle, hyperlinkIds);
            
            for (int i = 0; i < content.length(); i++) {
                fullText.append(content.charAt(i));
                charStyles.add(wrappedStyle);
            }
        }

        String text = fullText.toString();
        List<Line> wrapped = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            // Find the next word break point
            int lineEnd = findNextWordBreak(text, pos, maxWidth);
            
            // Extract the line and reconstruct spans with correct styles
            List<Span> lineSpans = new ArrayList<>();
            int spanStart = pos;
            Style currentStyle = charStyles.get(pos);
            
            for (int i = pos; i < lineEnd; i++) {
                Style charStyle = charStyles.get(i);
                if (!charStyle.equals(currentStyle)) {
                    // Style changed, create a span for the previous style
                    if (spanStart < i) {
                        lineSpans.add(new Span(text.substring(spanStart, i), currentStyle));
                    }
                    spanStart = i;
                    currentStyle = charStyle;
                }
            }
            
            // Add the final span
            if (spanStart < lineEnd) {
                lineSpans.add(new Span(text.substring(spanStart, lineEnd), currentStyle));
            }
            
            wrapped.add(Line.from(lineSpans));
            pos = lineEnd;
            
            // Skip leading whitespace at the start of next line (but preserve trailing whitespace on current line)
            // Only skip if we broke at a word boundary (whitespace), not if we broke mid-word
            if (lineEnd < text.length() && Character.isWhitespace(text.charAt(lineEnd - 1))) {
                // We already included the whitespace in the line, so skip it for the next line
                while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                    pos++;
                }
            }
        }

        return wrapped;
    }

    /**
     * Finds the next word break point for word wrapping.
     * Tries to break at word boundaries, but breaks by character if word is too long.
     *
     * @param text the full text
     * @param startPos the starting position
     * @param maxWidth the maximum width for the line
     * @return the position to break at (exclusive)
     */
    private int findNextWordBreak(String text, int startPos, int maxWidth) {
        int textLength = text.length();
        int maxEnd = Math.min(startPos + maxWidth, textLength);
        
        // If we can fit everything, return the end
        if (maxEnd >= textLength) {
            return textLength;
        }
        
        // Look backwards from maxEnd for a word boundary (whitespace)
        for (int i = maxEnd - 1; i > startPos; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1; // Break after whitespace
            }
        }
        
        // No word boundary found - check if we can break at punctuation
        for (int i = maxEnd - 1; i > startPos; i--) {
            char c = text.charAt(i);
            if (c == '-' || c == '/' || c == '\\') {
                return i + 1; // Break after punctuation
            }
        }
        
        // No good break point - break at character boundary (for long words)
        return maxEnd;
    }

    /**
     * Ensures that a hyperlink has an ID when it will wrap across multiple lines.
     * This allows all wrapped chunks to share the same ID and form one continuous link.
     *
     * @param style the style that may contain a hyperlink
     * @param hyperlinkIds a map to track and reuse IDs for the same URL
     * @return a style with an ID-assigned hyperlink if needed
     */
    private Style ensureHyperlinkIdForWrapping(Style style, java.util.Map<String, String> hyperlinkIds) {
        Optional<Hyperlink> hyperlinkOpt = style.hyperlink();
        if (!hyperlinkOpt.isPresent()) {
            return style;
        }

        Hyperlink hyperlink = hyperlinkOpt.get();
        // If hyperlink already has an ID, use it as-is
        if (hyperlink.id().isPresent()) {
            return style;
        }

        // Generate or reuse an ID for this URL
        String url = hyperlink.url();
        String id = hyperlinkIds.get(url);
        if (id == null) {
            // Generate a simple ID based on URL hash
            id = "link-" + Math.abs(url.hashCode());
            hyperlinkIds.put(url, id);
        }

        // Return style with hyperlink that has an ID
        return style.hyperlink(url, id);
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

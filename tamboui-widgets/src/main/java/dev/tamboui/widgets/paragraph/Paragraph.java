/*
 * Copyright TamboUI Contributors
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
import dev.tamboui.style.Overflow;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

/**
 * A paragraph widget for displaying styled text.
 * <p>
 * Supports style-aware properties: {@code text-overflow}, {@code text-align},
 * {@code background}, and {@code color}.
 */
public final class Paragraph implements Widget {

    private static final String ELLIPSIS = "...";

    private final Text text;
    private final Block block;
    private final Style style;
    private final Alignment alignment;
    private final Overflow overflow;
    private final int scroll;
    private final boolean hangingIndent;

    private Paragraph(Builder builder) {
        this.text = builder.text;
        this.block = builder.block;
        this.alignment = builder.resolveAlignment();
        this.overflow = builder.resolveOverflow();
        this.hangingIndent = builder.hangingIndent;
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
                int spanWidth = CharWidth.of(content);
                if (spanWidth <= remainingWidth) {
                    clippedSpans.add(span);
                    remainingWidth -= spanWidth;
                } else {
                    // Partial span - truncate content by display width
                    String clipped = CharWidth.substringByWidth(content, remainingWidth);
                    if (!clipped.isEmpty()) {
                        clippedSpans.add(new Span(clipped, span.style()));
                    }
                    break;
                }
            }

            result.add(Line.from(clippedSpans));
        }

        return result;
    }

    private enum EllipsisPosition {
        /** Ellipsis at the start. */
        START,
        /** Ellipsis in the middle. */
        MIDDLE,
        /** Ellipsis at the end. */
        END
    }

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
                result.add(Line.from(new Span(CharWidth.substringByWidth(fullText, maxWidth), lineStyle)));
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
        int availableWidth = maxWidth - ELLIPSIS.length();
        return CharWidth.substringByWidth(text, availableWidth) + ELLIPSIS;
    }

    private String truncateStart(String text, int maxWidth) {
        int availableWidth = maxWidth - ELLIPSIS.length();
        return ELLIPSIS + CharWidth.substringByWidthFromEnd(text, availableWidth);
    }

    private String truncateMiddle(String text, int maxWidth) {
        int availableWidth = maxWidth - ELLIPSIS.length();
        int leftWidth = (availableWidth + 1) / 2;
        int rightWidth = availableWidth / 2;
        return CharWidth.substringByWidth(text, leftWidth) + ELLIPSIS
                + CharWidth.substringByWidthFromEnd(text, rightWidth);
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

            int indent = hangingIndent ? leadingWhitespaceWidth(line) : 0;
            if (indent <= 0 || indent >= maxWidth) {
                wrapped.addAll(wrapOne(line, maxWidth));
                continue;
            }
            // the first row as usual, the rest of the line under its first non-blank character
            List<Line> first = wrapOne(line, maxWidth);
            Line head = first.get(0);
            wrapped.add(head);
            Line rest = dropLeadingWhitespace(dropCodePoints(line, codePointCount(head)));
            if (rest.width() == 0) {
                continue;
            }
            Span pad = Span.raw(repeatSpace(indent));
            for (Line continuation : wrapOne(rest, maxWidth - indent)) {
                List<Span> spans = new ArrayList<>(continuation.spans().size() + 1);
                spans.add(pad);
                spans.addAll(continuation.spans());
                wrapped.add(Line.from(spans));
            }
        }

        return wrapped;
    }

    private List<Line> wrapOne(Line line, int maxWidth) {
        if (line.width() <= maxWidth) {
            return Collections.singletonList(line);
        }
        return overflow == Overflow.WRAP_WORD ? wrapLineByWord(line, maxWidth) : wrapLineByCharacter(line, maxWidth);
    }

    private static int leadingWhitespaceWidth(Line line) {
        int width = 0;
        for (Span span : line.spans()) {
            String content = span.content();
            for (int i = 0; i < content.length(); i++) {
                if (content.charAt(i) != ' ') {
                    return width;
                }
                width++;
            }
        }
        return 0;
    }

    private static int codePointCount(Line line) {
        int count = 0;
        for (Span span : line.spans()) {
            count += span.content().codePointCount(0, span.content().length());
        }
        return count;
    }

    private static Line dropCodePoints(Line line, int count) {
        List<Span> result = new ArrayList<>();
        int toDrop = count;
        for (Span span : line.spans()) {
            String content = span.content();
            int cps = content.codePointCount(0, content.length());
            if (toDrop >= cps) {
                toDrop -= cps;
                continue;
            }
            String kept = toDrop > 0 ? content.substring(content.offsetByCodePoints(0, toDrop)) : content;
            toDrop = 0;
            result.add(new Span(kept, span.style()));
        }
        return Line.from(result);
    }

    private static Line dropLeadingWhitespace(Line line) {
        List<Span> result = new ArrayList<>();
        boolean leading = true;
        for (Span span : line.spans()) {
            String content = span.content();
            if (leading) {
                int i = 0;
                while (i < content.length() && Character.isWhitespace(content.charAt(i))) {
                    i++;
                }
                if (i == content.length()) {
                    continue;
                }
                content = content.substring(i);
                leading = false;
            }
            result.add(new Span(content, span.style()));
        }
        return Line.from(result);
    }

    private static String repeatSpace(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
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

                // Build chunk by iterating code points and checking display width
                StringBuilder chunk = new StringBuilder();
                int chunkWidth = 0;
                int j = i;
                while (j < content.length()) {
                    int codePoint = content.codePointAt(j);
                    int cpWidth = CharWidth.of(codePoint);
                    if (chunkWidth + cpWidth > remainingWidth) {
                        break;
                    }
                    chunk.appendCodePoint(codePoint);
                    chunkWidth += cpWidth;
                    j += Character.charCount(codePoint);
                }

                if (chunk.length() > 0) {
                    currentSpans.add(new Span(chunk.toString(), wrappedStyle));
                    currentWidth += chunkWidth;
                    i = j;
                } else {
                    // Wide character doesn't fit on remaining space, wrap to next line
                    if (currentWidth > 0) {
                        wrapped.add(Line.from(currentSpans));
                        currentSpans = new ArrayList<>();
                        currentWidth = 0;
                    } else {
                        // Single character wider than maxWidth (shouldn't happen with maxWidth >= 2)
                        int codePoint = content.codePointAt(j);
                        chunk.appendCodePoint(codePoint);
                        currentSpans.add(new Span(chunk.toString(), wrappedStyle));
                        wrapped.add(Line.from(currentSpans));
                        currentSpans = new ArrayList<>();
                        currentWidth = 0;
                        i = j + Character.charCount(codePoint);
                    }
                }
            }
        }

        if (!currentSpans.isEmpty()) {
            wrapped.add(Line.from(currentSpans));
        }

        return wrapped;
    }

    private List<Line> wrapLineByWord(Line line, int maxWidth) {
        // Build a code-point-to-style mapping to preserve span information
        List<Span> spans = line.spans();
        if (spans.isEmpty()) {
            return Collections.emptyList();
        }

        // Build full text and track which code point index maps to which style
        StringBuilder fullText = new StringBuilder();
        List<Style> cpStyles = new ArrayList<>();
        List<Integer> cpWidths = new ArrayList<>();
        Map<String, String> hyperlinkIds = new HashMap<>();

        for (Span span : spans) {
            String content = span.content();
            Style spanStyle = span.style();

            // Ensure hyperlinks have IDs when wrapping across lines
            Style wrappedStyle = ensureHyperlinkIdForWrapping(spanStyle, hyperlinkIds);

            int i = 0;
            while (i < content.length()) {
                int codePoint = content.codePointAt(i);
                fullText.appendCodePoint(codePoint);
                cpStyles.add(wrappedStyle);
                cpWidths.add(CharWidth.of(codePoint));
                i += Character.charCount(codePoint);
            }
        }

        String text = fullText.toString();
        List<Line> wrapped = new ArrayList<>();
        int pos = 0; // code point index

        // Build a char-offset array for code point index -> string offset
        int cpCount = cpStyles.size();
        int[] cpOffsets = new int[cpCount + 1];
        int off = 0;
        for (int idx = 0; idx < cpCount; idx++) {
            cpOffsets[idx] = off;
            off += Character.charCount(text.codePointAt(cpOffsets[idx]));
        }
        cpOffsets[cpCount] = text.length();

        while (pos < cpCount) {
            // Find the next word break point
            int lineEnd = findNextWordBreakByWidth(text, cpOffsets, cpWidths, pos, cpCount, maxWidth);

            // Extract the line and reconstruct spans with correct styles
            List<Span> lineSpans = new ArrayList<>();
            int spanStart = pos;
            Style currentStyle = cpStyles.get(pos);

            for (int i = pos; i < lineEnd; i++) {
                Style cpStyle = cpStyles.get(i);
                if (!cpStyle.equals(currentStyle)) {
                    if (spanStart < i) {
                        lineSpans.add(new Span(text.substring(cpOffsets[spanStart], cpOffsets[i]), currentStyle));
                    }
                    spanStart = i;
                    currentStyle = cpStyle;
                }
            }

            // Add the final span
            if (spanStart < lineEnd) {
                lineSpans.add(new Span(text.substring(cpOffsets[spanStart], cpOffsets[lineEnd]), currentStyle));
            }

            wrapped.add(Line.from(lineSpans));
            pos = lineEnd;

            // Skip leading whitespace at the start of next line
            if (lineEnd < cpCount && lineEnd > 0) {
                int prevCp = text.codePointAt(cpOffsets[lineEnd - 1]);
                if (Character.isWhitespace(prevCp)) {
                    while (pos < cpCount && Character.isWhitespace(text.codePointAt(cpOffsets[pos]))) {
                        pos++;
                    }
                }
            }
        }

        return wrapped;
    }

    /**
     * Finds the next word break point for word wrapping using display widths.
     */
    private int findNextWordBreakByWidth(String text,
                                         int[] cpOffsets,
                                         List<Integer> cpWidths,
                                         int startPos,
                                         int cpCount,
                                         int maxWidth) {
        // Find max end position that fits within maxWidth display columns
        int width = 0;
        int maxEnd = startPos;
        while (maxEnd < cpCount) {
            int cpWidth = cpWidths.get(maxEnd);
            if (width + cpWidth > maxWidth) {
                break;
            }
            width += cpWidth;
            maxEnd++;
        }

        // If we can fit everything, return the end
        if (maxEnd >= cpCount) {
            return cpCount;
        }

        // Look backwards from maxEnd for a word boundary (whitespace)
        for (int i = maxEnd - 1; i > startPos; i--) {
            int cp = text.codePointAt(cpOffsets[i]);
            if (Character.isWhitespace(cp)) {
                return i + 1;
            }
        }

        // No word boundary found - check for punctuation
        for (int i = maxEnd - 1; i > startPos; i--) {
            int cp = text.codePointAt(cpOffsets[i]);
            if (cp == '-' || cp == '/' || cp == '\\') {
                return i + 1;
            }
        }

        // No good break point - break at character boundary. A character wider than the line (a wide
        // character in a 1-column line) still takes a line of its own, as with WRAP_CHARACTER; returning
        // startPos would never advance the wrap loop
        return maxEnd == startPos ? startPos + 1 : maxEnd;
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

    /**
     * Builder for {@link Paragraph}.
     */
    public static final class Builder {
        private Text text = Text.empty();
        private Block block;
        private Style style = Style.EMPTY;
        private int scroll = 0;
        // Hanging indent is always on. The flag is kept wired (and off-by-false still works
        // internally) but is intentionally not exposed via a public setter yet; see the
        // commented-out hangingIndent(boolean) below.
        private boolean hangingIndent = true;
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

        // Kept non-public until a valid use case shows up. Hanging indent is the default and
        // only wrapping behaviour today; the off-at-left-edge path is still implemented behind
        // the flag, but exposing it is an API commitment we only want to make once someone has
        // a concrete need.
        //
        // When it IS exposed, prefer a continuation-indent strategy option (e.g. an enum like
        // ContinuationIndentStrategy.HANGING / FLUSH_LEFT) rather than this boolean, parallel to
        // Overflow: both are facets of one text-layout mechanism and should live in a shared
        // wrapping utility, not be re-implemented per widget. The boolean below is only the
        // internal wiring; uncomment it (and its javadoc) for a quick re-enable in the meantime.
        //
        // /**
        //  * Sets whether a wrapped line continues under its first non-blank character instead of at the left edge,
        //  * so an indented line (a list entry, a key and its value) keeps its indent when it wraps. Only applies with
        //  * a wrapping overflow ({@link Overflow#WRAP_WORD}, {@link Overflow#WRAP_CHARACTER}); an indent that leaves no
        //  * room for text is not kept. On by default; turn it off for text that should continue at the left edge, such
        //  * as ASCII art or a layout drawn with leading spaces.
        //  *
        //  * @param hangingIndent true (the default) to indent the continuation rows of a wrapped line
        //  * @return this builder
        //  */
        // public Builder hangingIndent(boolean hangingIndent) {
        //     this.hangingIndent = hangingIndent;
        //     return this;
        // }

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

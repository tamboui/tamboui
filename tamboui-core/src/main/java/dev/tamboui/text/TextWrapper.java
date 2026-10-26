/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.tamboui.style.ContinuationIndent;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;

/**
 * Shared text layout and wrapping utility.
 * <p>
 * Handles line breaking, word wrapping, character wrapping, truncation (clip, ellipsis),
 * and continuation indent strategies (hanging indent vs flush left).
 * Correctly respects unicode display width, CJK wide characters, and grapheme clusters (ZWJ sequences, emoji flags).
 */
public final class TextWrapper {

    private static final String ELLIPSIS = "...";

    private TextWrapper() {
    }

    /**
     * Returns whether the given {@link Overflow} mode causes lines to wrap.
     *
     * @param overflow the overflow mode, may be null
     * @return true if the mode is {@link Overflow#WRAP_WORD} or {@link Overflow#WRAP_CHARACTER}
     */
    public static boolean isWrapping(Overflow overflow) {
        return overflow == Overflow.WRAP_WORD || overflow == Overflow.WRAP_CHARACTER;
    }

    /**
     * Wraps a single {@link Line} to the given maximum width using default options
     * ({@link Overflow#WRAP_WORD} and {@link ContinuationIndent#HANGING}).
     *
     * @param line     the line to wrap
     * @param maxWidth the maximum width in display columns
     * @return the list of wrapped lines
     */
    public static List<Line> wrap(Line line, int maxWidth) {
        return wrap(line, maxWidth, TextWrapOptions.defaults());
    }

    /**
     * Wraps a single {@link Line} to the given maximum width using the specified overflow mode
     * and default continuation indent ({@link ContinuationIndent#HANGING}).
     *
     * @param line     the line to wrap
     * @param maxWidth the maximum width in display columns
     * @param overflow the overflow mode
     * @return the list of wrapped lines
     */
    public static List<Line> wrap(Line line, int maxWidth, Overflow overflow) {
        return wrap(line, maxWidth, TextWrapOptions.of(overflow));
    }

    /**
     * Wraps a single {@link Line} to the given maximum width using the specified options.
     *
     * @param line     the line to wrap
     * @param maxWidth the maximum width in display columns
     * @param options  the wrap options
     * @return the list of wrapped lines
     */
    public static List<Line> wrap(Line line, int maxWidth, TextWrapOptions options) {
        if (line == null || maxWidth <= 0) {
            return Collections.emptyList();
        }
        TextWrapOptions opt = options != null ? options : TextWrapOptions.defaults();
        return wrapLine(line, maxWidth, opt, new HashMap<>());
    }

    /**
     * Wraps a list of {@link Line}s to the given maximum width using default options.
     *
     * @param lines    the lines to wrap
     * @param maxWidth the maximum width in display columns
     * @return the list of wrapped lines
     */
    public static List<Line> wrap(List<Line> lines, int maxWidth) {
        return wrap(lines, maxWidth, TextWrapOptions.defaults());
    }

    /**
     * Wraps a list of {@link Line}s to the given maximum width using the specified overflow mode.
     *
     * @param lines    the lines to wrap
     * @param maxWidth the maximum width in display columns
     * @param overflow the overflow mode
     * @return the list of wrapped lines
     */
    public static List<Line> wrap(List<Line> lines, int maxWidth, Overflow overflow) {
        return wrap(lines, maxWidth, TextWrapOptions.of(overflow));
    }

    /**
     * Wraps a list of {@link Line}s to the given maximum width using the specified options.
     *
     * @param lines    the lines to wrap
     * @param maxWidth the maximum width in display columns
     * @param options  the wrap options
     * @return the list of wrapped lines
     */
    public static List<Line> wrap(List<Line> lines, int maxWidth, TextWrapOptions options) {
        if (lines == null || lines.isEmpty() || maxWidth <= 0) {
            return Collections.emptyList();
        }
        TextWrapOptions opt = options != null ? options : TextWrapOptions.defaults();
        Map<String, String> hyperlinkIds = new HashMap<>();
        List<Line> result = new ArrayList<>();
        for (Line line : lines) {
            result.addAll(wrapLine(line, maxWidth, opt, hyperlinkIds));
        }
        return result;
    }

    /**
     * Wraps a {@link Text} object to the given maximum width using default options.
     *
     * @param text     the text to wrap
     * @param maxWidth the maximum width in display columns
     * @return the list of wrapped lines
     */
    public static List<Line> wrap(Text text, int maxWidth) {
        return wrap(text, maxWidth, TextWrapOptions.defaults());
    }

    /**
     * Wraps a {@link Text} object to the given maximum width using the specified overflow mode.
     *
     * @param text     the text to wrap
     * @param maxWidth the maximum width in display columns
     * @param overflow the overflow mode
     * @return the list of wrapped lines
     */
    public static List<Line> wrap(Text text, int maxWidth, Overflow overflow) {
        return wrap(text, maxWidth, TextWrapOptions.of(overflow));
    }

    /**
     * Wraps a {@link Text} object to the given maximum width using the specified options.
     *
     * @param text     the text to wrap
     * @param maxWidth the maximum width in display columns
     * @param options  the wrap options
     * @return the list of wrapped lines
     */
    public static List<Line> wrap(Text text, int maxWidth, TextWrapOptions options) {
        if (text == null) {
            return Collections.emptyList();
        }
        return wrap(text.lines(), maxWidth, options);
    }

    /**
     * Wraps a list of {@link Span}s to the given maximum width using default options.
     *
     * @param spans    the spans to wrap
     * @param maxWidth the maximum width in display columns
     * @return the list of wrapped lines
     */
    public static List<Line> wrapSpans(List<Span> spans, int maxWidth) {
        return wrapSpans(spans, maxWidth, TextWrapOptions.defaults());
    }

    /**
     * Wraps a list of {@link Span}s to the given maximum width using the specified overflow mode.
     *
     * @param spans    the spans to wrap
     * @param maxWidth the maximum width in display columns
     * @param overflow the overflow mode
     * @return the list of wrapped lines
     */
    public static List<Line> wrapSpans(List<Span> spans, int maxWidth, Overflow overflow) {
        return wrapSpans(spans, maxWidth, TextWrapOptions.of(overflow));
    }

    /**
     * Wraps a list of {@link Span}s to the given maximum width using the specified options.
     *
     * @param spans    the spans to wrap
     * @param maxWidth the maximum width in display columns
     * @param options  the wrap options
     * @return the list of wrapped lines
     */
    public static List<Line> wrapSpans(List<Span> spans, int maxWidth, TextWrapOptions options) {
        if (spans == null || maxWidth <= 0) {
            return Collections.emptyList();
        }
        return wrap(Line.from(spans), maxWidth, options);
    }

    /**
     * Computes the character offset segments for wrapping the given text to {@code maxWidth} columns.
     *
     * @param text     the text to wrap
     * @param maxWidth the maximum width in display columns
     * @param overflow the overflow mode
     * @return list of segments, where each segment is {@code [startOffset, endOffset, indent]}
     */
    public static List<int[]> wrapOffsets(CharSequence text, int maxWidth, Overflow overflow) {
        return wrapOffsets(text, maxWidth, TextWrapOptions.of(overflow));
    }

    /**
     * Computes the character offset segments for wrapping the given text to {@code maxWidth} columns
     * using the specified options.
     *
     * @param text     the text to wrap
     * @param maxWidth the maximum width in display columns
     * @param options  the wrap options
     * @return list of segments, where each segment is {@code [startOffset, endOffset, indent]}
     */
    public static List<int[]> wrapOffsets(CharSequence text, int maxWidth, TextWrapOptions options) {
        if (maxWidth <= 0 || text == null) {
            return Collections.emptyList();
        }
        int len = text.length();
        if (len == 0) {
            return Collections.singletonList(new int[] {0, 0, 0});
        }
        TextWrapOptions opt = options != null ? options : TextWrapOptions.defaults();
        Overflow overflow = opt.overflow();
        if (!isWrapping(overflow)) {
            return Collections.singletonList(new int[] {0, len, 0});
        }

        int indent = (opt.continuationIndent() == ContinuationIndent.HANGING)
                ? leadingWhitespaceWidth(text)
                : 0;

        if (indent <= 0 || indent >= maxWidth) {
            return wrapOffsetsUnindented(text, maxWidth, overflow, 0);
        }

        // Hanging indent: row 0 at maxWidth (indent = 0)
        List<int[]> first = wrapOffsetsUnindented(text, maxWidth, overflow, 0);
        if (first.isEmpty()) {
            return Collections.emptyList();
        }
        int[] head = first.get(0);
        List<int[]> result = new ArrayList<>();
        result.add(new int[] {head[0], head[1], 0});

        // Skip leading whitespace of the remainder (as well as whitespace consumed by break)
        int restStart = head[1];
        while (restStart < len && Character.isWhitespace(text.charAt(restStart))) {
            restStart++;
        }
        if (restStart >= len) {
            return result;
        }

        // Wrap remainder at maxWidth - indent
        CharSequence rest = text.subSequence(restStart, len);
        List<int[]> continuation = wrapOffsetsUnindented(rest, maxWidth - indent, overflow, indent);
        for (int[] seg : continuation) {
            result.add(new int[] {restStart + seg[0], restStart + seg[1], indent});
        }
        return result;
    }

    private static List<Line> wrapLine(Line line, int maxWidth, TextWrapOptions options,
                                       Map<String, String> hyperlinkIds) {
        Overflow overflow = options.overflow();
        switch (overflow) {
            case CLIP:
                return Collections.singletonList(clipLine(line, maxWidth));
            case ELLIPSIS:
                return Collections.singletonList(truncateWithEllipsis(line, maxWidth, EllipsisPosition.END));
            case ELLIPSIS_START:
                return Collections.singletonList(truncateWithEllipsis(line, maxWidth, EllipsisPosition.START));
            case ELLIPSIS_MIDDLE:
                return Collections.singletonList(truncateWithEllipsis(line, maxWidth, EllipsisPosition.MIDDLE));
            case WRAP_CHARACTER:
            case WRAP_WORD:
            default:
                break;
        }

        if (line.width() <= maxWidth) {
            return Collections.singletonList(line);
        }

        String fullText = lineToString(line);
        List<int[]> segments = wrapOffsets(fullText, maxWidth, options);
        List<Line> result = new ArrayList<>(segments.size());

        for (int[] seg : segments) {
            int start = seg[0];
            int end = seg[1];
            int indent = seg.length > 2 ? seg[2] : 0;

            List<Span> lineSpans = new ArrayList<>();
            if (indent > 0) {
                lineSpans.add(Span.raw(repeatSpace(indent)));
            }
            lineSpans.addAll(sliceSpans(line.spans(), start, end, hyperlinkIds));
            result.add(Line.from(lineSpans));
        }

        return result;
    }

    private static List<int[]> wrapOffsetsUnindented(CharSequence text, int maxWidth, Overflow overflow, int indent) {
        if (overflow == Overflow.WRAP_CHARACTER) {
            return wrapOffsetsCharacter(text, maxWidth, indent);
        }
        return wrapOffsetsWord(text, maxWidth, indent);
    }

    private static List<int[]> wrapOffsetsCharacter(CharSequence text, int maxWidth, int indent) {
        List<int[]> segments = new ArrayList<>();
        int start = 0;
        int width = 0;
        int i = 0;
        int len = text.length();

        while (i < len) {
            int clusterEnd = GraphemeClusters.clusterEnd(text, i);
            int clusterWidth = CharWidth.of(text.subSequence(i, clusterEnd).toString());

            if (width + clusterWidth > maxWidth) {
                if (width == 0) {
                    // Single cluster wider than maxWidth: give it its own row.
                    segments.add(new int[] {start, clusterEnd, indent});
                    i = clusterEnd;
                    start = i;
                    width = 0;
                    continue;
                }
                segments.add(new int[] {start, i, indent});
                start = i;
                width = 0;
                continue;
            }

            width += clusterWidth;
            i = clusterEnd;
        }
        if (start < len) {
            segments.add(new int[] {start, len, indent});
        }
        return segments;
    }

    private static List<int[]> wrapOffsetsWord(CharSequence text, int maxWidth, int indent) {
        List<Integer> clusterOffsets = new ArrayList<>();
        List<Integer> clusterWidths = new ArrayList<>();
        int i = 0;
        int len = text.length();

        while (i < len) {
            int clusterEnd = GraphemeClusters.clusterEnd(text, i);
            clusterOffsets.add(i);
            clusterWidths.add(CharWidth.of(text.subSequence(i, clusterEnd).toString()));
            i = clusterEnd;
        }
        clusterOffsets.add(len);
        int clusterCount = clusterWidths.size();

        List<int[]> segments = new ArrayList<>();
        int pos = 0;

        while (pos < clusterCount) {
            int lineEnd = findNextWordBreakByWidth(text, clusterOffsets, clusterWidths, pos, clusterCount, maxWidth);
            segments.add(new int[] {clusterOffsets.get(pos), clusterOffsets.get(lineEnd), indent});

            // Skip whitespace consumed by the break so the next row never starts with it.
            int nextPos = lineEnd;
            while (nextPos < clusterCount
                    && Character.isWhitespace(Character.codePointAt(text, clusterOffsets.get(nextPos)))) {
                nextPos++;
            }
            pos = nextPos;
        }
        return segments;
    }

    private static int findNextWordBreakByWidth(CharSequence text,
                                                List<Integer> clusterOffsets,
                                                List<Integer> clusterWidths,
                                                int startPos,
                                                int clusterCount,
                                                int maxWidth) {
        int width = 0;
        int maxEnd = startPos;
        while (maxEnd < clusterCount) {
            int clusterWidth = clusterWidths.get(maxEnd);
            if (width + clusterWidth > maxWidth) {
                break;
            }
            width += clusterWidth;
            maxEnd++;
        }

        if (maxEnd == startPos) {
            // Single cluster wider than maxWidth: force progress.
            maxEnd = Math.min(startPos + 1, clusterCount);
        }

        if (maxEnd >= clusterCount) {
            return clusterCount;
        }

        // The fit already ends exactly at a word boundary; no need to backtrack.
        if (Character.isWhitespace(Character.codePointAt(text, clusterOffsets.get(maxEnd)))) {
            return maxEnd;
        }

        // Look backwards from maxEnd for a word boundary (whitespace)
        for (int idx = maxEnd - 1; idx > startPos; idx--) {
            int codePoint = Character.codePointAt(text, clusterOffsets.get(idx));
            if (Character.isWhitespace(codePoint)) {
                return idx;
            }
        }

        // No word boundary found - check for punctuation
        for (int idx = maxEnd - 1; idx > startPos; idx--) {
            int codePoint = Character.codePointAt(text, clusterOffsets.get(idx));
            if (codePoint == '-' || codePoint == '/' || codePoint == '\\') {
                return idx + 1;
            }
        }

        return maxEnd;
    }

    private static List<Span> sliceSpans(List<Span> spans, int start, int end,
                                         Map<String, String> hyperlinkIds) {
        List<Span> result = new ArrayList<>();
        int currentOffset = 0;

        for (Span span : spans) {
            String content = span.content();
            int spanLen = content.length();
            int spanStart = currentOffset;
            int spanEnd = currentOffset + spanLen;

            if (spanEnd <= start) {
                currentOffset = spanEnd;
                continue;
            }
            if (spanStart >= end) {
                break;
            }

            int sliceStart = Math.max(0, start - spanStart);
            int sliceEnd = Math.min(spanLen, end - spanStart);

            if (sliceStart < sliceEnd) {
                String piece = content.substring(sliceStart, sliceEnd);
                Style wrappedStyle = ensureHyperlinkIdForWrapping(span.style(), hyperlinkIds);
                result.add(new Span(piece, wrappedStyle));
            }

            currentOffset = spanEnd;
        }

        return result;
    }

    private static Line clipLine(Line line, int maxWidth) {
        if (line.width() <= maxWidth) {
            return line;
        }
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
                String clipped = CharWidth.substringByWidth(content, remainingWidth);
                if (!clipped.isEmpty()) {
                    clippedSpans.add(new Span(clipped, span.style()));
                }
                break;
            }
        }

        return Line.from(clippedSpans);
    }

    private enum EllipsisPosition {
        START,
        MIDDLE,
        END
    }

    private static Line truncateWithEllipsis(Line line, int maxWidth, EllipsisPosition position) {
        if (line.width() <= maxWidth) {
            return line;
        }

        String fullText = lineToString(line);
        Style lineStyle = getLineStyle(line);

        int ellipsisWidth = CharWidth.of(ELLIPSIS);
        if (maxWidth <= ellipsisWidth) {
            return Line.from(new Span(CharWidth.substringByWidth(fullText, maxWidth), lineStyle));
        }

        int availableWidth = maxWidth - ellipsisWidth;
        String truncated;
        switch (position) {
            case START:
                truncated = ELLIPSIS + CharWidth.substringByWidthFromEnd(fullText, availableWidth);
                break;
            case MIDDLE: {
                int leftWidth = (availableWidth + 1) / 2;
                int rightWidth = availableWidth / 2;
                truncated = CharWidth.substringByWidth(fullText, leftWidth)
                        + ELLIPSIS
                        + CharWidth.substringByWidthFromEnd(fullText, rightWidth);
                break;
            }
            case END:
            default:
                truncated = CharWidth.substringByWidth(fullText, availableWidth) + ELLIPSIS;
                break;
        }

        return Line.from(new Span(truncated, lineStyle));
    }

    private static String lineToString(Line line) {
        StringBuilder sb = new StringBuilder();
        for (Span span : line.spans()) {
            sb.append(span.content());
        }
        return sb.toString();
    }

    private static Style getLineStyle(Line line) {
        List<Span> spans = line.spans();
        return spans.isEmpty() ? Style.EMPTY : spans.get(0).style();
    }

    private static int leadingWhitespaceWidth(CharSequence text) {
        int width = 0;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c != ' ') {
                return width;
            }
            width++;
        }
        return 0;
    }

    private static String repeatSpace(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static Style ensureHyperlinkIdForWrapping(Style style, Map<String, String> hyperlinkIds) {
        Optional<Hyperlink> hyperlinkOpt = style.hyperlink();
        if (!hyperlinkOpt.isPresent()) {
            return style;
        }

        Hyperlink hyperlink = hyperlinkOpt.get();
        if (hyperlink.id().isPresent()) {
            return style;
        }

        String url = hyperlink.url();
        String id = hyperlinkIds.get(url);
        if (id == null) {
            id = UUID.randomUUID().toString();
            hyperlinkIds.put(url, id);
        }

        return style.hyperlink(url, id);
    }
}

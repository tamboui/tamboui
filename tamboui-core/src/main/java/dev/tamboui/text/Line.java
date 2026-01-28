/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import dev.tamboui.layout.Alignment;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A single line of text composed of styled spans.
 */
public final class Line {

    private final List<Span> spans;
    private final Alignment alignment;

    /**
     * Creates a new line with the given spans and alignment.
     *
     * @param spans the spans composing this line
     * @param alignment the alignment of this line, or null for default
     */
    public Line(List<Span> spans, Alignment alignment) {
        this.spans = listCopyOf(spans);
        this.alignment = alignment;
    }

    /**
     * Creates an empty line with no spans.
     *
     * @return an empty line
     */
    public static Line empty() {
        return new Line(listCopyOf(), null);
    }

    /**
     * Creates a line from a plain text string.
     *
     * @param text the text content
     * @return a new line containing the text
     */
    public static Line from(String text) {
        return new Line(listCopyOf(Span.raw(text)), null);
    }

    /**
     * Creates a line from a single span.
     *
     * @param span the span
     * @return a new line containing the span
     */
    public static Line from(Span span) {
        return new Line(listCopyOf(span), null);
    }

    /**
     * Creates a line from multiple spans.
     *
     * @param spans the spans
     * @return a new line containing the spans
     */
    public static Line from(Span... spans) {
        return new Line(Arrays.asList(spans), null);
    }

    /**
     * Creates a line from a list of spans.
     *
     * @param spans the spans
     * @return a new line containing the spans
     */
    public static Line from(List<Span> spans) {
        return new Line(spans, null);
    }

    /**
     * Creates a line from styled text.
     *
     * @param text the text content
     * @param style the style to apply
     * @return a new styled line
     */
    public static Line styled(String text, Style style) {
        return new Line(listCopyOf(Span.styled(text, style)), null);
    }

    /**
     * Returns the display width of this line (sum of span widths).
     *
     * @return the display width in columns
     */
    public int width() {
        return spans.stream().mapToInt(Span::width).sum();
    }

    /**
     * Returns whether this line is empty (no spans or all spans empty).
     *
     * @return {@code true} if this line is empty
     */
    public boolean isEmpty() {
        return spans.isEmpty() || spans.stream().allMatch(Span::isEmpty);
    }

    /**
     * Returns a new line with the given alignment.
     *
     * @param alignment the alignment to apply
     * @return a new line with the specified alignment
     */
    public Line alignment(Alignment alignment) {
        return new Line(spans, alignment);
    }

    /**
     * Returns a new line with left alignment.
     *
     * @return a new left-aligned line
     */
    public Line left() {
        return alignment(Alignment.LEFT);
    }

    /**
     * Returns a new line with center alignment.
     *
     * @return a new center-aligned line
     */
    public Line centered() {
        return alignment(Alignment.CENTER);
    }

    /**
     * Returns a new line with right alignment.
     *
     * @return a new right-aligned line
     */
    public Line right() {
        return alignment(Alignment.RIGHT);
    }

    /**
     * Applies a style patch to all spans.
     *
     * @param style the style to patch onto each span
     * @return a new line with the patched style applied
     */
    public Line patchStyle(Style style) {
        List<Span> newSpans = spans.stream()
            .map(span -> span.patchStyle(style))
            .collect(Collectors.toList());
        return new Line(newSpans, alignment);
    }

    /**
     * Returns a new line with the given foreground color applied to all spans.
     *
     * @param color the foreground color
     * @return a new line with the foreground color applied
     */
    public Line fg(Color color) {
        return patchStyle(Style.EMPTY.fg(color));
    }

    /**
     * Returns a new line with the given background color applied to all spans.
     *
     * @param color the background color
     * @return a new line with the background color applied
     */
    public Line bg(Color color) {
        return patchStyle(Style.EMPTY.bg(color));
    }

    /**
     * Returns a new line with bold applied to all spans.
     *
     * @return a new bold line
     */
    public Line bold() {
        return patchStyle(Style.EMPTY.bold());
    }

    /**
     * Returns a new line with italic applied to all spans.
     *
     * @return a new italic line
     */
    public Line italic() {
        return patchStyle(Style.EMPTY.italic());
    }

    /**
     * Returns a new line with underline applied to all spans.
     *
     * @return a new underlined line
     */
    public Line underlined() {
        return patchStyle(Style.EMPTY.underlined());
    }

    /**
     * Returns a new line with a hyperlink applied to all spans.
     *
     * @param url the hyperlink URL
     * @return a new line with the hyperlink applied
     */
    public Line hyperlink(String url) {
        return patchStyle(Style.EMPTY.hyperlink(url));
    }

    /**
     * Returns a new line with a hyperlink and ID applied to all spans.
     *
     * @param url the hyperlink URL
     * @param id the hyperlink ID for grouping
     * @return a new line with the hyperlink applied
     */
    public Line hyperlink(String url, String id) {
        return patchStyle(Style.EMPTY.hyperlink(url, id));
    }

    /**
     * Returns the raw text content without styling.
     *
     * @return the plain text content
     */
    public String rawContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            sb.append(span.content());
        }
        return sb.toString();
    }

    /**
     * Appends another line's spans to this line.
     *
     * @param other the line whose spans to append
     * @return a new line with the combined spans
     */
    public Line append(Line other) {
        List<Span> newSpans = new ArrayList<>(spans);
        newSpans.addAll(other.spans);
        return new Line(newSpans, alignment);
    }

    /**
     * Appends a span to this line.
     *
     * @param span the span to append
     * @return a new line with the span appended
     */
    public Line append(Span span) {
        List<Span> newSpans = new ArrayList<>(spans);
        newSpans.add(span);
        return new Line(newSpans, alignment);
    }

    /**
     * Returns the spans composing this line.
     *
     * @return the list of spans
     */
    public List<Span> spans() {
        return spans;
    }

    /**
     * Returns the alignment of this line, if set.
     *
     * @return the alignment, or empty if not set
     */
    public Optional<Alignment> alignment() {
        return Optional.ofNullable(alignment);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Line)) {
            return false;
        }
        Line line = (Line) o;
        return spans.equals(line.spans) && Objects.equals(alignment, line.alignment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spans, alignment);
    }

    @Override
    public String toString() {
        return String.format("Line[spans=%s, alignment=%s]", spans, alignment);
    }
}

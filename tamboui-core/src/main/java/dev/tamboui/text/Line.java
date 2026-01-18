/*
 * Copyright (c) 2025 TamboUI Contributors
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

/// A single line of text composed of styled spans.
public final class Line {

    private final List<Span> spans;
    private final Alignment alignment;

    public Line(List<Span> spans, Alignment alignment) {
        this.spans = listCopyOf(spans);
        this.alignment = alignment;
    }

    public static Line empty() {
        return new Line(listCopyOf(), null);
    }

    public static Line from(String text) {
        return new Line(listCopyOf(Span.raw(text)), null);
    }

    public static Line from(Span span) {
        return new Line(listCopyOf(span), null);
    }

    public static Line from(Span... spans) {
        return new Line(Arrays.asList(spans), null);
    }

    public static Line from(List<Span> spans) {
        return new Line(spans, null);
    }

    public static Line styled(String text, Style style) {
        return new Line(listCopyOf(Span.styled(text, style)), null);
    }

    /// Returns the display width of this line (sum of span widths).
    public int width() {
        return spans.stream().mapToInt(Span::width).sum();
    }

    public boolean isEmpty() {
        return spans.isEmpty() || spans.stream().allMatch(Span::isEmpty);
    }

    public Line alignment(Alignment alignment) {
        return new Line(spans, alignment);
    }

    public Line left() {
        return alignment(Alignment.LEFT);
    }

    public Line centered() {
        return alignment(Alignment.CENTER);
    }

    public Line right() {
        return alignment(Alignment.RIGHT);
    }

    /// Applies a style patch to all spans.
    public Line patchStyle(Style style) {
        List<Span> newSpans = spans.stream()
            .map(span -> span.patchStyle(style))
            .collect(Collectors.toList());
        return new Line(newSpans, alignment);
    }

    public Line fg(Color color) {
        return patchStyle(Style.EMPTY.fg(color));
    }

    public Line bg(Color color) {
        return patchStyle(Style.EMPTY.bg(color));
    }

    public Line bold() {
        return patchStyle(Style.EMPTY.bold());
    }

    public Line italic() {
        return patchStyle(Style.EMPTY.italic());
    }

    public Line underlined() {
        return patchStyle(Style.EMPTY.underlined());
    }

    public Line hyperlink(String url) {
        return patchStyle(Style.EMPTY.hyperlink(url));
    }

    public Line hyperlink(String url, String id) {
        return patchStyle(Style.EMPTY.hyperlink(url, id));
    }

    /// Returns the raw text content without styling.
    public String rawContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spans.size(); i++) {
            Span span = spans.get(i);
            sb.append(span.content());
        }
        return sb.toString();
    }

    /// Appends another line's spans to this line.
    public Line append(Line other) {
        List<Span> newSpans = new ArrayList<>(spans);
        newSpans.addAll(other.spans);
        return new Line(newSpans, alignment);
    }

    /// Appends a span to this line.
    public Line append(Span span) {
        List<Span> newSpans = new ArrayList<>(spans);
        newSpans.add(span);
        return new Line(newSpans, alignment);
    }

    public List<Span> spans() {
        return spans;
    }

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


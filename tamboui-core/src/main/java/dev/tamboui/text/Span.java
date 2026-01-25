/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * A string with a single style applied. The smallest styled text unit.
 */
public final class Span {

    private final String content;
    private final Style style;
    private final int cachedHashCode;

    public Span(String content, Style style) {
        this.content = content;
        this.style = style;
        this.cachedHashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = content.hashCode();
        result = 31 * result + style.hashCode();
        return result;
    }

    public static Span raw(String content) {
        return new Span(content, Style.EMPTY);
    }

    public static Span styled(String content, Style style) {
        return new Span(content, style);
    }

    /**
     * Creates a span from a masked string.
     * The masked value will be used as the content.
     */
    public static Span from(Masked masked) {
        return new Span(masked.value(), Style.EMPTY);
    }

    /**
     * Creates a styled span from a masked string.
     * The masked value will be used as the content.
     */
    public static Span styled(Masked masked, Style style) {
        return new Span(masked.value(), style);
    }

    /**
     * Returns the display width of this span in terminal columns.
     * Wide characters (CJK, emoji) count as 2, combining marks as 0.
     */
    public int width() {
        return CharWidth.of(content);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    // Style builder methods

    public Span style(Style newStyle) {
        return new Span(content, newStyle);
    }

    public Span patchStyle(Style patch) {
        return new Span(content, style.patch(patch));
    }

    public Span fg(Color color) {
        return new Span(content, style.fg(color));
    }

    public Span bg(Color color) {
        return new Span(content, style.bg(color));
    }

    public Span bold() {
        return new Span(content, style.bold());
    }

    public Span italic() {
        return new Span(content, style.italic());
    }

    public Span underlined() {
        return new Span(content, style.underlined());
    }

    public Span dim() {
        return new Span(content, style.dim());
    }

    public Span reversed() {
        return new Span(content, style.reversed());
    }

    public Span crossedOut() {
        return new Span(content, style.crossedOut());
    }

    public Span hyperlink(String url) {
        return new Span(content, style.hyperlink(url));
    }

    public Span hyperlink(String url, String id) {
        return new Span(content, style.hyperlink(url, id));
    }

    // Color convenience methods

    public Span black() {
        return fg(Color.BLACK);
    }

    public Span red() {
        return fg(Color.RED);
    }

    public Span green() {
        return fg(Color.GREEN);
    }

    public Span yellow() {
        return fg(Color.YELLOW);
    }

    public Span blue() {
        return fg(Color.BLUE);
    }

    public Span magenta() {
        return fg(Color.MAGENTA);
    }

    public Span cyan() {
        return fg(Color.CYAN);
    }

    public Span white() {
        return fg(Color.WHITE);
    }

    public Span gray() {
        return fg(Color.GRAY);
    }

    public String content() {
        return content;
    }

    public Style style() {
        return style;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Span)) {
            return false;
        }
        Span span = (Span) o;
        if (cachedHashCode != span.cachedHashCode) {
            return false;
        }
        return content.equals(span.content) && style.equals(span.style);
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return String.format("Span[content=%s, style=%s]", content, style);
    }
}

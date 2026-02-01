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

    /**
     * Creates a new span with the given content and style.
     *
     * @param content the text content
     * @param style   the style to apply
     */
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

    /**
     * Creates an unstyled span from the given content.
     *
     * @param content the text content
     * @return a new unstyled span
     */
    public static Span raw(String content) {
        return new Span(content, Style.EMPTY);
    }

    /**
     * Creates a styled span from the given content.
     *
     * @param content the text content
     * @param style   the style to apply
     * @return a new styled span
     */
    public static Span styled(String content, Style style) {
        return new Span(content, style);
    }

    /**
     * Creates a span from a masked string.
     * The masked value will be used as the content.
     *
     * @param masked the masked string
     * @return a new unstyled span
     */
    public static Span from(Masked masked) {
        return new Span(masked.value(), Style.EMPTY);
    }

    /**
     * Creates a styled span from a masked string.
     * The masked value will be used as the content.
     *
     * @param masked the masked string
     * @param style  the style to apply
     * @return a new styled span
     */
    public static Span styled(Masked masked, Style style) {
        return new Span(masked.value(), style);
    }

    /**
     * Returns the display width of this span in terminal columns.
     * Wide characters (CJK, emoji) count as 2, combining marks as 0.
     *
     * @return the display width in terminal columns
     */
    public int width() {
        return CharWidth.of(content);
    }

    /**
     * Returns true if this span has no content.
     *
     * @return true if the content is empty
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    // Style builder methods

    /**
     * Returns a new span with the given style replacing the current one.
     *
     * @param newStyle the new style
     * @return a new span with the given style
     */
    public Span style(Style newStyle) {
        return new Span(content, newStyle);
    }

    /**
     * Returns a new span with the given style patch applied.
     *
     * @param patch the style patch to apply
     * @return a new span with the patched style
     */
    public Span patchStyle(Style patch) {
        return new Span(content, style.patch(patch));
    }

    /**
     * Returns a new span with the given foreground color.
     *
     * @param color the foreground color
     * @return a new span with the foreground color applied
     */
    public Span fg(Color color) {
        return new Span(content, style.fg(color));
    }

    /**
     * Returns a new span with the given background color.
     *
     * @param color the background color
     * @return a new span with the background color applied
     */
    public Span bg(Color color) {
        return new Span(content, style.bg(color));
    }

    /**
     * Returns a new span with bold style.
     *
     * @return a new bold span
     */
    public Span bold() {
        return new Span(content, style.bold());
    }

    /**
     * Returns a new span with italic style.
     *
     * @return a new italic span
     */
    public Span italic() {
        return new Span(content, style.italic());
    }

    /**
     * Returns a new span with underlined style.
     *
     * @return a new underlined span
     */
    public Span underlined() {
        return new Span(content, style.underlined());
    }

    /**
     * Returns a new span with dim style.
     *
     * @return a new dim span
     */
    public Span dim() {
        return new Span(content, style.dim());
    }

    /**
     * Returns a new span with reversed style.
     *
     * @return a new reversed span
     */
    public Span reversed() {
        return new Span(content, style.reversed());
    }

    /**
     * Returns a new span with crossed-out style.
     *
     * @return a new crossed-out span
     */
    public Span crossedOut() {
        return new Span(content, style.crossedOut());
    }

    /**
     * Returns a new span with a hyperlink.
     *
     * @param url the hyperlink URL
     * @return a new span with the hyperlink applied
     */
    public Span hyperlink(String url) {
        return new Span(content, style.hyperlink(url));
    }

    /**
     * Returns a new span with a hyperlink and explicit ID.
     *
     * @param url the hyperlink URL
     * @param id  the hyperlink ID
     * @return a new span with the hyperlink applied
     */
    public Span hyperlink(String url, String id) {
        return new Span(content, style.hyperlink(url, id));
    }

    // Color convenience methods

    /**
     * Returns a new span with black foreground color.
     *
     * @return a new span with black foreground
     */
    public Span black() {
        return fg(Color.BLACK);
    }

    /**
     * Returns a new span with red foreground color.
     *
     * @return a new span with red foreground
     */
    public Span red() {
        return fg(Color.RED);
    }

    /**
     * Returns a new span with green foreground color.
     *
     * @return a new span with green foreground
     */
    public Span green() {
        return fg(Color.GREEN);
    }

    /**
     * Returns a new span with yellow foreground color.
     *
     * @return a new span with yellow foreground
     */
    public Span yellow() {
        return fg(Color.YELLOW);
    }

    /**
     * Returns a new span with blue foreground color.
     *
     * @return a new span with blue foreground
     */
    public Span blue() {
        return fg(Color.BLUE);
    }

    /**
     * Returns a new span with magenta foreground color.
     *
     * @return a new span with magenta foreground
     */
    public Span magenta() {
        return fg(Color.MAGENTA);
    }

    /**
     * Returns a new span with cyan foreground color.
     *
     * @return a new span with cyan foreground
     */
    public Span cyan() {
        return fg(Color.CYAN);
    }

    /**
     * Returns a new span with white foreground color.
     *
     * @return a new span with white foreground
     */
    public Span white() {
        return fg(Color.WHITE);
    }

    /**
     * Returns a new span with gray foreground color.
     *
     * @return a new span with gray foreground
     */
    public Span gray() {
        return fg(Color.GRAY);
    }

    /**
     * Returns the text content of this span.
     *
     * @return the text content
     */
    public String content() {
        return content;
    }

    /**
     * Returns the style of this span.
     *
     * @return the style
     */
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

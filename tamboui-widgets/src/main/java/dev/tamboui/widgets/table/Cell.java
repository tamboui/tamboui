/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.table;

import java.util.Optional;

import dev.tamboui.layout.Alignment;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

/**
 * A cell in a {@link Table}.
 * <p>
 * A cell contains styled text content that can be a single line or multi-line text.
 * Cells can have their own style which takes precedence over row and table styles.
 * Cells can also have their own text alignment.
 *
 * <pre>{@code
 * // Simple cell
 * Cell.from("Hello")
 *
 * // Styled cell
 * Cell.from("Important").style(Style.EMPTY.fg(Color.RED).bold())
 *
 * // Right-aligned cell
 * Cell.from("123").rightAligned()
 *
 * // Multi-line cell
 * Cell.from(Text.from("Line 1\nLine 2"))
 * }</pre>
 */
public final class Cell {

    private final Text content;
    private final Style style;
    private final Alignment alignment;

    private Cell(Text content, Style style) {
        this(content, style, null);
    }

    private Cell(Text content, Style style, Alignment alignment) {
        this.content = content;
        this.style = style;
        this.alignment = alignment;
    }

    /**
     * Creates a cell from a string.
     *
     * @param content the cell content
     * @return a new cell
     */
    public static Cell from(String content) {
        return new Cell(Text.from(content), Style.EMPTY);
    }

    /**
     * Creates a cell from a span.
     *
     * @param span the span to use as content
     * @return a new cell
     */
    public static Cell from(Span span) {
        return new Cell(Text.from(span), Style.EMPTY);
    }

    /**
     * Creates a cell from a line.
     *
     * @param line the line to use as content
     * @return a new cell
     */
    public static Cell from(Line line) {
        return new Cell(Text.from(line), Style.EMPTY);
    }

    /**
     * Creates a cell from text.
     *
     * @param text the text to use as content
     * @return a new cell
     */
    public static Cell from(Text text) {
        return new Cell(text, Style.EMPTY);
    }

    /**
     * Creates an empty cell.
     *
     * @return a new empty cell
     */
    public static Cell empty() {
        return new Cell(Text.empty(), Style.EMPTY);
    }

    /**
     * Returns the content of this cell.
     *
     * @return the cell content
     */
    public Text content() {
        return content;
    }

    /**
     * Returns the style of this cell.
     *
     * @return the cell style
     */
    public Style style() {
        return style;
    }

    /**
     * Returns a new cell with the given style.
     *
     * @param style the style to apply
     * @return a new cell with the style
     */
    public Cell style(Style style) {
        return new Cell(this.content, style, this.alignment);
    }

    /**
     * Returns a new cell with the given alignment.
     *
     * @param alignment the alignment to apply
     * @return a new cell with the alignment
     */
    public Cell alignment(Alignment alignment) {
        return new Cell(this.content, this.style, alignment);
    }

    /**
     * Returns a new cell with left alignment.
     *
     * @return a new cell with left alignment
     */
    public Cell leftAligned() {
        return alignment(Alignment.LEFT);
    }

    /**
     * Returns a new cell with center alignment.
     *
     * @return a new cell with center alignment
     */
    public Cell centered() {
        return alignment(Alignment.CENTER);
    }

    /**
     * Returns a new cell with right alignment.
     *
     * @return a new cell with right alignment
     */
    public Cell rightAligned() {
        return alignment(Alignment.RIGHT);
    }

    /**
     * Returns the alignment of this cell, if set.
     * <p>
     * If this cell does not have an explicit alignment set, falls back to the content's alignment.
     *
     * @return the alignment, or empty if not set
     */
    public Optional<Alignment> alignment() {
        if (alignment != null) {
            return Optional.of(alignment);
        }
        return content.alignment();
    }

    /**
     * Returns the width of this cell (maximum line width).
     *
     * @return the cell width
     */
    public int width() {
        return content.width();
    }

    /**
     * Returns the height of this cell (number of lines).
     *
     * @return the cell height
     */
    public int height() {
        return Math.max(1, content.height());
    }
}

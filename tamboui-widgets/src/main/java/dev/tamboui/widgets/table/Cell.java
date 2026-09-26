/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.table;

import java.util.Objects;

import dev.tamboui.layout.Alignment;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

/**
 * A cell in a {@link Table}.
 * <p>
 * A cell contains styled text content that can be a single line or multi-line text.
 * Cells can have their own style which takes precedence over row and table styles,
 * and an {@link Alignment} that controls horizontal placement within the column.
 *
 * <pre>{@code
 * // Simple cell
 * Cell.from("Hello")
 *
 * // Styled cell
 * Cell.from("Important").style(Style.EMPTY.fg(Color.RED).bold())
 *
 * // Multi-line cell
 * Cell.from(Text.from("Line 1\nLine 2"))
 *
 * // Right-aligned cell
 * Cell.from("42").alignment(Alignment.RIGHT)
 * }</pre>
 */
public final class Cell {

    private final Text content;
    private final Style style;
    private final Alignment alignment;

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
        return new Cell(Text.from(content), Style.EMPTY, Alignment.LEFT);
    }

    /**
     * Creates a cell from a span.
     *
     * @param span the span to use as content
     * @return a new cell
     */
    public static Cell from(Span span) {
        return new Cell(Text.from(span), Style.EMPTY, Alignment.LEFT);
    }

    /**
     * Creates a cell from a line.
     *
     * @param line the line to use as content
     * @return a new cell
     */
    public static Cell from(Line line) {
        return new Cell(Text.from(line), Style.EMPTY, Alignment.LEFT);
    }

    /**
     * Creates a cell from text.
     *
     * @param text the text to use as content
     * @return a new cell
     */
    public static Cell from(Text text) {
        return new Cell(text, Style.EMPTY, Alignment.LEFT);
    }

    /**
     * Creates an empty cell.
     *
     * @return a new empty cell
     */
    public static Cell empty() {
        return new Cell(Text.empty(), Style.EMPTY, Alignment.LEFT);
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
     * Returns the horizontal alignment for this cell.
     *
     * @return the cell alignment
     */
    public Alignment alignment() {
        return alignment;
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
     * Returns a new cell with the given horizontal alignment.
     * <p>
     * Alignment controls the placement of cell content within the column width:
     * {@link Alignment#LEFT} (the default), {@link Alignment#CENTER}, or
     * {@link Alignment#RIGHT}.
     *
     * @param alignment the alignment to apply
     * @return a new cell with the alignment
     */
    public Cell alignment(Alignment alignment) {
        return new Cell(this.content, this.style, Objects.requireNonNull(alignment, "alignment"));
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

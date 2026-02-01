/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

import dev.tamboui.style.Style;
import dev.tamboui.symbols.merge.MergeStrategy;

/**
 * A single cell in the terminal buffer.
 */
public final class Cell {

    /** An empty cell containing a single space with no style. */
    public static final Cell EMPTY = new Cell(" ", Style.EMPTY);

    /**
     * A continuation cell placeholder for the trailing column(s) of a wide character.
     * Wide characters (CJK, emoji) occupy 2 terminal columns; the second column
     * is filled with this cell. Renderers must skip continuation cells.
     */
    public static final Cell CONTINUATION = new Cell("", Style.EMPTY);

    private final String symbol;
    private final Style style;
    private final int cachedHashCode;

    /**
     * Creates a cell with the given symbol and style.
     *
     * @param symbol the character or grapheme cluster displayed in this cell
     * @param style  the visual style
     */
    public Cell(String symbol, Style style) {
        this.symbol = symbol;
        this.style = style;
        this.cachedHashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = symbol.hashCode();
        result = 31 * result + style.hashCode();
        return result;
    }

    /**
     * Returns the symbol displayed in this cell.
     *
     * @return the character or grapheme cluster
     */
    public String symbol() {
        return symbol;
    }

    /**
     * Returns the visual style of this cell.
     *
     * @return the style
     */
    public Style style() {
        return style;
    }

    /**
     * Returns the empty cell singleton.
     *
     * @return {@link #EMPTY}
     */
    public Cell reset() {
        return EMPTY;
    }

    /**
     * Returns a new cell with the given symbol and this cell's style.
     *
     * @param symbol the new symbol
     * @return a new cell
     */
    public Cell symbol(String symbol) {
        return new Cell(symbol, this.style);
    }

    /**
     * Returns a new cell with the given style and this cell's symbol.
     *
     * @param style the new style
     * @return a new cell
     */
    public Cell style(Style style) {
        return new Cell(this.symbol, style);
    }

    /**
     * Returns a new cell with this cell's style patched by the given style.
     *
     * @param patch the style patch to apply
     * @return a new cell with the patched style
     */
    public Cell patchStyle(Style patch) {
        return new Cell(this.symbol, this.style.patch(patch));
    }

    /**
     * Merges this cell's symbol with another symbol using the given merge strategy.
     * Returns a new cell with the merged symbol.
     *
     * @param otherSymbol the symbol to merge with
     * @param strategy the merge strategy to use
     * @return a new cell with the merged symbol
     */
    public Cell mergeSymbol(String otherSymbol, MergeStrategy strategy) {
        String merged = strategy.merge(this.symbol, otherSymbol);
        return new Cell(merged, this.style);
    }

    /**
     * Returns whether this cell is a continuation placeholder for a wide character.
     *
     * @return true if this is a continuation cell
     */
    public boolean isContinuation() {
        return symbol.isEmpty();
    }

    /**
     * Returns whether this cell is empty (single space with no style).
     *
     * @return true if this cell equals {@link #EMPTY}
     */
    public boolean isEmpty() {
        return " ".equals(symbol) && style.equals(Style.EMPTY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cell)) {
            return false;
        }
        Cell cell = (Cell) o;
        if (cachedHashCode != cell.cachedHashCode) {
            return false;
        }
        return symbol.equals(cell.symbol) && style.equals(cell.style);
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return String.format("Cell[symbol=%s, style=%s]", symbol, style);
    }
}

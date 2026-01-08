/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

import dev.tamboui.style.Style;
import dev.tamboui.symbols.merge.MergeStrategy;

/**
 * A single cell in the terminal buffer.
 */
public final class Cell {

    public static final Cell EMPTY = new Cell(" ", Style.EMPTY);

    private final String symbol;
    private final Style style;
    private final int cachedHashCode;

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

    public String symbol() {
        return symbol;
    }

    public Style style() {
        return style;
    }

    public Cell reset() {
        return EMPTY;
    }

    public Cell symbol(String symbol) {
        return new Cell(symbol, this.style);
    }

    public Cell style(Style style) {
        return new Cell(this.symbol, style);
    }

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

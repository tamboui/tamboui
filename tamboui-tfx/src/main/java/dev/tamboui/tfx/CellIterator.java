/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over terminal cells within a rectangular area.
 * <p>
 * CellIterator provides efficient access to terminal cells in a Buffer within a
 * specified rectangular region. It supports optional filtering via CellFilter to
 * selectively process cells based on their properties.
 * <p>
 * For optimal performance, prefer {@link #forEachCell(java.util.function.BiConsumer)}
 * over iterator-based iteration when you don't need iterator combinators.
 */
public final class CellIterator implements Iterable<CellIterator.CellEntry> {
    
    private final Buffer buffer;
    private final Rect area;
    private final CellFilter filter;
    
    /**
     * Creates a new CellIterator over the specified area of a buffer.
     * 
     * @param buffer The buffer to iterate over
     * @param area The rectangular area to iterate over
     */
    public CellIterator(Buffer buffer, Rect area) {
        this(buffer, area, CellFilter.all());
    }
    
    /**
     * Creates a new CellIterator over the specified area of a buffer with filtering.
     * 
     * @param buffer The buffer to iterate over
     * @param area The rectangular area to iterate over
     * @param filter The cell filter to apply (null means no filtering)
     */
    public CellIterator(Buffer buffer, Rect area, CellFilter filter) {
        this.buffer = buffer;
        // Intersect with buffer area
        this.area = area.intersection(buffer.area());
        this.filter = filter != null ? filter : CellFilter.all();
    }
    
    /**
     * Applies a function to each cell in the iterator's area.
     * <p>
     * This is the preferred method for iterating over cells when you don't need
     * iterator combinators. It's significantly faster than using the Iterator
     * interface because it avoids coordinate calculations.
     * <p>
     * The function receives the cell's position and the cell itself. To modify
     * the cell, use {@link Buffer#set(Position, Cell)}.
     * 
     * @param consumer A function that takes (Position, Cell) and processes each cell
     */
    public void forEachCell(java.util.function.BiConsumer<Position, Cell> consumer) {
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                Position pos = new Position(x, y);
                Cell cell = buffer.get(pos);
                if (filter.matches(pos, cell, area)) {
                    consumer.accept(pos, cell);
                }
            }
        }
    }
    
    /**
     * Applies a function to each cell in the iterator's area, allowing mutation.
     * <p>
     * This variant provides a mutable cell consumer that can modify cells directly.
     * The consumer receives the position and a mutable cell reference.
     *
     * @param consumer A function that takes (Position, MutableCell) and processes each cell
     * @deprecated Use {@link #forEachCellMutable(CellMutator)} instead for better performance
     */
    @Deprecated
    public void forEachCellMutable(java.util.function.BiConsumer<Position, MutableCell> consumer) {
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                Position pos = new Position(x, y);
                Cell cell = buffer.get(x, y);
                if (filter.matches(x, y, cell, area)) {
                    MutableCell mutable = new MutableCell(buffer);
                    mutable.reset(x, y, cell);
                    consumer.accept(pos, mutable);
                }
            }
        }
    }

    /**
     * Applies a function to each cell in the iterator's area, allowing mutation.
     * <p>
     * This is the preferred method for mutating cells. It avoids object allocation
     * by reusing a single MutableCell instance and passing primitive coordinates.
     * The MutableCell is reset for each cell, so do not retain references to it.
     *
     * @param mutator A function that takes (x, y, MutableCell) and processes each cell
     */
    public void forEachCellMutable(CellMutator mutator) {
        MutableCell reusable = new MutableCell(buffer);
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                Cell cell = buffer.get(x, y);
                if (filter.matches(x, y, cell, area)) {
                    reusable.reset(x, y, cell);
                    mutator.mutate(x, y, reusable);
                }
            }
        }
    }

    /**
     * A functional interface for mutating cells with primitive coordinates.
     * <p>
     * This interface is used by {@link #forEachCellMutable(CellMutator)} to avoid
     * Position object allocation in performance-critical loops.
     */
    @FunctionalInterface
    public interface CellMutator {
        /**
         * Processes and optionally mutates a cell.
         *
         * @param x The cell's x coordinate
         * @param y The cell's y coordinate
         * @param cell The mutable cell wrapper
         */
        void mutate(int x, int y, MutableCell cell);
    }
    
    @Override
    public Iterator<CellEntry> iterator() {
        return new CellIteratorImpl();
    }
    
    /**
     * Represents a cell entry with its position and value.
     */
    public static final class CellEntry {
        private final Position position;
        private final Cell cell;
        
        CellEntry(Position position, Cell cell) {
            this.position = position;
            this.cell = cell;
        }

        /**
         * Returns the position of this cell entry.
         *
         * @return the cell position
         */
        public Position position() {
            return position;
        }

        /**
         * Returns the cell value of this entry.
         *
         * @return the cell
         */
        public Cell cell() {
            return cell;
        }
    }
    
    /**
     * A mutable cell wrapper that allows modifying cells in the buffer.
     * <p>
     * This class is designed to be reused across iterations to avoid object allocation.
     * Call {@link #reset(int, int, Cell)} to prepare it for a new cell position.
     */
    public static final class MutableCell {
        private final Buffer buffer;
        private int posX;
        private int posY;
        private Cell cell;

        MutableCell(Buffer buffer) {
            this.buffer = buffer;
        }

        /**
         * Resets this mutable cell to a new position and cell value.
         * This allows the same MutableCell instance to be reused across iterations.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @param cell the cell at this position
         */
        void reset(int x, int y, Cell cell) {
            this.posX = x;
            this.posY = y;
            this.cell = cell;
        }

        /**
         * Returns the cell's x coordinate.
         *
         * @return the x coordinate
         */
        public int x() {
            return posX;
        }

        /**
         * Returns the cell's y coordinate.
         *
         * @return the y coordinate
         */
        public int y() {
            return posY;
        }

        /**
         * Returns a Position object for this cell.
         * <p>
         * Note: This creates a new Position object. Prefer using {@link #x()} and {@link #y()}
         * when possible to avoid allocation.
         *
         * @return the position
         */
        public Position position() {
            return new Position(posX, posY);
        }

        /**
         * Returns the current cell value.
         *
         * @return the cell
         */
        public Cell cell() {
            return cell;
        }

        /**
         * Sets the cell's symbol.
         *
         * @param symbol the new symbol
         */
        public void setSymbol(String symbol) {
            this.cell = this.cell.symbol(symbol);
            buffer.set(posX, posY, this.cell);
        }

        /**
         * Sets the cell's style.
         *
         * @param style the new style
         */
        public void setStyle(Style style) {
            this.cell = this.cell.style(style);
            buffer.set(posX, posY, this.cell);
        }

        /**
         * Patches the cell's style with the given style.
         *
         * @param style the style to patch
         */
        public void patchStyle(Style style) {
            this.cell = this.cell.patchStyle(style);
            buffer.set(posX, posY, this.cell);
        }

        /**
         * Sets the cell's foreground color directly.
         * <p>
         * This is more efficient than creating a Style and calling patchStyle
         * when only the foreground color needs to change.
         *
         * @param color the foreground color
         */
        public void setFg(Color color) {
            Style newStyle = this.cell.style().fg(color);
            this.cell = new Cell(this.cell.symbol(), newStyle);
            buffer.set(posX, posY, this.cell);
        }

        /**
         * Sets the cell's background color directly.
         * <p>
         * This is more efficient than creating a Style and calling patchStyle
         * when only the background color needs to change.
         *
         * @param color the background color
         */
        public void setBg(Color color) {
            Style newStyle = this.cell.style().bg(color);
            this.cell = new Cell(this.cell.symbol(), newStyle);
            buffer.set(posX, posY, this.cell);
        }

        /**
         * Sets the entire cell.
         *
         * @param cell the new cell
         */
        public void setCell(Cell cell) {
            this.cell = cell;
            buffer.set(posX, posY, cell);
        }
    }
    
    private class CellIteratorImpl implements Iterator<CellEntry> {
        private int currentX = area.left();
        private int currentY = area.top();
        
        @Override
        public boolean hasNext() {
            return currentY < area.bottom() && currentX < area.right();
        }
        
        @Override
        public CellEntry next() {
            while (hasNext()) {
                Position pos = new Position(currentX, currentY);
                Cell cell = buffer.get(pos);
                
                // Advance to next position
                currentX++;
                if (currentX >= area.right()) {
                    currentX = area.left();
                    currentY++;
                }
                
                // Check filter
                if (filter.matches(pos, cell, area)) {
                    return new CellEntry(pos, cell);
                }
            }
            throw new NoSuchElementException();
        }
    }
}


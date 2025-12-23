/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;

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
     */
    public void forEachCellMutable(java.util.function.BiConsumer<Position, MutableCell> consumer) {
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                Position pos = new Position(x, y);
                Cell cell = buffer.get(pos);
                if (filter.matches(pos, cell, area)) {
                    MutableCell mutable = new MutableCell(pos, cell, buffer);
                    consumer.accept(pos, mutable);
                }
            }
        }
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
        
        public Position position() {
            return position;
        }
        
        public Cell cell() {
            return cell;
        }
    }
    
    /**
     * A mutable cell wrapper that allows modifying cells in the buffer.
     */
    public static final class MutableCell {
        private final Position position;
        private final Buffer buffer;
        private Cell cell;
        
        MutableCell(Position position, Cell cell, Buffer buffer) {
            this.position = position;
            this.cell = cell;
            this.buffer = buffer;
        }
        
        public Position position() {
            return position;
        }
        
        public Cell cell() {
            return cell;
        }
        
        /**
         * Sets the cell's symbol.
         */
        public void setSymbol(String symbol) {
            this.cell = this.cell.symbol(symbol);
            buffer.set(position, this.cell);
        }
        
        /**
         * Sets the cell's style.
         */
        public void setStyle(dev.tamboui.style.Style style) {
            this.cell = this.cell.style(style);
            buffer.set(position, this.cell);
        }
        
        /**
         * Patches the cell's style.
         */
        public void patchStyle(dev.tamboui.style.Style style) {
            this.cell = this.cell.patchStyle(style);
            buffer.set(position, this.cell);
        }
        
        /**
         * Sets the entire cell.
         */
        public void setCell(Cell cell) {
            this.cell = cell;
            buffer.set(position, cell);
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


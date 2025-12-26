/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;

import java.util.List;

/**
 * A filter that enables effects to operate on specific cells based on various criteria.
 * <p>
 * CellFilter provides a flexible, composable way to select cells for applying effects.
 * Filters can match cells based on their properties (colors, position, content) or
 * custom predicates, and can be combined using logical operations (AND, OR, NOR).
 * <p>
 * <b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Composability:</b> Simple filters can be combined to create complex
 *       selection patterns using {@code allOf}, {@code anyOf}, and {@code noneOf}.</li>
 *   <li><b>Performance:</b> Filters are evaluated during cell iteration, allowing
 *       effects to skip irrelevant cells efficiently.</li>
 *   <li><b>Flexibility:</b> Supports both spatial (area, margin) and content-based
 *       (color, text) filtering.</li>
 * </ul>
 * <p>
 * <b>Filter Types:</b>
 * <ul>
 *   <li><b>Spatial:</b> {@code area}, {@code inner}, {@code outer}</li>
 *   <li><b>Color:</b> {@code fgColor}, {@code bgColor}</li>
 *   <li><b>Content:</b> {@code text}</li>
 *   <li><b>Logical:</b> {@code allOf}, {@code anyOf}, {@code noneOf}</li>
 *   <li><b>Special:</b> {@code all}, {@code none}</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Simple filter
 * Effect effect = Fx.dissolve(2000, Interpolation.Linear)
 *     .withFilter(CellFilter.text());
 * 
 * // Combined filters
 * CellFilter combined = CellFilter.allOf(
 *     CellFilter.text(),
 *     CellFilter.fgColor(Color.WHITE),
 *     CellFilter.outer(Margin.uniform(1))
 * );
 * Effect filtered = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
 *     .withFilter(combined);
 * }</pre>
 * <p>
 * Filters are evaluated during effect execution, allowing effects to efficiently
 * skip cells that don't match the filter criteria.
 */
public abstract class CellFilter {
    
    /**
     * Selects every cell in the area (no filtering).
     */
    public static CellFilter all() {
        return All.INSTANCE;
    }
    
    /**
     * Selects no cells.
     */
    public static CellFilter none() {
        return None.INSTANCE;
    }
    
    /**
     * Selects cells within the specified rectangular area.
     */
    public static CellFilter area(Rect area) {
        return new Area(area);
    }
    
    /**
     * Selects cells with a matching foreground color.
     */
    public static CellFilter fgColor(Color color) {
        return new FgColor(color);
    }
    
    /**
     * Selects cells with a matching background color.
     */
    public static CellFilter bgColor(Color color) {
        return new BgColor(color);
    }
    
    /**
     * Selects cells within the inner margin of the area.
     */
    public static CellFilter inner(Margin margin) {
        return new Inner(margin);
    }
    
    /**
     * Selects cells outside the inner margin of the area (border region).
     */
    public static CellFilter outer(Margin margin) {
        return new Outer(margin);
    }
    
    /**
     * Selects cells containing textual content.
     */
    public static CellFilter text() {
        return Text.INSTANCE;
    }
    
    /**
     * Selects cells that match ALL of the given filters (logical AND).
     */
    public static CellFilter allOf(List<CellFilter> filters) {
        return new AllOf(filters);
    }
    
    /**
     * Selects cells that match ANY of the given filters (logical OR).
     */
    public static CellFilter anyOf(List<CellFilter> filters) {
        return new AnyOf(filters);
    }
    
    /**
     * Selects cells that match NONE of the given filters (logical NOR).
     */
    public static CellFilter noneOf(List<CellFilter> filters) {
        return new NoneOf(filters);
    }
    
    /**
     * Inverts the result of the given filter (logical NOT).
     */
    public static CellFilter not(CellFilter filter) {
        return new Not(filter);
    }
    
    /**
     * Selects cells using a custom position-based predicate function.
     */
    public static CellFilter positionFn(java.util.function.Predicate<Position> predicate) {
        return new PositionFn(predicate);
    }
    
    /**
     * Selects cells using a custom cell-content-based predicate function.
     */
    public static CellFilter evalCell(java.util.function.Predicate<Cell> predicate) {
        return new EvalCell(predicate);
    }
    
    /**
     * Checks if a cell matches this filter.
     * 
     * @param position The cell's position
     * @param cell The cell to check
     * @param area The area being processed
     * @return true if the cell matches the filter
     */
    public abstract boolean matches(Position position, Cell cell, Rect area);
    
    // Concrete implementations
    
    private static final class All extends CellFilter {
        static final All INSTANCE = new All();
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            return true;
        }
    }
    
    private static final class None extends CellFilter {
        static final None INSTANCE = new None();
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            return false;
        }
    }
    
    private static final class Area extends CellFilter {
        private final Rect filterArea;
        
        Area(Rect filterArea) {
            this.filterArea = filterArea;
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            return filterArea.contains(position);
        }
    }
    
    private static final class FgColor extends CellFilter {
        private final Color color;
        
        FgColor(Color color) {
            this.color = color;
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            java.util.Optional<Color> cellFg = cell.style().fg();
            return cellFg.isPresent() && color.equals(cellFg.get());
        }
    }
    
    private static final class BgColor extends CellFilter {
        private final Color color;
        
        BgColor(Color color) {
            this.color = color;
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            java.util.Optional<Color> cellBg = cell.style().bg();
            return cellBg.isPresent() && color.equals(cellBg.get());
        }
    }
    
    private static final class Inner extends CellFilter {
        private final Margin margin;
        
        Inner(Margin margin) {
            this.margin = margin;
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            Rect inner = area.inner(margin);
            return inner.contains(position);
        }
    }
    
    private static final class Outer extends CellFilter {
        private final Margin margin;
        
        Outer(Margin margin) {
            this.margin = margin;
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            Rect inner = area.inner(margin);
            return !inner.contains(position);
        }
    }
    
    private static final class Text extends CellFilter {
        static final Text INSTANCE = new Text();
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            if (cell.isEmpty()) {
                return false;
            }
            String symbol = cell.symbol();
            if (symbol == null || symbol.isEmpty() || " ".equals(symbol)) {
                return false;
            }
            // Check if symbol contains text characters (letters, numbers, punctuation, and common symbols)
            // Base matches Rust: is_alphabetic() || is_numeric() || " ?!.,:;()".contains(ch)
            // Extended to include common symbols that are part of text content: @#$%^&*+-=_[ ]{}|\/<>"'~`
            for (int i = 0; i < symbol.length(); i++) {
                char c = symbol.charAt(i);
                if (Character.isLetterOrDigit(c) || 
                    c == '?' || c == '!' || c == '.' || c == ',' || 
                    c == ':' || c == ';' || c == '(' || c == ')' ||
                    // Extended: common symbols that should be considered part of text content
                    c == '@' || c == '#' || c == '$' || c == '%' || 
                    c == '^' || c == '&' || c == '*' || c == '+' || 
                    c == '-' || c == '=' || c == '_' || c == '[' || 
                    c == ']' || c == '{' || c == '}' || c == '|' || 
                    c == '\\' || c == '/' || c == '<' || c == '>' || 
                    c == '"' || c == '\'' || c == '`' || c == '~') {
                    return true;
                }
            }
            return false;
        }
    }
    
    private static final class AllOf extends CellFilter {
        private final List<CellFilter> filters;
        
        AllOf(List<CellFilter> filters) {
            this.filters = new java.util.ArrayList<>(filters);
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            for (CellFilter filter : filters) {
                if (!filter.matches(position, cell, area)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    private static final class AnyOf extends CellFilter {
        private final List<CellFilter> filters;
        
        AnyOf(List<CellFilter> filters) {
            this.filters = new java.util.ArrayList<>(filters);
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            for (CellFilter filter : filters) {
                if (filter.matches(position, cell, area)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    private static final class NoneOf extends CellFilter {
        private final List<CellFilter> filters;
        
        NoneOf(List<CellFilter> filters) {
            this.filters = new java.util.ArrayList<>(filters);
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            for (CellFilter filter : filters) {
                if (filter.matches(position, cell, area)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    private static final class Not extends CellFilter {
        private final CellFilter filter;
        
        Not(CellFilter filter) {
            this.filter = filter;
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            return !filter.matches(position, cell, area);
        }
    }
    
    private static final class PositionFn extends CellFilter {
        private final java.util.function.Predicate<Position> predicate;
        
        PositionFn(java.util.function.Predicate<Position> predicate) {
            this.predicate = predicate;
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            return predicate.test(position);
        }
    }
    
    private static final class EvalCell extends CellFilter {
        private final java.util.function.Predicate<Cell> predicate;
        
        EvalCell(java.util.function.Predicate<Cell> predicate) {
            this.predicate = predicate;
        }
        
        @Override
        public boolean matches(Position position, Cell cell, Rect area) {
            return predicate.test(cell);
        }
    }
}


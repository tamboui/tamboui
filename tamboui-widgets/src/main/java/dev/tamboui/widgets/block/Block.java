/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.symbols.merge.MergeStrategy;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.Widget;
import dev.tamboui.widgets.text.Overflow;

import java.util.EnumSet;
import java.util.List;

/**
 * A block is a container widget with optional borders and titles.
 */
public final class Block implements Widget {

    private final Title title;
    private final Title titleBottom;
    private final EnumSet<Borders> borders;
    private final BorderType borderType;
    private final Style borderStyle;
    private final Style style;
    private final Padding padding;
    private final MergeStrategy mergeStrategy;

    private Block(Builder builder) {
        this.title = builder.title;
        this.titleBottom = builder.titleBottom;
        this.borders = builder.borders;
        this.borderType = builder.borderType;
        this.borderStyle = builder.borderStyle;
        this.style = builder.style;
        this.padding = builder.padding;
        this.mergeStrategy = builder.mergeStrategy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Block bordered() {
        return builder().borders(Borders.ALL).build();
    }

    public static Block empty() {
        return builder().build();
    }

    /**
     * Returns the inner area after accounting for borders and padding.
     */
    public Rect inner(Rect area) {
        int x = area.x();
        int y = area.y();
        int width = area.width();
        int height = area.height();

        // Account for borders
        if (borders.contains(Borders.LEFT)) {
            x += 1;
            width -= 1;
        }
        if (borders.contains(Borders.TOP)) {
            y += 1;
            height -= 1;
        }
        if (borders.contains(Borders.RIGHT)) {
            width -= 1;
        }
        if (borders.contains(Borders.BOTTOM)) {
            height -= 1;
        }

        // Account for padding
        x += padding.left();
        y += padding.top();
        width -= padding.horizontalTotal();
        height -= padding.verticalTotal();

        return new Rect(x, y, Math.max(0, width), Math.max(0, height));
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        // Fill background
        buffer.setStyle(area, style);

        // Draw borders
        if (!borders.isEmpty()) {
            renderBorders(area, buffer);
        }

        // Draw titles
        if (title != null) {
            renderTitle(title, area, buffer, true);
        }
        if (titleBottom != null) {
            renderTitle(titleBottom, area, buffer, false);
        }
    }

    private void renderBorders(Rect area, Buffer buffer) {
        BorderSet set = borderType.set();

        // When merge strategy is not REPLACE, skip corner positions when rendering sides
        // This prevents corners from being merged with side characters incorrectly
        boolean isReplace = mergeStrategy == MergeStrategy.REPLACE;
        int leftInset = area.left() + (isReplace || !borders.contains(Borders.LEFT) ? 0 : 1);
        int topInset = area.top() + (isReplace || !borders.contains(Borders.TOP) ? 0 : 1);
        int rightInset = area.right() - 1 - (isReplace || !borders.contains(Borders.RIGHT) ? 0 : 1);
        int bottomInset = area.bottom() - 1 - (isReplace || !borders.contains(Borders.BOTTOM) ? 0 : 1);

        // Top border (skip corners if not REPLACE)
        if (borders.contains(Borders.TOP) && area.height() > 0) {
            for (int x = leftInset; x <= rightInset; x++) {
                setBorderCell(buffer, x, area.top(), set.topHorizontal(), borderStyle);
            }
        }

        // Bottom border (skip corners if not REPLACE)
        if (borders.contains(Borders.BOTTOM) && area.height() > 1) {
            for (int x = leftInset; x <= rightInset; x++) {
                setBorderCell(buffer, x, area.bottom() - 1, set.bottomHorizontal(), borderStyle);
            }
        }

        // Left border (skip corners if not REPLACE)
        if (borders.contains(Borders.LEFT) && area.width() > 0) {
            for (int y = topInset; y <= bottomInset; y++) {
                setBorderCell(buffer, area.left(), y, set.leftVertical(), borderStyle);
            }
        }

        // Right border (skip corners if not REPLACE)
        if (borders.contains(Borders.RIGHT) && area.width() > 1) {
            for (int y = topInset; y <= bottomInset; y++) {
                setBorderCell(buffer, area.right() - 1, y, set.rightVertical(), borderStyle);
            }
        }

        // Corners
        boolean hasTop = borders.contains(Borders.TOP);
        boolean hasBottom = borders.contains(Borders.BOTTOM);
        boolean hasLeft = borders.contains(Borders.LEFT);
        boolean hasRight = borders.contains(Borders.RIGHT);

        if (hasTop && hasLeft) {
            setBorderCell(buffer, area.left(), area.top(), set.topLeft(), borderStyle);
        }
        if (hasTop && hasRight && area.width() > 1) {
            setBorderCell(buffer, area.right() - 1, area.top(), set.topRight(), borderStyle);
        }
        if (hasBottom && hasLeft && area.height() > 1) {
            setBorderCell(buffer, area.left(), area.bottom() - 1, set.bottomLeft(), borderStyle);
        }
        if (hasBottom && hasRight && area.width() > 1 && area.height() > 1) {
            setBorderCell(buffer, area.right() - 1, area.bottom() - 1, set.bottomRight(), borderStyle);
        }
    }
    
    private void setBorderCell(Buffer buffer, int x, int y, String symbol, Style style) {
        if (mergeStrategy != MergeStrategy.REPLACE) {
            Cell existing = buffer.get(x, y);
            String existingSymbol = existing.symbol();
            boolean existingIsText = !MergeStrategy.isBorderSymbol(existingSymbol);
            boolean newIsBorder = MergeStrategy.isBorderSymbol(symbol);
            
            // If existing cell is empty (space), just set the new cell
            if (" ".equals(existingSymbol)) {
                buffer.set(x, y, new Cell(symbol, style));
                return;
            }
            
            // Check if existing cell contains non-border text (like titles)
            // If we're trying to draw a border over non-border text, preserve the text but update style
            if (existingIsText && newIsBorder) {
                // Preserve existing non-border text (like titles) when drawing borders
                // But update the style to match the new cell's style
                buffer.set(x, y, new Cell(existing.symbol(), style));
                return;
            }
            
            // Both are borders (or existing is a border) - merge the symbols
            Cell merged = existing.mergeSymbol(symbol, mergeStrategy);
            // Use the new cell's style (last rendered takes precedence for style)
            buffer.set(x, y, new Cell(merged.symbol(), style));
        } else {
            // REPLACE strategy - just set the cell directly
            buffer.set(x, y, new Cell(symbol, style));
        }
    }

    private static final String ELLIPSIS = "...";

    private void renderTitle(Title title, Rect area, Buffer buffer, boolean top) {
        int y = top ? area.top() : area.bottom() - 1;
        int availableWidth = area.width();

        if (borders.contains(Borders.LEFT)) {
            availableWidth -= 1;
        }
        if (borders.contains(Borders.RIGHT)) {
            availableWidth -= 1;
        }

        if (availableWidth <= 0) {
            return;
        }

        // Apply overflow handling if title is too wide
        Line titleLine = title.content();
        int contentWidth = titleLine.width();

        if (contentWidth > availableWidth) {
            titleLine = applyTitleOverflow(titleLine, availableWidth, title.overflow());
            contentWidth = titleLine.width();
        }

        int titleWidth = Math.min(contentWidth, availableWidth);
        int startX = area.left() + (borders.contains(Borders.LEFT) ? 1 : 0);

        // Calculate x position based on alignment
        int x;
        switch (title.alignment()) {
            case LEFT:
                x = startX;
                break;
            case CENTER:
                x = startX + (availableWidth - titleWidth) / 2;
                break;
            case RIGHT:
            default:
                x = startX + availableWidth - titleWidth;
                break;
        }

        // Apply borderStyle to title if it's set (non-empty)
        if (!borderStyle.equals(Style.EMPTY)) {
            titleLine = titleLine.patchStyle(borderStyle);
        }

        // When merge strategy is active, only render title if cells are empty or borders
        // This prevents overwriting titles from overlapping blocks
        if (mergeStrategy != MergeStrategy.REPLACE) {
            renderTitleWithMerge(x, y, titleLine, buffer);
        } else {
            buffer.setLine(x, y, titleLine);
        }
    }

    private Line applyTitleOverflow(Line line, int maxWidth, Overflow overflow) {
        if (overflow == Overflow.CLIP || overflow == Overflow.WRAP_CHARACTER || overflow == Overflow.WRAP_WORD) {
            // CLIP: just let it be clipped by the buffer
            // WRAP modes don't make sense for titles, treat as CLIP
            return line;
        }

        // Extract text content and style
        String fullText = lineToString(line);
        Style lineStyle = getLineStyle(line);

        if (maxWidth <= ELLIPSIS.length()) {
            // Not enough room for ellipsis, just clip
            return Line.from(new Span(fullText.substring(0, Math.min(fullText.length(), maxWidth)), lineStyle));
        }

        String truncated;
        switch (overflow) {
            case ELLIPSIS:
                truncated = truncateEnd(fullText, maxWidth);
                break;
            case ELLIPSIS_START:
                truncated = truncateStart(fullText, maxWidth);
                break;
            case ELLIPSIS_MIDDLE:
                truncated = truncateMiddle(fullText, maxWidth);
                break;
            default:
                return line;
        }

        return Line.from(new Span(truncated, lineStyle));
    }

    private String truncateEnd(String text, int maxWidth) {
        int availableChars = maxWidth - ELLIPSIS.length();
        return text.substring(0, availableChars) + ELLIPSIS;
    }

    private String truncateStart(String text, int maxWidth) {
        int availableChars = maxWidth - ELLIPSIS.length();
        return ELLIPSIS + text.substring(text.length() - availableChars);
    }

    private String truncateMiddle(String text, int maxWidth) {
        int availableChars = maxWidth - ELLIPSIS.length();
        int leftChars = (availableChars + 1) / 2;
        int rightChars = availableChars / 2;
        return text.substring(0, leftChars) + ELLIPSIS + text.substring(text.length() - rightChars);
    }

    private String lineToString(Line line) {
        StringBuilder sb = new StringBuilder();
        for (Span span : line.spans()) {
            sb.append(span.content());
        }
        return sb.toString();
    }

    private Style getLineStyle(Line line) {
        List<Span> spans = line.spans();
        return spans.isEmpty() ? Style.EMPTY : spans.get(0).style();
    }
    
    private void renderTitleWithMerge(int x, int y, Line titleLine, Buffer buffer) {
        List<Span> spans = titleLine.spans();
        int col = x;
        for (Span span : spans) {
            String content = span.content();
            for (int i = 0; i < content.length(); ) {
                int codePoint = content.codePointAt(i);
                String symbol = new String(Character.toChars(codePoint));
                
                Cell existing = buffer.get(col, y);
                String existingSymbol = existing.symbol();
                
                // In Ratatui, titles are rendered using Line.render() which calls set_symbol()
                // directly, overwriting cells. However, when merge strategy is active,
                // we should preserve existing non-border text (like other titles).
                // Only write to cells that are:
                // 1. Empty (space character) - can be overwritten
                // 2. Border characters - can be overwritten (borders are merged separately)
                // 3. NOT non-border text (like other titles) - should be preserved
                
                // In Ratatui, Line.render() uses set_symbol() which overwrites cells.
                // However, when merge strategy is active (EXACT/FUZZY), we should preserve
                // existing non-border text (like other titles) when they don't overlap.
                // Only write to cells that are:
                // 1. Empty (space character) - can be overwritten
                // 2. Border characters - can be overwritten (borders are merged separately)
                // 3. NOT non-border text (like other titles) - should be preserved
                
                // Check if cell is empty (space) or contains a border character
                // Empty cells are a space character (" ") when queried:
                boolean isEmpty = " ".equals(existingSymbol);
                boolean isBorder = MergeStrategy.isBorderSymbol(existingSymbol);
                
                // Only write to empty cells or cells with border characters
                // This preserves existing title text from other blocks when they don't overlap
                // If cell contains non-border text (like another title), preserve it
                if (isEmpty || isBorder) {
                    buffer.set(col, y, new Cell(symbol, span.style()));
                } else {
                    // Cell contains existing non-border text (like another title)
                    // Preserve the symbol but update the style to match the new title's style
                    // This ensures all titles get the same style when blocks overlap
                    buffer.set(col, y, new Cell(existingSymbol, span.style()));
                }
                // This allows titles from different blocks to coexist when they don't overlap,
                // while ensuring they all share the same style
                
                col++;
                i += Character.charCount(codePoint);
            }
        }
    }

    public static final class Builder {
        private Title title;
        private Title titleBottom;
        private EnumSet<Borders> borders = Borders.NONE;
        private BorderType borderType = BorderType.PLAIN;
        private Style borderStyle = Style.EMPTY;
        private Style style = Style.EMPTY;
        private Padding padding = Padding.NONE;
        private MergeStrategy mergeStrategy = MergeStrategy.REPLACE;

        private Builder() {}

        public Builder title(String title) {
            this.title = Title.from(title);
            return this;
        }

        public Builder title(Title title) {
            this.title = title;
            return this;
        }

        public Builder titleBottom(String title) {
            this.titleBottom = Title.from(title);
            return this;
        }

        public Builder titleBottom(Title title) {
            this.titleBottom = title;
            return this;
        }

        public Builder borders(EnumSet<Borders> borders) {
            this.borders = EnumSet.copyOf(borders);
            return this;
        }

        public Builder borderType(BorderType borderType) {
            this.borderType = borderType;
            return this;
        }

        public Builder borderStyle(Style borderStyle) {
            this.borderStyle = borderStyle;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder padding(Padding padding) {
            this.padding = padding;
            return this;
        }

        public Builder padding(int value) {
            this.padding = Padding.uniform(value);
            return this;
        }

        /**
         * Sets the merge strategy for borders.
         * <p>
         * When blocks are rendered with overlapping borders, this strategy determines
         * how the borders are merged. See {@link MergeStrategy} for details.
         *
         * @param mergeStrategy the merge strategy to use
         * @return this builder
         */
        public Builder mergeBorders(MergeStrategy mergeStrategy) {
            this.mergeStrategy = mergeStrategy;
            return this;
        }

        public Block build() {
            return new Block(this);
        }
    }
}

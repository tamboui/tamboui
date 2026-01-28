/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Padding;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.BorderCharConverter;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.StringConverter;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.symbols.merge.MergeStrategy;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.Widget;

import java.util.EnumSet;
import java.util.List;

/**
 * A block is a container widget with optional borders and titles.
 * <p>
 * Supports style-aware properties: {@code border-type}, {@code border-color},
 * {@code background}, and {@code color}.
 */
public final class Block implements Widget {
    /**
     * The {@code border-type} property for border style.
     * This property is inheritable - nested panels inherit border type.
     * Default: {@link BorderType#PLAIN}
     */
    public static final PropertyDefinition<BorderType> BORDER_TYPE =
            PropertyDefinition.builder("border-type", BorderTypeConverter.INSTANCE)
                    .inheritable()
                    .defaultValue(BorderType.PLAIN)
                    .build();

    /**
     * The {@code border-color} property.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<Color> BORDER_COLOR =
            PropertyDefinition.of("border-color", ColorConverter.INSTANCE);


    // ═══════════════════════════════════════════════════════════════
    // Border character properties (strings)
    // ═══════════════════════════════════════════════════════════════

    /**
     * The {@code border-chars} property for custom border character sets.
     * Format: 8 quoted strings (top-h, bottom-h, left-v, right-v, tl, tr, bl, br).
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<String> BORDER_CHARS =
            PropertyDefinition.of("border-chars", StringConverter.INSTANCE);

    /**
     * The {@code border-top} property for the top horizontal border character.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<String> BORDER_TOP =
            PropertyDefinition.of("border-top", BorderCharConverter.INSTANCE);

    /**
     * The {@code border-bottom} property for the bottom horizontal border character.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<String> BORDER_BOTTOM =
            PropertyDefinition.of("border-bottom", BorderCharConverter.INSTANCE);

    /**
     * The {@code border-left} property for the left vertical border character.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<String> BORDER_LEFT =
            PropertyDefinition.of("border-left", BorderCharConverter.INSTANCE);

    /**
     * The {@code border-right} property for the right vertical border character.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<String> BORDER_RIGHT =
            PropertyDefinition.of("border-right", BorderCharConverter.INSTANCE);

    /**
     * The {@code border-top-left} property for the top-left corner character.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<String> BORDER_TOP_LEFT =
            PropertyDefinition.of("border-top-left", BorderCharConverter.INSTANCE);

    /**
     * The {@code border-top-right} property for the top-right corner character.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<String> BORDER_TOP_RIGHT =
            PropertyDefinition.of("border-top-right", BorderCharConverter.INSTANCE);

    /**
     * The {@code border-bottom-left} property for the bottom-left corner character.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<String> BORDER_BOTTOM_LEFT =
            PropertyDefinition.of("border-bottom-left", BorderCharConverter.INSTANCE);

    /**
     * The {@code border-bottom-right} property for the bottom-right corner character.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<String> BORDER_BOTTOM_RIGHT =
            PropertyDefinition.of("border-bottom-right", BorderCharConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(
                BORDER_TYPE,
                BORDER_COLOR,
                BORDER_CHARS,
                BORDER_TOP,
                BORDER_BOTTOM,
                BORDER_LEFT,
                BORDER_RIGHT,
                BORDER_TOP_LEFT,
                BORDER_TOP_RIGHT,
                BORDER_BOTTOM_LEFT,
                BORDER_BOTTOM_RIGHT
        );
    }

    private final Title title;
    private final Title titleBottom;
    private final EnumSet<Borders> borders;
    private final BorderType borderType;
    private final BorderSet customBorderSet;
    private final Style borderStyle;
    private final Style style;
    private final Padding padding;
    private final MergeStrategy mergeStrategy;

    private Block(Builder builder) {
        this.title = builder.title;
        this.titleBottom = builder.titleBottom;
        this.borders = builder.borders;
        this.borderType = builder.resolveBorderType();
        this.customBorderSet = builder.customBorderSet;
        this.padding = builder.padding;
        this.mergeStrategy = builder.mergeStrategy;

        Style baseBorderStyle = builder.borderStyle;
        Color resolvedBorderColor = builder.resolveBorderColor();
        if (resolvedBorderColor != null) {
            baseBorderStyle = baseBorderStyle.fg(resolvedBorderColor);
        }
        this.borderStyle = baseBorderStyle;

        Color resolvedBackground = builder.resolveBackground();
        Color resolvedForeground = builder.resolveForeground();
        Style baseStyle = builder.style;
        if (resolvedBackground != null) {
            baseStyle = baseStyle.bg(resolvedBackground);
        }
        if (resolvedForeground != null) {
            baseStyle = baseStyle.fg(resolvedForeground);
        }
        this.style = baseStyle;
    }

    /**
     * Creates a new block builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a block with all borders enabled.
     *
     * @return a bordered block
     */
    public static Block bordered() {
        return builder().borders(Borders.ALL).build();
    }

    /**
     * Creates an empty block with no borders.
     *
     * @return an empty block
     */
    public static Block empty() {
        return builder().build();
    }

    /**
     * Returns the inner area after accounting for borders, titles, and padding.
     *
     * @param area the outer area
     * @return the inner area
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
        } else if (title != null) {
            // Reserve space for title even without top border
            y += 1;
            height -= 1;
        }
        if (borders.contains(Borders.RIGHT)) {
            width -= 1;
        }
        if (borders.contains(Borders.BOTTOM)) {
            height -= 1;
        } else if (titleBottom != null) {
            // Reserve space for bottom title even without bottom border
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

        // Draw borders (also when customBorderSet is provided, even with borders=NONE)
        if (!borders.isEmpty() || customBorderSet != null) {
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
        // Use custom border set if provided, otherwise use borderType's set
        BorderSet set = customBorderSet != null ? customBorderSet : borderType.set();
        if (set == null) {
            // BorderType.NONE - skip border rendering
            return;
        }

        // When merge strategy is not REPLACE, skip corner positions when rendering sides
        // This prevents corners from being merged with side characters incorrectly
        boolean isReplace = mergeStrategy == MergeStrategy.REPLACE;
        int leftInset = area.left() + (isReplace || !borders.contains(Borders.LEFT) ? 0 : 1);
        int topInset = area.top() + (isReplace || !borders.contains(Borders.TOP) ? 0 : 1);
        int rightInset = area.right() - 1 - (isReplace || !borders.contains(Borders.RIGHT) ? 0 : 1);
        int bottomInset = area.bottom() - 1 - (isReplace || !borders.contains(Borders.BOTTOM) ? 0 : 1);

        // Top border (skip corners if not REPLACE, skip if character is empty)
        String topChar = set.topHorizontal();
        if (borders.contains(Borders.TOP) && area.height() > 0 && !topChar.isEmpty()) {
            for (int x = leftInset; x <= rightInset; x++) {
                setBorderCell(buffer, x, area.top(), topChar, borderStyle);
            }
        }

        // Bottom border (skip corners if not REPLACE, skip if character is empty)
        String bottomChar = set.bottomHorizontal();
        if (borders.contains(Borders.BOTTOM) && area.height() > 1 && !bottomChar.isEmpty()) {
            for (int x = leftInset; x <= rightInset; x++) {
                setBorderCell(buffer, x, area.bottom() - 1, bottomChar, borderStyle);
            }
        }

        // Left border (skip corners if not REPLACE, skip if character is empty)
        String leftChar = set.leftVertical();
        if (borders.contains(Borders.LEFT) && area.width() > 0 && !leftChar.isEmpty()) {
            for (int y = topInset; y <= bottomInset; y++) {
                setBorderCell(buffer, area.left(), y, leftChar, borderStyle);
            }
        }

        // Right border (skip corners if not REPLACE, skip if character is empty)
        String rightChar = set.rightVertical();
        if (borders.contains(Borders.RIGHT) && area.width() > 1 && !rightChar.isEmpty()) {
            for (int y = topInset; y <= bottomInset; y++) {
                setBorderCell(buffer, area.right() - 1, y, rightChar, borderStyle);
            }
        }

        // Corners - with customBorderSet, render if character is not empty (allows corners-only).
        // Without customBorderSet, require both adjacent sides to be enabled (original behavior).
        boolean hasTop = borders.contains(Borders.TOP);
        boolean hasBottom = borders.contains(Borders.BOTTOM);
        boolean hasLeft = borders.contains(Borders.LEFT);
        boolean hasRight = borders.contains(Borders.RIGHT);
        boolean hasCustomSet = customBorderSet != null;

        String topLeftChar = set.topLeft();
        if (!topLeftChar.isEmpty() && (hasCustomSet || (hasTop && hasLeft))) {
            setBorderCell(buffer, area.left(), area.top(), topLeftChar, borderStyle);
        }
        String topRightChar = set.topRight();
        if (!topRightChar.isEmpty() && area.width() > 1 && (hasCustomSet || (hasTop && hasRight))) {
            setBorderCell(buffer, area.right() - 1, area.top(), topRightChar, borderStyle);
        }
        String bottomLeftChar = set.bottomLeft();
        if (!bottomLeftChar.isEmpty() && area.height() > 1 && (hasCustomSet || (hasBottom && hasLeft))) {
            setBorderCell(buffer, area.left(), area.bottom() - 1, bottomLeftChar, borderStyle);
        }
        String bottomRightChar = set.bottomRight();
        if (!bottomRightChar.isEmpty() && area.width() > 1 && area.height() > 1 && (hasCustomSet || (hasBottom && hasRight))) {
            setBorderCell(buffer, area.right() - 1, area.bottom() - 1, bottomRightChar, borderStyle);
        }
    }

    private void setBorderCell(Buffer buffer, int x, int y, String symbol, Style borderStyle) {
        Cell existing = buffer.get(x, y);
        // For QUADRANT_OUTSIDE borders, the half-block characters need:
        // - fg = content color (filled part)
        // - bg = terminal default (empty part to blend with terminal background)
        // So we should NOT preserve existing background for these borders.
        // For regular borders, preserving background allows content background to show through.
        Style mergedStyle;
        if (borderType == BorderType.QUADRANT_OUTSIDE) {
            // Don't preserve background - let border style control both fg and bg
            mergedStyle = borderStyle;
        } else {
            // Preserve only the background color from existing style, not text modifiers like italic.
            // Text modifiers should only apply to text content, not border characters.
            Style baseStyle = existing.style().bg()
                    .map(bg -> Style.EMPTY.bg(bg))
                    .orElse(Style.EMPTY);
            mergedStyle = baseStyle.patch(borderStyle);
        }

        if (mergeStrategy != MergeStrategy.REPLACE) {
            String existingSymbol = existing.symbol();
            boolean existingIsText = !MergeStrategy.isBorderSymbol(existingSymbol);
            boolean newIsBorder = MergeStrategy.isBorderSymbol(symbol);

            // If existing cell is empty (space), just set the new cell
            if (" ".equals(existingSymbol)) {
                buffer.set(x, y, new Cell(symbol, mergedStyle));
                return;
            }

            // Check if existing cell contains non-border text (like titles)
            // If we're trying to draw a border over non-border text, preserve the text but update style
            if (existingIsText && newIsBorder) {
                // Preserve existing non-border text (like titles) when drawing borders
                // But update the style to match the new cell's style
                buffer.set(x, y, new Cell(existing.symbol(), mergedStyle));
                return;
            }

            // Both are borders (or existing is a border) - merge the symbols
            Cell merged = existing.mergeSymbol(symbol, mergeStrategy);
            // Use the merged style (preserves background, applies border foreground)
            buffer.set(x, y, new Cell(merged.symbol(), mergedStyle));
        } else {
            // REPLACE strategy - set the cell with merged style
            buffer.set(x, y, new Cell(symbol, mergedStyle));
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

        // Apply block style (for background) then borderStyle (for foreground)
        // This ensures title inherits background from block and foreground from border
        if (!style.equals(Style.EMPTY)) {
            titleLine = titleLine.patchStyle(style);
        }
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

        if (maxWidth <= CharWidth.of(ELLIPSIS)) {
            // Not enough room for ellipsis, just clip
            return Line.from(new Span(CharWidth.substringByWidth(fullText, maxWidth), lineStyle));
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
        int availableWidth = maxWidth - CharWidth.of(ELLIPSIS);
        return CharWidth.substringByWidth(text, availableWidth) + ELLIPSIS;
    }

    private String truncateStart(String text, int maxWidth) {
        int availableWidth = maxWidth - CharWidth.of(ELLIPSIS);
        return ELLIPSIS + CharWidth.substringByWidthFromEnd(text, availableWidth);
    }

    private String truncateMiddle(String text, int maxWidth) {
        int availableWidth = maxWidth - CharWidth.of(ELLIPSIS);
        int leftWidth = (availableWidth + 1) / 2;
        int rightWidth = availableWidth / 2;
        return CharWidth.substringByWidth(text, leftWidth) + ELLIPSIS + CharWidth.substringByWidthFromEnd(text, rightWidth);
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

    /**
     * Builder for {@link Block}.
     */
    public static final class Builder {
        private Title title;
        private Title titleBottom;
        private EnumSet<Borders> borders = Borders.NONE;
        private BorderSet customBorderSet;
        private Style borderStyle = Style.EMPTY;
        private Style style = Style.EMPTY;
        private Padding padding = Padding.NONE;
        private MergeStrategy mergeStrategy = MergeStrategy.REPLACE;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties (resolved via styleResolver in build())
        private BorderType borderType;
        private Color borderColor;
        private Color background;
        private Color foreground;

        private Builder() {
        }

        /**
         * Sets the top title from a string.
         *
         * @param title the title text
         * @return this builder
         */
        public Builder title(String title) {
            this.title = Title.from(title);
            return this;
        }

        /**
         * Sets the top title.
         *
         * @param title the title
         * @return this builder
         */
        public Builder title(Title title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the bottom title from a string.
         *
         * @param title the title text
         * @return this builder
         */
        public Builder titleBottom(String title) {
            this.titleBottom = Title.from(title);
            return this;
        }

        /**
         * Sets the bottom title.
         *
         * @param title the title
         * @return this builder
         */
        public Builder titleBottom(Title title) {
            this.titleBottom = title;
            return this;
        }

        /**
         * Sets the borders to draw.
         *
         * @param borders the borders to draw
         * @return this builder
         */
        public Builder borders(EnumSet<Borders> borders) {
            this.borders = EnumSet.copyOf(borders);
            return this;
        }

        /**
         * Sets the border type.
         *
         * @param borderType the border type
         * @return this builder
         */
        public Builder borderType(BorderType borderType) {
            this.borderType = borderType;
            return this;
        }

        /**
         * Sets a custom border set with specific characters for each border element.
         * <p>
         * When set, this overrides the characters from {@link #borderType(BorderType)}.
         * Characters can be empty strings ({@code ""}) to skip rendering that element.
         * <p>
         * Example for corners-only rendering:
         * <pre>{@code
         * customBorderSet(new BorderSet("", "", "", "", "┌", "┐", "└", "┘"))
         * }</pre>
         *
         * @param borderSet the custom border set, or null to use borderType
         * @return this builder
         */
        public Builder customBorderSet(BorderSet borderSet) {
            this.customBorderSet = borderSet;
            return this;
        }

        /**
         * Sets the border color.
         *
         * @param color the border color
         * @return this builder
         */
        public Builder borderColor(Color color) {
            this.borderColor = color;
            return this;
        }

        /**
         * Sets the background color.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.background = color;
            return this;
        }

        /**
         * Sets the foreground (text) color.
         *
         * @param color the foreground color
         * @return this builder
         */
        public Builder foreground(Color color) {
            this.foreground = color;
            return this;
        }

        /**
         * Sets the border style.
         *
         * @param borderStyle the border style
         * @return this builder
         */
        public Builder borderStyle(Style borderStyle) {
            this.borderStyle = borderStyle;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code border-type} and {@code border-color}
         * will fall back to resolved values if not set programmatically.
         *
         * @param resolver the property resolver
         * @return this builder
         */
        public Builder styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
            return this;
        }

        /**
         * Sets the block style.
         *
         * @param style the block style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the block padding.
         *
         * @param padding the padding
         * @return this builder
         */
        public Builder padding(Padding padding) {
            this.padding = padding;
            return this;
        }

        /**
         * Sets uniform padding on all sides.
         *
         * @param value the padding value
         * @return this builder
         */
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

        /**
         * Builds the block.
         *
         * @return the built block
         */
        public Block build() {
            return new Block(this);
        }

        // Resolution helpers
        private BorderType resolveBorderType() {
            return styleResolver.resolve(BORDER_TYPE, borderType);
        }

        private Color resolveBorderColor() {
            return styleResolver.resolve(BORDER_COLOR, borderColor);
        }

        private Color resolveBackground() {
            return styleResolver.resolve(StandardProperties.BACKGROUND, background);
        }

        private Color resolveForeground() {
            return styleResolver.resolve(StandardProperties.COLOR, foreground);
        }
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardPropertyKeys;
import dev.tamboui.style.Style;
import dev.tamboui.style.StyledProperty;
import dev.tamboui.style.Width;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.style.PropertyKey;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.text.Overflow;
import dev.tamboui.widgets.text.OverflowConverter;

/**
 * A list widget for displaying selectable items.
 */
public final class ListWidget implements StatefulWidget<ListState> {

    /**
     * Property key for text-overflow property.
     */
    public static final PropertyKey<Overflow> TEXT_OVERFLOW =
            PropertyKey.of("text-overflow", OverflowConverter.INSTANCE);

    private static final String ELLIPSIS = "...";

    private final List<ListItem> items;
    private final Block block;
    private final Style style;
    private final Style highlightStyle;
    private final Line highlightSymbol;
    private final ListDirection direction;
    private final boolean repeatHighlightSymbol;
    private final Overflow overflow;

    private ListWidget(Builder builder) {
        this.block = builder.block;
        this.highlightSymbol = builder.highlightSymbol;
        this.direction = builder.direction;
        this.repeatHighlightSymbol = builder.repeatHighlightSymbol;
        this.highlightStyle = builder.highlightStyle;
        this.overflow = builder.overflow.resolve();

        Color resolvedBg = builder.background.resolve();
        Color resolvedFg = builder.foreground.resolve();
        Style baseStyle = builder.style;
        if (resolvedBg != null) {
            baseStyle = baseStyle.bg(resolvedBg);
        }
        if (resolvedFg != null) {
            baseStyle = baseStyle.fg(resolvedFg);
        }
        this.style = baseStyle;

        // Apply item style resolver if provided
        if (builder.itemStyleResolver != null) {
            List<ListItem> styledItems = new ArrayList<>(builder.items.size());
            int total = builder.items.size();
            for (int i = 0; i < total; i++) {
                ListItem item = builder.items.get(i);
                Style itemStyle = builder.itemStyleResolver.apply(i, total);
                if (itemStyle != null && !itemStyle.equals(Style.EMPTY)) {
                    styledItems.add(item.style(item.style().patch(itemStyle)));
                } else {
                    styledItems.add(item);
                }
            }
            this.items = listCopyOf(styledItems);
        } else {
            this.items = listCopyOf(builder.items);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ListItem> items() {
        return items;
    }

    @Override
    public void render(Rect area, Buffer buffer, ListState state) {
        if (area.isEmpty()) {
            return;
        }

        // Apply background style
        buffer.setStyle(area, style);

        // Render block if present
        Rect listArea = area;
        if (block != null) {
            block.render(area, buffer);
            listArea = block.inner(area);
        }

        if (listArea.isEmpty() || items.isEmpty()) {
            return;
        }

        // Ensure selected item is visible
        if (state.selected() != null) {
            state.scrollToSelected(listArea.height(), items);
        }

        int symbolWidth = highlightSymbol.width();
        // Always reserve space for symbol to keep content aligned
        int contentWidth = listArea.width() - symbolWidth;
        if (contentWidth <= 0) {
            return;
        }

        // Items stay in original order - direction only affects rendering position
        List<ListItem> orderedItems = items;

        // Selected index stays the same - no adjustment needed
        Integer selectedIndex = state.selected();

        if (direction == ListDirection.BOTTOM_TO_TOP) {
            // Render from bottom to top
            int y = listArea.bottom() - 1;
            int offset = state.offset();
            int totalHeight = orderedItems.stream().mapToInt(ListItem::height).sum();
            int currentOffset = 0;

            for (int i = 0; i < orderedItems.size() && y >= listArea.top(); i++) {
                ListItem item = orderedItems.get(i);
                int itemHeight = item.height();

                // Skip items after the visible area (from bottom)
                int itemBottomOffset = totalHeight - currentOffset - itemHeight;
                if (itemBottomOffset + itemHeight <= offset) {
                    currentOffset += itemHeight;
                    continue;
                }

                // Calculate visible portion of this item
                int itemStartOffset = itemBottomOffset;
                int startLine = Math.max(0, offset - itemStartOffset);

                boolean isSelected = selectedIndex != null && selectedIndex == i;
                Style itemStyle = style.patch(item.style()).patch(isSelected ? highlightStyle : Style.EMPTY);

                // Draw item content from bottom to top
                List<Line> lines = item.content().lines();
                int itemY = y;

                for (int lineIdx = itemHeight - 1; lineIdx >= startLine && itemY >= listArea.top(); lineIdx--) {
                    if (lineIdx >= lines.size()) {
                        itemY--;
                        continue;
                    }

                    // For bottom-to-top, we render from bottom to top, so:
                    // - First line visually (top) is at lineIdx = 0 (rendered last)
                    // - Last line visually (bottom) is at lineIdx = itemHeight - 1 (rendered first)
                    // Always reserve space for symbol on all lines to keep content aligned
                    int contentX = listArea.left() + symbolWidth;
                    int availableWidth = contentWidth;

                    // Fill the content area background based on width property (default: fill)
                    // Only fill the content area, not the symbol area, so symbol keeps its own style
                    Width widthBtt = itemStyle.extension(Width.class, Width.FILL);
                    if (widthBtt.isFill()) {
                        Rect rowArea = new Rect(contentX, itemY, contentWidth, 1);
                        buffer.setStyle(rowArea, itemStyle);
                    }

                    // Draw highlight symbol on each line if selected
                    // If repeatHighlightSymbol is true, show on all lines; otherwise only on first line (top visually)
                    boolean shouldShowSymbol = isSelected && symbolWidth > 0
                        && (lineIdx == 0 || repeatHighlightSymbol);
                    if (shouldShowSymbol) {
                        buffer.setLine(listArea.left(), itemY, highlightSymbol);
                    }

                    Line line = lines.get(lineIdx).patchStyle(itemStyle);

                    // Process line according to overflow mode
                    Line processedLine = processLine(line, availableWidth);
                    buffer.setLine(contentX, itemY, processedLine);
                    itemY--;
                }

                y = itemY;
                currentOffset += itemHeight;
            }
        } else {
            // Render from top to bottom (default)
            int y = listArea.top();
            int offset = state.offset();
            int currentOffset = 0;

            for (int i = 0; i < orderedItems.size() && y < listArea.bottom(); i++) {
                ListItem item = orderedItems.get(i);
                int itemHeight = item.height();

                // Skip items before the visible area
                if (currentOffset + itemHeight <= offset) {
                    currentOffset += itemHeight;
                    continue;
                }

                // Calculate visible portion of this item
                int startLine = Math.max(0, offset - currentOffset);
                int visibleHeight = Math.min(itemHeight - startLine, listArea.bottom() - y);

                boolean isSelected = selectedIndex != null && selectedIndex == i;
                Style itemStyle = style.patch(item.style()).patch(isSelected ? highlightStyle : Style.EMPTY);

                // Draw item content
                List<Line> lines = item.content().lines();

                for (int lineIdx = startLine; lineIdx < startLine + visibleHeight && lineIdx < lines.size(); lineIdx++) {
                    // Always reserve space for symbol on all lines to keep content aligned
                    int contentX = listArea.left() + symbolWidth;
                    int availableWidth = contentWidth;

                    // Fill the content area background based on width property (default: fill)
                    // Only fill the content area, not the symbol area, so symbol keeps its own style
                    Width width = itemStyle.extension(Width.class, Width.FILL);
                    if (width.isFill()) {
                        Rect rowArea = new Rect(contentX, y, contentWidth, 1);
                        buffer.setStyle(rowArea, itemStyle);
                    }

                    // Draw highlight symbol on each line if selected
                    // If repeatHighlightSymbol is true, show on all lines; otherwise only on first line
                    boolean shouldShowSymbol = isSelected && symbolWidth > 0
                        && (lineIdx == startLine || repeatHighlightSymbol);
                    if (shouldShowSymbol) {
                        buffer.setLine(listArea.left(), y, highlightSymbol);
                    }

                    Line line = lines.get(lineIdx).patchStyle(itemStyle);

                    // Process line according to overflow mode
                    Line processedLine = processLine(line, availableWidth);
                    buffer.setLine(contentX, y, processedLine);
                    y++;
                }

                currentOffset += itemHeight;
            }
        }
    }

    /**
     * Processes a line according to the overflow mode.
     */
    private Line processLine(Line line, int maxWidth) {
        if (line.width() <= maxWidth) {
            return line;
        }

        switch (overflow) {
            case CLIP:
                return clipLine(line, maxWidth);
            case ELLIPSIS:
                return truncateWithEllipsis(line, maxWidth, EllipsisPosition.END);
            case ELLIPSIS_START:
                return truncateWithEllipsis(line, maxWidth, EllipsisPosition.START);
            case ELLIPSIS_MIDDLE:
                return truncateWithEllipsis(line, maxWidth, EllipsisPosition.MIDDLE);
            case WRAP_CHARACTER:
            case WRAP_WORD:
                // Wrapping not supported for list items - fall back to clip
                return clipLine(line, maxWidth);
            default:
                return clipLine(line, maxWidth);
        }
    }

    private Line clipLine(Line line, int maxWidth) {
        List<Span> clippedSpans = new ArrayList<>();
        int remainingWidth = maxWidth;

        for (Span span : line.spans()) {
            if (remainingWidth <= 0) {
                break;
            }

            String content = span.content();
            if (content.length() <= remainingWidth) {
                clippedSpans.add(span);
                remainingWidth -= content.length();
            } else {
                clippedSpans.add(new Span(content.substring(0, remainingWidth), span.style()));
                break;
            }
        }

        return Line.from(clippedSpans);
    }

    private enum EllipsisPosition { START, MIDDLE, END }

    private Line truncateWithEllipsis(Line line, int maxWidth, EllipsisPosition position) {
        if (maxWidth <= ELLIPSIS.length()) {
            // Not enough room for ellipsis, just clip
            return clipLine(line, maxWidth);
        }

        String fullText = lineToString(line);
        Style lineStyle = getLineStyle(line);

        String truncated;
        switch (position) {
            case END:
                truncated = truncateEnd(fullText, maxWidth);
                break;
            case START:
                truncated = truncateStart(fullText, maxWidth);
                break;
            case MIDDLE:
                truncated = truncateMiddle(fullText, maxWidth);
                break;
            default:
                truncated = truncateEnd(fullText, maxWidth);
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

    public static final class Builder {
        private List<ListItem> items = new ArrayList<>();
        private Block block;
        private Style style = Style.EMPTY;
        private Style highlightStyle = Style.EMPTY.reversed();
        private Line highlightSymbol = Line.from(">> ");
        private ListDirection direction = ListDirection.TOP_TO_BOTTOM;
        private boolean repeatHighlightSymbol = false;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();
        private BiFunction<Integer, Integer, Style> itemStyleResolver;

        // Style-aware properties bound to this builder's resolver
        private final StyledProperty<Overflow> overflow =
                StyledProperty.of(TEXT_OVERFLOW, Overflow.CLIP, () -> styleResolver);
        private final StyledProperty<Color> background =
                StyledProperty.of(StandardPropertyKeys.BACKGROUND, null, () -> styleResolver);
        private final StyledProperty<Color> foreground =
                StyledProperty.of(StandardPropertyKeys.COLOR, null, () -> styleResolver);

        private Builder() {}

        public Builder items(List<ListItem> items) {
            this.items = new ArrayList<>(items);
            return this;
        }

        public Builder items(ListItem... items) {
            this.items = new ArrayList<>(Arrays.asList(items));
            return this;
        }

        public Builder addItem(ListItem item) {
            this.items.add(item);
            return this;
        }

        public Builder addItem(String text) {
            this.items.add(ListItem.from(text));
            return this;
        }

        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder highlightStyle(Style highlightStyle) {
            this.highlightStyle = highlightStyle;
            return this;
        }

        /**
         * Sets a function to resolve styles for each item based on position.
         * <p>
         * The function receives the item index (0-based) and total item count,
         * and returns a Style to apply to that item. This enables positional
         * styling like alternating row colors.
         *
         * @param resolver function that takes (index, totalCount) and returns a Style
         * @return this builder
         */
        public Builder itemStyleResolver(BiFunction<Integer, Integer, Style> resolver) {
            this.itemStyleResolver = resolver;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code color} and {@code background}
         * will be resolved if not set programmatically.
         *
         * @param resolver the property resolver
         * @return this builder
         */
        public Builder styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
            return this;
        }

        /**
         * Sets the background color for items.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.background.set(color);
            return this;
        }

        /**
         * Sets the foreground (text) color for items.
         *
         * @param color the foreground color
         * @return this builder
         */
        public Builder foreground(Color color) {
            this.foreground.set(color);
            return this;
        }

        /**
         * Sets the highlight symbol as a Line (which can include styling).
         *
         * @param highlightSymbol the line to use as the highlight symbol
         * @return this builder
         */
        public Builder highlightSymbol(Line highlightSymbol) {
            this.highlightSymbol = highlightSymbol;
            return this;
        }

        /**
         * Sets the highlight symbol as a string (convenience method).
         *
         * @param highlightSymbol the string to use as the highlight symbol
         * @return this builder
         */
        public Builder highlightSymbol(String highlightSymbol) {
            this.highlightSymbol = Line.from(highlightSymbol);
            return this;
        }

        /**
         * Sets the direction for rendering list items.
         *
         * @param direction the direction (default: TOP_TO_BOTTOM)
         */
        public Builder direction(ListDirection direction) {
            this.direction = direction != null ? direction : ListDirection.TOP_TO_BOTTOM;
            return this;
        }

        /**
         * Sets whether to repeat the highlight symbol on all lines of multiline items.
         *
         * @param repeatHighlightSymbol if true, repeat symbol on all lines (default: false)
         */
        public Builder repeatHighlightSymbol(boolean repeatHighlightSymbol) {
            this.repeatHighlightSymbol = repeatHighlightSymbol;
            return this;
        }

        /**
         * Sets the overflow mode for list item text.
         * <p>
         * Controls how text is handled when it exceeds the available width:
         * <ul>
         *   <li>{@code CLIP} - silently truncate at boundary (default)</li>
         *   <li>{@code ELLIPSIS} - truncate with "..." at end</li>
         *   <li>{@code ELLIPSIS_START} - truncate with "..." at start</li>
         *   <li>{@code ELLIPSIS_MIDDLE} - truncate with "..." in middle</li>
         * </ul>
         * Note: WRAP_CHARACTER and WRAP_WORD are not supported for list items.
         *
         * @param overflow the overflow mode
         * @return this builder
         */
        public Builder overflow(Overflow overflow) {
            this.overflow.set(overflow);
            return this;
        }

        public ListWidget build() {
            return new ListWidget(this);
        }
    }
}

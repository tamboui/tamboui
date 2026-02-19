/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.common.ScrollBarPolicy;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

/**
 * A list widget for displaying selectable items using {@link SizedWidget}.
 * <p>
 * Unlike the simple text-based list, this widget accepts arbitrary widgets
 * with optional size hints, enabling rich content such as styled elements,
 * progress bars, or complex layouts as list items.
 * <p>
 * Supports multiple scroll modes via {@link ScrollMode}, optional scrollbar
 * via {@link ScrollBarPolicy}, and configurable highlight styles.
 *
 * <pre>{@code
 * ListWidget widget = ListWidget.builder()
 *     .items(items)
 *     .highlightSymbol("> ")
 *     .highlightStyle(Style.EMPTY.reversed())
 *     .scrollMode(ScrollMode.AUTO_SCROLL)
 *     .scrollBarPolicy(ScrollBarPolicy.AS_NEEDED)
 *     .build();
 *
 * widget.render(area, buffer, state);
 * }</pre>
 */
public final class ListWidget implements StatefulWidget<ListState> {

    private final List<SizedWidget> items;
    private final Style style;
    private final Style highlightStyle;
    private final Line highlightSymbol;
    private final Block block;
    private final ScrollMode scrollMode;
    private final ScrollBarPolicy scrollBarPolicy;
    private final Style scrollbarThumbStyle;
    private final Style scrollbarTrackStyle;
    private final BiFunction<Integer, Integer, Style> itemStyleResolver;

    private ListWidget(Builder builder) {
        this.items = listCopyOf(builder.items);
        this.style = builder.style;
        this.highlightStyle = builder.highlightStyle;
        this.highlightSymbol = builder.highlightSymbol;
        this.block = builder.block;
        this.scrollMode = builder.scrollMode;
        this.scrollBarPolicy = builder.scrollBarPolicy;
        this.scrollbarThumbStyle = builder.scrollbarThumbStyle;
        this.scrollbarTrackStyle = builder.scrollbarTrackStyle;
        this.itemStyleResolver = builder.itemStyleResolver;
    }

    /**
     * Creates a new builder for ListWidget.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
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

        if (listArea.isEmpty()) {
            return;
        }

        int totalItems = items.size();
        if (totalItems == 0) {
            return;
        }

        // Clamp selection
        if (state.selected() != null) {
            int sel = Math.max(0, Math.min(state.selected(), totalItems - 1));
            state.select(sel);
        }

        int visibleHeight = listArea.height();
        int symbolWidth = highlightSymbol.width();

        // Determine if we should reserve space for scrollbar (heuristic)
        boolean reserveScrollbarSpace = scrollBarPolicy == ScrollBarPolicy.ALWAYS
                || (scrollBarPolicy == ScrollBarPolicy.AS_NEEDED && totalItems > visibleHeight);

        // Calculate content area
        int contentX = listArea.left() + symbolWidth;
        int contentWidth = listArea.width() - symbolWidth;
        if (reserveScrollbarSpace) {
            contentWidth -= 1;
        }

        if (contentWidth <= 0) {
            return;
        }

        // Calculate item heights
        int[] itemHeights = new int[totalItems];
        int totalHeight = 0;
        for (int i = 0; i < totalItems; i++) {
            itemHeights[i] = items.get(i).heightOr(1);
            totalHeight += itemHeights[i];
        }

        int maxScroll = Math.max(0, totalHeight - visibleHeight);

        // Final determination of whether to show scrollbar
        boolean showScrollbar = scrollBarPolicy == ScrollBarPolicy.ALWAYS
                || (scrollBarPolicy == ScrollBarPolicy.AS_NEEDED && totalHeight > visibleHeight);

        // Apply scroll mode logic
        switch (scrollMode) {
            case STICKY_SCROLL:
                state.applyStickyScroll(totalItems, totalHeight, visibleHeight);
                break;
            case SCROLL_TO_END:
                state.applyScrollToEnd(totalHeight, visibleHeight);
                break;
            case AUTO_SCROLL:
                state.scrollToSelected(visibleHeight, itemHeights);
                break;
            default:
                break;
        }

        // Clamp offset
        int scrollOffset = Math.max(0, Math.min(state.offset(), maxScroll));
        state.setOffset(scrollOffset);

        // Render visible items
        int y = listArea.top();
        int currentOffset = 0;

        for (int i = 0; i < totalItems && y < listArea.bottom(); i++) {
            int itemHeight = itemHeights[i];

            // Skip items before the visible area
            if (currentOffset + itemHeight <= scrollOffset) {
                currentOffset += itemHeight;
                continue;
            }

            // Calculate visible portion of this item
            int startLine = Math.max(0, scrollOffset - currentOffset);
            int visibleItemHeight = Math.min(itemHeight - startLine, listArea.bottom() - y);

            boolean isSelected = state.selected() != null && state.selected() == i;

            // Get item positional style (for zebra striping etc.)
            Style posStyle = Style.EMPTY;
            if (itemStyleResolver != null) {
                posStyle = itemStyleResolver.apply(i, totalItems);
                if (posStyle == null) {
                    posStyle = Style.EMPTY;
                }
            }

            // Draw highlight symbol for selected item
            if (isSelected && symbolWidth > 0) {
                buffer.setLine(listArea.left(), y, highlightSymbol.patchStyle(highlightStyle));
            }

            // Render the item widget
            Rect itemArea = new Rect(contentX, y, contentWidth, visibleItemHeight);
            items.get(i).widget().render(itemArea, buffer);

            // Apply row style overlays AFTER widget renders
            boolean hasPosStyleBg = posStyle.bg().isPresent();
            if (hasPosStyleBg || isSelected) {
                for (int row = 0; row < visibleItemHeight && y + row < listArea.bottom(); row++) {
                    Rect rowArea = new Rect(listArea.left(), y + row, listArea.width() - (showScrollbar ? 1 : 0), 1);
                    if (hasPosStyleBg) {
                        buffer.setStyle(rowArea, Style.EMPTY.bg(posStyle.bg().get()));
                    }
                    if (isSelected) {
                        buffer.setStyle(rowArea, highlightStyle);
                    }
                }
            }

            y += visibleItemHeight;
            currentOffset += itemHeight;
        }

        // Render scrollbar if enabled
        if (showScrollbar) {
            Rect scrollbarArea = new Rect(
                listArea.right() - 1,
                listArea.top(),
                1,
                listArea.height()
            );

            ScrollbarState scrollbarState = new ScrollbarState()
                .contentLength(totalHeight)
                .viewportContentLength(visibleHeight)
                .position(scrollOffset);

            Scrollbar.Builder scrollbarBuilder = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_RIGHT);
            if (scrollbarThumbStyle != null && !scrollbarThumbStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.thumbStyle(scrollbarThumbStyle);
            }
            if (scrollbarTrackStyle != null && !scrollbarTrackStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.trackStyle(scrollbarTrackStyle);
            }

            scrollbarBuilder.build().render(scrollbarArea, buffer, scrollbarState);
        }
    }

    /**
     * Builder for {@link ListWidget}.
     */
    public static final class Builder {
        private List<SizedWidget> items = new ArrayList<>();
        private Style style = Style.EMPTY;
        private Style highlightStyle = Style.EMPTY.reversed();
        private Line highlightSymbol = Line.from("> ");
        private Block block;
        private ScrollMode scrollMode = ScrollMode.NONE;
        private ScrollBarPolicy scrollBarPolicy = ScrollBarPolicy.NONE;
        private Style scrollbarThumbStyle;
        private Style scrollbarTrackStyle;
        private BiFunction<Integer, Integer, Style> itemStyleResolver;

        private Builder() {
        }

        /**
         * Sets the list items.
         *
         * @param items the SizedWidget items to display
         * @return this builder
         */
        public Builder items(List<SizedWidget> items) {
            this.items = new ArrayList<>(items);
            return this;
        }

        /**
         * Sets the list items from {@link ListItem} instances.
         *
         * @param items the list items
         * @return this builder
         */
        public Builder items(ListItem... items) {
            this.items = new ArrayList<>(items.length);
            for (ListItem item : items) {
                this.items.add(item.toSizedWidget());
            }
            return this;
        }

        /**
         * Sets the list items from strings.
         *
         * @param items the item texts
         * @return this builder
         */
        public Builder items(String... items) {
            this.items = new ArrayList<>(items.length);
            for (String item : items) {
                this.items.add(ListItem.from(item).toSizedWidget());
            }
            return this;
        }

        /**
         * Adds an item to the list.
         *
         * @param item the SizedWidget item to add
         * @return this builder
         */
        public Builder addItem(SizedWidget item) {
            this.items.add(item);
            return this;
        }

        /**
         * Sets the base style.
         *
         * @param style the base style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style != null ? style : Style.EMPTY;
            return this;
        }

        /**
         * Sets the highlight style for selected items.
         *
         * @param style the highlight style
         * @return this builder
         */
        public Builder highlightStyle(Style style) {
            this.highlightStyle = style != null ? style : Style.EMPTY.reversed();
            return this;
        }

        /**
         * Sets the highlight symbol shown before selected items.
         *
         * @param symbol the highlight symbol
         * @return this builder
         */
        public Builder highlightSymbol(Line symbol) {
            this.highlightSymbol = symbol != null ? symbol : Line.from("");
            return this;
        }

        /**
         * Sets the highlight symbol as a string.
         *
         * @param symbol the highlight symbol
         * @return this builder
         */
        public Builder highlightSymbol(String symbol) {
            this.highlightSymbol = symbol != null ? Line.from(symbol) : Line.from("");
            return this;
        }

        /**
         * Wraps the list in a block.
         *
         * @param block the block
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the scroll mode.
         *
         * @param mode the scroll mode
         * @return this builder
         */
        public Builder scrollMode(ScrollMode mode) {
            this.scrollMode = mode != null ? mode : ScrollMode.NONE;
            return this;
        }

        /**
         * Sets the scrollbar policy.
         *
         * @param policy the scrollbar display policy
         * @return this builder
         */
        public Builder scrollBarPolicy(ScrollBarPolicy policy) {
            this.scrollBarPolicy = policy != null ? policy : ScrollBarPolicy.NONE;
            return this;
        }

        /**
         * Sets the scrollbar thumb style.
         *
         * @param style the thumb style
         * @return this builder
         */
        public Builder scrollbarThumbStyle(Style style) {
            this.scrollbarThumbStyle = style;
            return this;
        }

        /**
         * Sets the scrollbar track style.
         *
         * @param style the track style
         * @return this builder
         */
        public Builder scrollbarTrackStyle(Style style) {
            this.scrollbarTrackStyle = style;
            return this;
        }

        /**
         * Sets a function to resolve styles for each item based on position.
         * <p>
         * The function receives the item index (0-based) and total item count,
         * and returns a Style to apply to that item.
         *
         * @param resolver function that takes (index, totalCount) and returns a Style
         * @return this builder
         */
        public Builder itemStyleResolver(BiFunction<Integer, Integer, Style> resolver) {
            this.itemStyleResolver = resolver;
            return this;
        }

        /**
         * Builds the ListWidget.
         *
         * @return a new ListWidget
         */
        public ListWidget build() {
            return new ListWidget(this);
        }
    }
}

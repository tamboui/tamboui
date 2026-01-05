/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.toolkit.element.ChildPosition;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * A container element that displays a selectable list of items.
 * <p>
 * This is a DSL wrapper for the ListWidget.
 * <pre>{@code
 * list("Item 1", "Item 2", "Item 3")
 *     .state(listState)
 *     .highlightColor(Color.YELLOW)
 *     .title("My List")
 *     .rounded()
 * }</pre>
 * <p>
 * CSS selectors:
 * <ul>
 *   <li>{@code ListContainer} - styles the container (border, background)</li>
 *   <li>{@code ListContainer-item} - styles each list item</li>
 *   <li>{@code ListContainer-item:selected} - styles the selected item</li>
 *   <li>{@code ListContainer-item:nth-child(odd/even)} - zebra striping</li>
 *   <li>{@code ListContainer-scrollbar-thumb} - styles the scrollbar thumb</li>
 *   <li>{@code ListContainer-scrollbar-track} - styles the scrollbar track</li>
 * </ul>
 */
public final class ListContainer<T> extends StyledElement<ListContainer<T>> {

    private static final Style DEFAULT_HIGHLIGHT_STYLE = Style.EMPTY.reversed();
    private static final String DEFAULT_HIGHLIGHT_SYMBOL = "> ";

    private final List<ListItem> items = new ArrayList<>();
    private List<T> data;
    private Function<T, ListItem> itemRenderer;
    private ListState state;
    private Style highlightStyle;  // null means "use CSS or default"
    private String highlightSymbol;  // null means "use CSS or default"
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private boolean autoScroll;
    private boolean showScrollbar;
    private Color scrollbarThumbColor;
    private Color scrollbarTrackColor;

    public ListContainer() {
    }

    public ListContainer(String... items) {
        for (String item : items) {
            this.items.add(ListItem.from(item));
        }
    }

    public ListContainer(List<String> items) {
        for (String item : items) {
            this.items.add(ListItem.from(item));
        }
    }

    /**
     * Sets the list items from strings.
     */
    public ListContainer<T> items(String... items) {
        this.items.clear();
        for (String item : items) {
            this.items.add(ListItem.from(item));
        }
        return this;
    }

    /**
     * Sets the list items from a collection.
     */
    public ListContainer<T> items(List<String> items) {
        this.items.clear();
        for (String item : items) {
            this.items.add(ListItem.from(item));
        }
        return this;
    }

    /**
     * Sets the list items from ListItem objects.
     */
    public ListContainer<T> listItems(ListItem... items) {
        this.items.clear();
        this.items.addAll(Arrays.asList(items));
        return this;
    }

    /**
     * Adds an item to the list.
     */
    public ListContainer<T> add(String item) {
        this.items.add(ListItem.from(item));
        return this;
    }

    /**
     * Adds an item to the list.
     */
    public ListContainer<T> add(ListItem item) {
        this.items.add(item);
        return this;
    }

    /**
     * Sets the list state for selection tracking.
     */
    public ListContainer<T> state(ListState state) {
        this.state = state;
        return this;
    }

    /**
     * Sets the highlight style for selected items.
     */
    public ListContainer<T> highlightStyle(Style style) {
        this.highlightStyle = style;
        return this;
    }

    /**
     * Sets the highlight color for selected items.
     */
    public ListContainer<T> highlightColor(Color color) {
        this.highlightStyle = Style.EMPTY.fg(color).bold();
        return this;
    }

    /**
     * Sets the symbol displayed before the selected item.
     */
    public ListContainer<T> highlightSymbol(String symbol) {
        this.highlightSymbol = symbol;
        return this;
    }

    /**
     * Sets the title.
     */
    public ListContainer<T> title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     */
    public ListContainer<T> rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     */
    public ListContainer<T> borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the data items and a renderer function.
     * <p>
     * The renderer function is called at render time to convert each data item
     * to a ListItem. This allows the list to reflect the current state of your data.
     *
     * @param data the list of data items
     * @param renderer function to convert each item to a ListItem
     * @return this element
     */
    public <U> ListContainer<U> data(List<U> data, Function<U, ListItem> renderer) {
        @SuppressWarnings("unchecked")
        ListContainer<U> self = (ListContainer<U>) this;
        self.data = data;
        self.itemRenderer = renderer;
        self.items.clear();
        return self;
    }

    /**
     * Sets the renderer function for converting data items to ListItems.
     * <p>
     * The renderer is called at render time for each data item.
     *
     * @param renderer function to convert each item to a ListItem
     * @return this element
     */
    public ListContainer<T> itemRenderer(Function<T, ListItem> renderer) {
        this.itemRenderer = renderer;
        return this;
    }

    /**
     * Enables auto-scroll to keep the selected item visible.
     * <p>
     * When enabled, the list automatically scrolls to show the selected item
     * before rendering.
     *
     * @return this element
     */
    public ListContainer<T> autoScroll() {
        this.autoScroll = true;
        return this;
    }

    /**
     * Sets whether auto-scroll is enabled.
     *
     * @param enabled true to enable auto-scroll
     * @return this element
     */
    public ListContainer<T> autoScroll(boolean enabled) {
        this.autoScroll = enabled;
        return this;
    }

    /**
     * Enables showing a scrollbar on the right side of the list.
     *
     * @return this element
     */
    public ListContainer<T> scrollbar() {
        this.showScrollbar = true;
        return this;
    }

    /**
     * Sets whether a scrollbar is shown.
     *
     * @param enabled true to show a scrollbar
     * @return this element
     */
    public ListContainer<T> scrollbar(boolean enabled) {
        this.showScrollbar = enabled;
        return this;
    }

    /**
     * Sets the scrollbar thumb color.
     *
     * @param color the thumb color
     * @return this element
     */
    public ListContainer<T> scrollbarThumbColor(Color color) {
        this.scrollbarThumbColor = color;
        return this;
    }

    /**
     * Sets the scrollbar track color.
     *
     * @param color the track color
     * @return this element
     */
    public ListContainer<T> scrollbarTrackColor(Color color) {
        this.scrollbarTrackColor = color;
        return this;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Build the effective items list
        List<ListItem> effectiveItems;
        if (data != null && itemRenderer != null) {
            // Convert data to ListItems at render time
            effectiveItems = new ArrayList<>(data.size());
            for (T item : data) {
                effectiveItems.add(itemRenderer.apply(item));
            }
        } else {
            effectiveItems = new ArrayList<>(items);
        }

        // Apply CSS positional styles (odd/even, first/last) to each item
        int totalItems = effectiveItems.size();
        for (int i = 0; i < totalItems; i++) {
            ChildPosition pos = ChildPosition.of(i, totalItems);
            Style posStyle = context.childStyle("item", pos);
            // Only apply if CSS defined something beyond the base style
            if (!posStyle.equals(context.currentStyle())) {
                ListItem original = effectiveItems.get(i);
                effectiveItems.set(i, original.style(original.style().patch(posStyle)));
            }
        }

        // Auto-scroll if enabled
        ListState effectiveState = state != null ? state : new ListState();
        if (autoScroll && state != null) {
            // Calculate visible height (area height minus border if present)
            int visibleHeight = area.height();
            if (title != null || borderType != null) {
                visibleHeight -= 2; // Top and bottom border
            }
            state.scrollToSelected(visibleHeight, effectiveItems);
        }

        // Resolve highlight style: explicit > CSS > default
        Style effectiveHighlightStyle = highlightStyle;
        if (effectiveHighlightStyle == null) {
            Style cssStyle = context.childStyle("item", PseudoClassState.ofSelected());
            // Only use CSS style if it differs from base (meaning CSS defined something)
            effectiveHighlightStyle = cssStyle.equals(context.currentStyle())
                ? DEFAULT_HIGHLIGHT_STYLE
                : cssStyle;
        }

        // Resolve highlight symbol: explicit > default
        String effectiveHighlightSymbol = highlightSymbol != null ? highlightSymbol : DEFAULT_HIGHLIGHT_SYMBOL;

        ListWidget.Builder builder = ListWidget.builder()
            .items(effectiveItems)
            .style(context.currentStyle())
            .highlightStyle(effectiveHighlightStyle)
            .highlightSymbol(effectiveHighlightSymbol);

        if (title != null || borderType != null) {
            Block.Builder blockBuilder = Block.builder().borders(Borders.ALL);
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (borderColor != null) {
                blockBuilder.borderStyle(Style.EMPTY.fg(borderColor));
            }
            builder.block(blockBuilder.build());
        }

        ListWidget widget = builder.build();
        frame.renderStatefulWidget(widget, area, effectiveState);

        // Render scrollbar if enabled
        if (showScrollbar && totalItems > 0) {
            // Calculate scrollbar area (inside border, on the right edge)
            Rect scrollbarArea;
            if (title != null || borderType != null) {
                // Adjust for border: 1 from top, 1 from bottom, 1 from right edge
                scrollbarArea = new Rect(
                    area.x() + area.width() - 2,
                    area.y() + 1,
                    1,
                    Math.max(1, area.height() - 2)
                );
            } else {
                // No border: rightmost column
                scrollbarArea = new Rect(
                    area.x() + area.width() - 1,
                    area.y(),
                    1,
                    area.height()
                );
            }

            // Calculate visible height for viewport
            int viewportHeight = scrollbarArea.height();

            // Build scrollbar state from list state
            ScrollbarState scrollbarState = new ScrollbarState()
                .contentLength(totalItems)
                .viewportContentLength(viewportHeight)
                .position(effectiveState.offset());

            // Resolve scrollbar styles: explicit > CSS > default
            Style thumbStyle = null;
            Style trackStyle = null;

            // Check CSS for scrollbar-thumb style
            Style cssThumbStyle = context.childStyle("scrollbar-thumb");
            if (!cssThumbStyle.equals(context.currentStyle())) {
                thumbStyle = cssThumbStyle;
            }
            // Explicit color overrides CSS
            if (scrollbarThumbColor != null) {
                thumbStyle = Style.EMPTY.fg(scrollbarThumbColor);
            }

            // Check CSS for scrollbar-track style
            Style cssTrackStyle = context.childStyle("scrollbar-track");
            if (!cssTrackStyle.equals(context.currentStyle())) {
                trackStyle = cssTrackStyle;
            }
            // Explicit color overrides CSS
            if (scrollbarTrackColor != null) {
                trackStyle = Style.EMPTY.fg(scrollbarTrackColor);
            }

            // Build and render scrollbar
            Scrollbar.Builder scrollbarBuilder = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_RIGHT);

            if (thumbStyle != null) {
                scrollbarBuilder.thumbStyle(thumbStyle);
            }
            if (trackStyle != null) {
                scrollbarBuilder.trackStyle(trackStyle);
            }

            frame.renderStatefulWidget(scrollbarBuilder.build(), scrollbarArea, scrollbarState);
        }
    }
}

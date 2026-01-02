/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * A DSL wrapper for the ListWidget.
 * <p>
 * Displays a selectable list of items.
 * <pre>{@code
 * list("Item 1", "Item 2", "Item 3")
 *     .state(listState)
 *     .highlightColor(Color.YELLOW)
 *     .title("My List")
 *     .rounded()
 * }</pre>
 */
public final class ListElement<T> extends StyledElement<ListElement<T>> {

    private final List<ListItem> items = new ArrayList<>();
    private List<T> data;
    private Function<T, ListItem> itemRenderer;
    private ListState state;
    private Style highlightStyle = Style.EMPTY.reversed();
    private String highlightSymbol = ">> ";
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private boolean autoScroll;

    public ListElement() {
    }

    public ListElement(String... items) {
        for (String item : items) {
            this.items.add(ListItem.from(item));
        }
    }

    public ListElement(List<String> items) {
        for (String item : items) {
            this.items.add(ListItem.from(item));
        }
    }

    /**
     * Sets the list items from strings.
     */
    public ListElement<T> items(String... items) {
        this.items.clear();
        for (String item : items) {
            this.items.add(ListItem.from(item));
        }
        return this;
    }

    /**
     * Sets the list items from a collection.
     */
    public ListElement<T> items(List<String> items) {
        this.items.clear();
        for (String item : items) {
            this.items.add(ListItem.from(item));
        }
        return this;
    }

    /**
     * Sets the list items from ListItem objects.
     */
    public ListElement<T> listItems(ListItem... items) {
        this.items.clear();
        this.items.addAll(Arrays.asList(items));
        return this;
    }

    /**
     * Adds an item to the list.
     */
    public ListElement<T> add(String item) {
        this.items.add(ListItem.from(item));
        return this;
    }

    /**
     * Adds an item to the list.
     */
    public ListElement<T> add(ListItem item) {
        this.items.add(item);
        return this;
    }

    /**
     * Sets the list state for selection tracking.
     */
    public ListElement<T> state(ListState state) {
        this.state = state;
        return this;
    }

    /**
     * Sets the highlight style for selected items.
     */
    public ListElement<T> highlightStyle(Style style) {
        this.highlightStyle = style;
        return this;
    }

    /**
     * Sets the highlight color for selected items.
     */
    public ListElement<T> highlightColor(Color color) {
        this.highlightStyle = Style.EMPTY.fg(color).bold();
        return this;
    }

    /**
     * Sets the symbol displayed before the selected item.
     */
    public ListElement<T> highlightSymbol(String symbol) {
        this.highlightSymbol = symbol;
        return this;
    }

    /**
     * Sets the title.
     */
    public ListElement<T> title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     */
    public ListElement<T> rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     */
    public ListElement<T> borderColor(Color color) {
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
    public <U> ListElement<U> data(List<U> data, Function<U, ListItem> renderer) {
        @SuppressWarnings("unchecked")
        ListElement<U> self = (ListElement<U>) this;
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
    public ListElement<T> itemRenderer(Function<T, ListItem> renderer) {
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
    public ListElement<T> autoScroll() {
        this.autoScroll = true;
        return this;
    }

    /**
     * Sets whether auto-scroll is enabled.
     *
     * @param enabled true to enable auto-scroll
     * @return this element
     */
    public ListElement<T> autoScroll(boolean enabled) {
        this.autoScroll = enabled;
        return this;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
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
            effectiveItems = items;
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

        ListWidget.Builder builder = ListWidget.builder()
            .items(effectiveItems)
            .style(style)
            .highlightStyle(highlightStyle)
            .highlightSymbol(highlightSymbol);

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
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.toolkit.element.ChildPosition;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * A scrollable container that displays a list of selectable items.
 * <p>
 * Unlike {@code ListWidget} (which only displays text), {@code ListElement}
 * can display any {@link StyledElement} as list items, including complex
 * layouts like rows with multiple styled children.
 * <p>
 * Example usage:
 * <pre>{@code
 * list("Item 1", "Item 2", "Item 3")
 *     .state(listState)
 *     .highlightColor(Color.YELLOW)
 *     .title("My List")
 *     .rounded()
 *
 * // With complex items:
 * list(
 *     row(text("Name: ").bold(), text("John").green()),
 *     row(text("Age: ").bold(), text("25").cyan())
 * ).state(listState)
 * }</pre>
 * <p>
 * CSS selectors:
 * <ul>
 *   <li>{@code ListElement} - styles the container (border, background)</li>
 *   <li>{@code ListElement-item} - styles each list item</li>
 *   <li>{@code ListElement-item:selected} - styles the selected item</li>
 *   <li>{@code ListElement-item:nth-child(odd/even)} - zebra striping</li>
 *   <li>{@code ListElement-scrollbar-thumb} - styles the scrollbar thumb</li>
 *   <li>{@code ListElement-scrollbar-track} - styles the scrollbar track</li>
 * </ul>
 *
 * @see dev.tamboui.widgets.list.ListWidget for simple text-only lists at the widget level
 */
public final class ListElement<T> extends StyledElement<ListElement<T>> {

    private static final Style DEFAULT_HIGHLIGHT_STYLE = Style.EMPTY.reversed();
    private static final String DEFAULT_HIGHLIGHT_SYMBOL = "> ";

    private final List<StyledElement<?>> items = new ArrayList<>();
    private List<T> data;
    private Function<T, StyledElement<?>> itemRenderer;
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private Style highlightStyle;  // null means "use CSS or default"
    private String highlightSymbol;  // null means "use CSS or default"
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private boolean autoScroll;
    private boolean autoScrollToEnd;
    private boolean showScrollbar;
    private Color scrollbarThumbColor;
    private Color scrollbarTrackColor;

    // Cached values from last render for event handling
    private int lastItemCount;
    private int lastViewportHeight;

    public ListElement() {
    }

    public ListElement(String... items) {
        for (String item : items) {
            this.items.add(new TextElement(item));
        }
    }

    public ListElement(List<String> items) {
        for (String item : items) {
            this.items.add(new TextElement(item));
        }
    }

    /**
     * Creates a list container with styled element items.
     *
     * @param items the list items as styled elements
     */
    public ListElement(StyledElement<?>... items) {
        this.items.addAll(Arrays.asList(items));
    }

    /**
     * Sets the list items from strings.
     *
     * @param items the text items
     * @return this element
     */
    public ListElement<T> items(String... items) {
        this.items.clear();
        for (String item : items) {
            this.items.add(new TextElement(item));
        }
        return this;
    }

    /**
     * Sets the list items from a collection of strings.
     *
     * @param items the text items
     * @return this element
     */
    public ListElement<T> items(List<String> items) {
        this.items.clear();
        for (String item : items) {
            this.items.add(new TextElement(item));
        }
        return this;
    }

    /**
     * Sets the list items from styled elements.
     *
     * @param elements the styled element items
     * @return this element
     */
    public ListElement<T> elements(StyledElement<?>... elements) {
        this.items.clear();
        this.items.addAll(Arrays.asList(elements));
        return this;
    }

    /**
     * Adds a text item to the list.
     *
     * @param item the text to add
     * @return this element
     */
    public ListElement<T> add(String item) {
        this.items.add(new TextElement(item));
        return this;
    }

    /**
     * Adds a styled element item to the list.
     *
     * @param element the element to add
     * @return this element
     */
    public ListElement<T> add(StyledElement<?> element) {
        this.items.add(element);
        return this;
    }

    /**
     * Sets the selected index.
     *
     * @param index the index to select
     * @return this element
     */
    public ListElement<T> selected(int index) {
        this.selectedIndex = Math.max(0, index);
        return this;
    }

    /**
     * Returns the currently selected index.
     *
     * @return the selected index
     */
    public int selected() {
        return selectedIndex;
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
     * to a styled element. This allows the list to reflect the current state of your data.
     *
     * @param data the list of data items
     * @param renderer function to convert each item to a styled element
     * @param <U> the data item type
     * @return this element
     */
    public <U> ListElement<U> data(List<U> data, Function<U, StyledElement<?>> renderer) {
        @SuppressWarnings("unchecked")
        ListElement<U> self = (ListElement<U>) this;
        self.data = data;
        self.itemRenderer = renderer;
        self.items.clear();
        return self;
    }

    /**
     * Sets the renderer function for converting data items to styled elements.
     * <p>
     * The renderer is called at render time for each data item.
     *
     * @param renderer function to convert each item to a styled element
     * @return this element
     */
    public ListElement<T> itemRenderer(Function<T, StyledElement<?>> renderer) {
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

    /**
     * Scrolls the list to show the last items.
     * <p>
     * Unlike {@link #autoScroll()}, this scrolls to the end immediately
     * without requiring a selection. Useful for chat messages, logs, or
     * other content where you want to always show the most recent items.
     *
     * @return this element
     */
    public ListElement<T> scrollToEnd() {
        this.autoScrollToEnd = true;
        return this;
    }

    /**
     * Enables showing a scrollbar on the right side of the list.
     *
     * @return this element
     */
    public ListElement<T> scrollbar() {
        this.showScrollbar = true;
        return this;
    }

    /**
     * Sets whether a scrollbar is shown.
     *
     * @param enabled true to show a scrollbar
     * @return this element
     */
    public ListElement<T> scrollbar(boolean enabled) {
        this.showScrollbar = enabled;
        return this;
    }

    /**
     * Configures the list for display-only mode (non-interactive scrolling).
     * <p>
     * This disables visual selection feedback by setting an empty highlight symbol
     * and empty highlight style. Useful for displaying chat messages, logs, or
     * other content where selection is not meaningful.
     *
     * @return this element
     */
    public ListElement<T> displayOnly() {
        this.highlightSymbol = "";
        this.highlightStyle = Style.EMPTY;
        return this;
    }

    /**
     * Sets the scrollbar thumb color.
     *
     * @param color the thumb color
     * @return this element
     */
    public ListElement<T> scrollbarThumbColor(Color color) {
        this.scrollbarThumbColor = color;
        return this;
    }

    /**
     * Sets the scrollbar track color.
     *
     * @param color the track color
     * @return this element
     */
    public ListElement<T> scrollbarTrackColor(Color color) {
        this.scrollbarTrackColor = color;
        return this;
    }

    // Navigation methods

    /**
     * Selects the previous item.
     */
    public void selectPrevious() {
        if (selectedIndex > 0) {
            selectedIndex--;
        }
    }

    /**
     * Selects the next item.
     *
     * @param itemCount the total number of items
     */
    public void selectNext(int itemCount) {
        if (selectedIndex < itemCount - 1) {
            selectedIndex++;
        }
    }

    /**
     * Selects the first item.
     */
    public void selectFirst() {
        selectedIndex = 0;
        scrollOffset = 0;
    }

    /**
     * Selects the last item.
     *
     * @param itemCount the total number of items
     */
    public void selectLast(int itemCount) {
        selectedIndex = Math.max(0, itemCount - 1);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Build the effective items list from StyledElements
        List<StyledElement<?>> effectiveItems;
        if (data != null && itemRenderer != null) {
            effectiveItems = new ArrayList<>(data.size());
            for (T item : data) {
                effectiveItems.add(itemRenderer.apply(item));
            }
        } else {
            effectiveItems = new ArrayList<>(items);
        }

        int totalItems = effectiveItems.size();
        this.lastItemCount = totalItems;

        if (totalItems == 0) {
            return;
        }

        // Clamp selection to valid range
        selectedIndex = Math.max(0, Math.min(selectedIndex, totalItems - 1));

        // Render border/block if needed
        Rect listArea = area;
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
            Block block = blockBuilder.build();
            block.render(area, frame.buffer());
            listArea = block.inner(area);
        }

        if (listArea.isEmpty()) {
            return;
        }

        int visibleHeight = listArea.height();
        this.lastViewportHeight = visibleHeight;

        // Calculate item heights (assume 1 for each item, could be extended)
        int[] itemHeights = new int[totalItems];
        for (int i = 0; i < totalItems; i++) {
            itemHeights[i] = getItemHeight(effectiveItems.get(i));
        }

        // Auto-scroll logic
        if (autoScrollToEnd) {
            // Scroll to show last items
            int totalHeight = 0;
            for (int h : itemHeights) {
                totalHeight += h;
            }
            scrollOffset = Math.max(0, totalHeight - visibleHeight);
        } else if (autoScroll) {
            // Scroll to keep selected item visible
            scrollToSelected(visibleHeight, itemHeights);
        }

        // Resolve highlight style: explicit > CSS > default
        Style effectiveHighlightStyle = highlightStyle;
        if (effectiveHighlightStyle == null) {
            Style cssStyle = context.childStyle("item", PseudoClassState.ofSelected());
            effectiveHighlightStyle = cssStyle.equals(context.currentStyle())
                ? DEFAULT_HIGHLIGHT_STYLE
                : cssStyle;
        }

        // Resolve highlight symbol
        String effectiveHighlightSymbol = highlightSymbol != null ? highlightSymbol : DEFAULT_HIGHLIGHT_SYMBOL;
        int symbolWidth = effectiveHighlightSymbol.length();

        // Calculate content area (reserve space for symbol)
        int contentX = listArea.left() + symbolWidth;
        int contentWidth = listArea.width() - symbolWidth;
        if (showScrollbar) {
            contentWidth -= 1; // Reserve space for scrollbar
        }

        if (contentWidth <= 0) {
            return;
        }

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

            boolean isSelected = (i == selectedIndex);

            // Get CSS positional style for this item
            ChildPosition pos = ChildPosition.of(i, totalItems);
            Style posStyle = context.childStyle("item", pos);

            // Determine the row background style
            Style rowStyle = context.currentStyle();
            if (!posStyle.equals(context.currentStyle())) {
                rowStyle = rowStyle.patch(posStyle);
            }
            if (isSelected) {
                rowStyle = rowStyle.patch(effectiveHighlightStyle);
            }

            // Draw highlight symbol for selected item
            if (isSelected && symbolWidth > 0) {
                frame.buffer().setString(listArea.left(), y, effectiveHighlightSymbol, effectiveHighlightStyle);
            }

            // Render the item element (it self-registers for events if needed)
            Rect itemArea = new Rect(contentX, y, contentWidth, visibleItemHeight);
            StyledElement<?> item = effectiveItems.get(i);
            item.render(frame, itemArea, context);

            // Apply row background AFTER child renders
            // This ensures zebra/selection styling takes precedence over child's CSS background
            Color rowBg = posStyle.bg().orElse(null);
            if (isSelected && effectiveHighlightStyle.bg().isPresent()) {
                rowBg = effectiveHighlightStyle.bg().get();
            }
            if (rowBg != null) {
                Style bgOnly = Style.EMPTY.bg(rowBg);
                for (int row = 0; row < visibleItemHeight && y + row < listArea.bottom(); row++) {
                    Rect rowArea = new Rect(listArea.left(), y + row, listArea.width() - (showScrollbar ? 1 : 0), 1);
                    frame.buffer().setStyle(rowArea, bgOnly);
                }
            }

            y += visibleItemHeight;
            currentOffset += itemHeight;
        }

        // Render scrollbar if enabled
        if (showScrollbar && totalItems > 0) {
            Rect scrollbarArea = new Rect(
                listArea.right() - 1,
                listArea.top(),
                1,
                listArea.height()
            );

            int totalHeight = 0;
            for (int h : itemHeights) {
                totalHeight += h;
            }

            ScrollbarState scrollbarState = new ScrollbarState()
                .contentLength(totalHeight)
                .viewportContentLength(visibleHeight)
                .position(scrollOffset);

            // Resolve scrollbar styles
            Style thumbStyle = null;
            Style trackStyle = null;

            Style cssThumbStyle = context.childStyle("scrollbar-thumb");
            if (!cssThumbStyle.equals(context.currentStyle())) {
                thumbStyle = cssThumbStyle;
            }
            if (scrollbarThumbColor != null) {
                thumbStyle = Style.EMPTY.fg(scrollbarThumbColor);
            }

            Style cssTrackStyle = context.childStyle("scrollbar-track");
            if (!cssTrackStyle.equals(context.currentStyle())) {
                trackStyle = cssTrackStyle;
            }
            if (scrollbarTrackColor != null) {
                trackStyle = Style.EMPTY.fg(scrollbarTrackColor);
            }

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

    /**
     * Returns the height of an item (in rows).
     * Currently assumes 1 row per item, but could be extended to support multi-line items.
     */
    private int getItemHeight(StyledElement<?> item) {
        Constraint c = item.constraint();
        if (c instanceof Constraint.Length) {
            return ((Constraint.Length) c).value();
        }
        // Default to 1 row
        return 1;
    }

    /**
     * Scrolls to keep the selected item visible.
     */
    private void scrollToSelected(int visibleHeight, int[] itemHeights) {
        // Calculate the top position of the selected item
        int selectedTop = 0;
        for (int i = 0; i < selectedIndex && i < itemHeights.length; i++) {
            selectedTop += itemHeights[i];
        }
        int selectedHeight = selectedIndex < itemHeights.length ? itemHeights[selectedIndex] : 1;
        int selectedBottom = selectedTop + selectedHeight;

        // Adjust scroll offset to keep selected item visible
        if (selectedTop < scrollOffset) {
            scrollOffset = selectedTop;
        } else if (selectedBottom > scrollOffset + visibleHeight) {
            scrollOffset = selectedBottom - visibleHeight;
        }

        scrollOffset = Math.max(0, scrollOffset);
    }

    // ═══════════════════════════════════════════════════════════════
    // Event handling for automatic navigation
    // ═══════════════════════════════════════════════════════════════

    @Override
    protected boolean needsEventRouting() {
        return super.needsEventRouting() || showScrollbar || lastItemCount > 0;
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        EventResult result = super.handleKeyEvent(event, focused);
        if (result.isHandled()) {
            return result;
        }

        if (lastItemCount == 0) {
            return EventResult.UNHANDLED;
        }

        if (event.matches(Actions.MOVE_UP)) {
            selectPrevious();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.MOVE_DOWN)) {
            selectNext(lastItemCount);
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.PAGE_UP)) {
            int steps = Math.max(1, lastViewportHeight - 1);
            for (int i = 0; i < steps; i++) {
                selectPrevious();
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.PAGE_DOWN)) {
            int steps = Math.max(1, lastViewportHeight - 1);
            for (int i = 0; i < steps; i++) {
                selectNext(lastItemCount);
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.HOME)) {
            selectFirst();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.END)) {
            selectLast(lastItemCount);
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        EventResult result = super.handleMouseEvent(event);
        if (result.isHandled()) {
            return result;
        }

        if (lastItemCount > 0) {
            if (event.kind() == MouseEventKind.SCROLL_UP) {
                for (int i = 0; i < 3; i++) {
                    selectPrevious();
                }
                return EventResult.HANDLED;
            }
            if (event.kind() == MouseEventKind.SCROLL_DOWN) {
                for (int i = 0; i < 3; i++) {
                    selectNext(lastItemCount);
                }
                return EventResult.HANDLED;
            }
        }

        return EventResult.UNHANDLED;
    }
}

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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Policy for displaying the scrollbar.
     */
    public enum ScrollBarPolicy {
        /** Never show the scrollbar. */
        NONE,
        /** Always show the scrollbar. */
        ALWAYS,
        /** Show the scrollbar only when content exceeds the viewport. */
        AS_NEEDED
    }

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
    private boolean stickyScroll;
    private boolean userScrolledAway;
    private int lastDataSize;
    private ScrollBarPolicy scrollBarPolicy = ScrollBarPolicy.NONE;
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
     * <p>
     * Note: Cannot be combined with {@link #scrollToEnd()} or {@link #stickyScroll()}.
     *
     * @return this element
     * @throws IllegalStateException if another scroll mode is already enabled
     */
    public ListElement<T> autoScroll() {
        checkScrollModeNotSet("autoScroll");
        this.autoScroll = true;
        return this;
    }

    /**
     * Sets whether auto-scroll is enabled.
     *
     * @param enabled true to enable auto-scroll
     * @return this element
     * @throws IllegalStateException if enabled and another scroll mode is already enabled
     */
    public ListElement<T> autoScroll(boolean enabled) {
        if (enabled) {
            checkScrollModeNotSet("autoScroll");
        }
        this.autoScroll = enabled;
        return this;
    }

    /**
     * Scrolls the list to show the last items.
     * <p>
     * Unlike {@link #autoScroll()}, this scrolls to the end immediately
     * without requiring a selection. Useful for chat messages, logs, or
     * other content where you want to always show the most recent items.
     * <p>
     * Note: This always forces scroll to end, overriding any user scrolling.
     * For logs or chat where you want auto-scroll that pauses when the user
     * scrolls up, use {@link #stickyScroll()} instead.
     * <p>
     * Note: Cannot be combined with {@link #autoScroll()} or {@link #stickyScroll()}.
     *
     * @return this element
     * @throws IllegalStateException if another scroll mode is already enabled
     */
    public ListElement<T> scrollToEnd() {
        checkScrollModeNotSet("scrollToEnd");
        this.autoScrollToEnd = true;
        return this;
    }

    /**
     * Enables sticky scroll behavior for the list.
     * <p>
     * With sticky scroll, the list automatically scrolls to show new items
     * at the bottom, but pauses auto-scrolling when the user scrolls up.
     * Auto-scrolling resumes when the user scrolls back to the bottom.
     * <p>
     * This is ideal for logs, chat messages, or activity feeds where you want
     * to show the latest content but allow users to scroll back through history.
     * <p>
     * Note: Cannot be combined with {@link #autoScroll()} or {@link #scrollToEnd()}.
     *
     * @return this element
     * @throws IllegalStateException if another scroll mode is already enabled
     */
    public ListElement<T> stickyScroll() {
        checkScrollModeNotSet("stickyScroll");
        this.stickyScroll = true;
        return this;
    }

    private void checkScrollModeNotSet(String requestedMode) {
        if (autoScroll && !"autoScroll".equals(requestedMode)) {
            throw new IllegalStateException(
                    "Cannot enable " + requestedMode + ": autoScroll is already enabled. " +
                    "Only one scroll mode (autoScroll, scrollToEnd, stickyScroll) can be active.");
        }
        if (autoScrollToEnd && !"scrollToEnd".equals(requestedMode)) {
            throw new IllegalStateException(
                    "Cannot enable " + requestedMode + ": scrollToEnd is already enabled. " +
                    "Only one scroll mode (autoScroll, scrollToEnd, stickyScroll) can be active.");
        }
        if (stickyScroll && !"stickyScroll".equals(requestedMode)) {
            throw new IllegalStateException(
                    "Cannot enable " + requestedMode + ": stickyScroll is already enabled. " +
                    "Only one scroll mode (autoScroll, scrollToEnd, stickyScroll) can be active.");
        }
    }

    /**
     * Enables showing a scrollbar on the right side of the list (always visible).
     *
     * @return this element
     */
    public ListElement<T> scrollbar() {
        this.scrollBarPolicy = ScrollBarPolicy.ALWAYS;
        return this;
    }

    /**
     * Sets the scrollbar policy.
     *
     * @param policy the scrollbar display policy
     * @return this element
     */
    public ListElement<T> scrollbar(ScrollBarPolicy policy) {
        this.scrollBarPolicy = policy != null ? policy : ScrollBarPolicy.NONE;
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
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (title != null) {
            attrs.put("title", title);
        }
        return Collections.unmodifiableMap(attrs);
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
            Block.Builder blockBuilder = Block.builder()
                    .borders(Borders.ALL)
                    .styleResolver(styleResolver(context));
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (borderColor != null) {
                blockBuilder.borderColor(borderColor);
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

        // Resolve highlight style: explicit > CSS > default
        Style effectiveHighlightStyle = resolveEffectiveStyle(
            context, "item", PseudoClassState.ofSelected(),
            highlightStyle, DEFAULT_HIGHLIGHT_STYLE);

        // Resolve highlight symbol
        String effectiveHighlightSymbol = highlightSymbol != null ? highlightSymbol : DEFAULT_HIGHLIGHT_SYMBOL;
        int symbolWidth = effectiveHighlightSymbol.length();

        // Determine if we should reserve space for scrollbar
        // For AS_NEEDED, we use a heuristic: reserve space if items exceed viewport height
        boolean reserveScrollbarSpace = scrollBarPolicy == ScrollBarPolicy.ALWAYS
                || (scrollBarPolicy == ScrollBarPolicy.AS_NEEDED && totalItems > visibleHeight);

        // Calculate content area (reserve space for symbol and possibly scrollbar)
        // This must be done before calculating item heights since wrapping depends on width
        int contentX = listArea.left() + symbolWidth;
        int contentWidth = listArea.width() - symbolWidth;
        if (reserveScrollbarSpace) {
            contentWidth -= 1; // Reserve space for scrollbar
        }

        if (contentWidth <= 0) {
            return;
        }

        // Calculate item heights using content width for proper text wrapping
        int[] itemHeights = new int[totalItems];
        for (int i = 0; i < totalItems; i++) {
            itemHeights[i] = itemHeightOf(effectiveItems.get(i), contentWidth, context);
        }

        // Calculate total content height for scroll calculations
        int totalHeight = 0;
        for (int h : itemHeights) {
            totalHeight += h;
        }
        int maxScroll = Math.max(0, totalHeight - visibleHeight);

        // Final determination of whether to show scrollbar
        boolean showScrollbar = scrollBarPolicy == ScrollBarPolicy.ALWAYS
                || (scrollBarPolicy == ScrollBarPolicy.AS_NEEDED && totalHeight > visibleHeight);

        // Auto-scroll logic
        if (stickyScroll) {
            // Sticky scroll: auto-scroll to end unless user has scrolled away
            boolean newItemsAdded = totalItems > lastDataSize;
            lastDataSize = totalItems;

            // Clamp scrollOffset to valid range
            scrollOffset = Math.min(scrollOffset, maxScroll);
            scrollOffset = Math.max(0, scrollOffset);

            // Check if at bottom - must be at or very near maxScroll, and maxScroll must be positive
            boolean atBottom = maxScroll > 0 && scrollOffset >= maxScroll;

            // Reset userScrolledAway if at bottom
            if (atBottom) {
                userScrolledAway = false;
            } else if (newItemsAdded && !userScrolledAway) {
                // New items added and we were auto-scrolling, stay at bottom
                scrollOffset = maxScroll;
            }

            // Auto-scroll to end if user hasn't scrolled away
            if (!userScrolledAway) {
                scrollOffset = maxScroll;
            }
        } else if (autoScrollToEnd) {
            // Scroll to show last items (always, overriding user scroll)
            scrollOffset = maxScroll;
        } else if (autoScroll) {
            // Scroll to keep selected item visible
            scrollToSelected(visibleHeight, itemHeights);
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

            ScrollbarState scrollbarState = new ScrollbarState()
                .contentLength(totalHeight)
                .viewportContentLength(visibleHeight)
                .position(scrollOffset);

            // Resolve scrollbar styles: explicit > CSS > default
            Style explicitThumbStyle = scrollbarThumbColor != null ? Style.EMPTY.fg(scrollbarThumbColor) : null;
            Style explicitTrackStyle = scrollbarTrackColor != null ? Style.EMPTY.fg(scrollbarTrackColor) : null;
            Style thumbStyle = resolveEffectiveStyle(context, "scrollbar-thumb", explicitThumbStyle, Style.EMPTY);
            Style trackStyle = resolveEffectiveStyle(context, "scrollbar-track", explicitTrackStyle, Style.EMPTY);

            Scrollbar.Builder scrollbarBuilder = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_RIGHT);
            if (!thumbStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.thumbStyle(thumbStyle);
            }
            if (!trackStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.trackStyle(trackStyle);
            }

            frame.renderStatefulWidget(scrollbarBuilder.build(), scrollbarArea, scrollbarState);
        }
    }

    /**
     * Returns the height of an item (in rows) given the available content width.
     * <p>
     * Uses {@link dev.tamboui.toolkit.element.Element#preferredHeight(int, RenderContext)} to allow
     * elements like text to calculate wrapped height based on available width and CSS properties.
     *
     * @param item the item element
     * @param contentWidth the available width for content
     * @param context the render context for CSS resolution
     * @return the height in rows
     */
    private int itemHeightOf(StyledElement<?> item, int contentWidth, RenderContext context) {
        // First check for explicit length constraint
        Constraint c = item.constraint();
        if (c instanceof Constraint.Length) {
            return ((Constraint.Length) c).value();
        }
        // Use width-aware preferred height with context for CSS property resolution
        int preferred = item.preferredHeight(contentWidth, context);
        return preferred > 0 ? preferred : 1;
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
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        EventResult result = super.handleKeyEvent(event, focused);
        if (result.isHandled()) {
            return result;
        }

        if (lastItemCount == 0) {
            return EventResult.UNHANDLED;
        }

        if (event.matches(Actions.MOVE_UP)) {
            if (stickyScroll) {
                scrollOffset = Math.max(0, scrollOffset - 1);
                userScrolledAway = true; // Scrolling up means user is scrolling away
            } else {
                selectPrevious();
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.MOVE_DOWN)) {
            if (stickyScroll) {
                scrollOffset += 1;
                userScrolledAway = true; // Will be reset in render if at bottom
            } else {
                selectNext(lastItemCount);
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.PAGE_UP)) {
            int steps = Math.max(1, lastViewportHeight - 1);
            if (stickyScroll) {
                scrollOffset = Math.max(0, scrollOffset - steps);
                userScrolledAway = true; // Scrolling up means user is scrolling away
            } else {
                for (int i = 0; i < steps; i++) {
                    selectPrevious();
                }
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.PAGE_DOWN)) {
            int steps = Math.max(1, lastViewportHeight - 1);
            if (stickyScroll) {
                scrollOffset += steps;
                userScrolledAway = true; // Will be reset in render if at bottom
            } else {
                for (int i = 0; i < steps; i++) {
                    selectNext(lastItemCount);
                }
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.HOME)) {
            if (stickyScroll) {
                scrollOffset = 0;
                userScrolledAway = true; // At top, not bottom
            } else {
                selectFirst();
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.END)) {
            if (stickyScroll) {
                userScrolledAway = false; // Going to end, resume auto-scroll
                // scrollOffset will be set to max in render
            } else {
                selectLast(lastItemCount);
            }
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
                if (stickyScroll) {
                    // Direct scroll for sticky mode - scrolling up always means user is scrolling away
                    scrollOffset = Math.max(0, scrollOffset - 3);
                    userScrolledAway = true;
                } else {
                    for (int i = 0; i < 3; i++) {
                        selectPrevious();
                    }
                }
                return EventResult.HANDLED;
            }
            if (event.kind() == MouseEventKind.SCROLL_DOWN) {
                if (stickyScroll) {
                    // Direct scroll for sticky mode
                    scrollOffset += 3;
                    // Will be reset in render if at bottom
                    userScrolledAway = true;
                } else {
                    for (int i = 0; i < 3; i++) {
                        selectNext(lastItemCount);
                    }
                }
                return EventResult.HANDLED;
            }
        }

        return EventResult.UNHANDLED;
    }
}

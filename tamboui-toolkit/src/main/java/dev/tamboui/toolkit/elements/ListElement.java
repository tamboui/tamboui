/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StringConverter;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.ChildPosition;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.error.TuiException;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.common.ScrollBarPolicy;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.list.ScrollMode;

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
 * @param <T> the type of data items backing each list entry
 * @see dev.tamboui.widgets.list.ListWidget for simple text-only lists at the widget level
 */
public final class ListElement<T> extends StyledElement<ListElement<T>> {

    // ═══════════════════════════════════════════════════════════════
    // CSS Property Definitions
    // ═══════════════════════════════════════════════════════════════

    private static final Style DEFAULT_HIGHLIGHT_STYLE = Style.EMPTY.reversed();
    private static final String DEFAULT_HIGHLIGHT_SYMBOL = "> ";

    /**
     * CSS property for scrollbar policy. Values: "none", "always", "as-needed".
     */
    public static final PropertyDefinition<ScrollBarPolicy> SCROLLBAR_POLICY =
            PropertyDefinition.builder("scrollbar-policy", ScrollBarPolicy.CONVERTER)
                    .defaultValue(ScrollBarPolicy.NONE)
                    .build();

    /**
     * CSS property for highlight symbol shown before selected item.
     */
    public static final PropertyDefinition<String> HIGHLIGHT_SYMBOL =
            PropertyDefinition.builder("highlight-symbol", StringConverter.INSTANCE)
                    .defaultValue(DEFAULT_HIGHLIGHT_SYMBOL)
                    .build();

    static {
        PropertyRegistry.registerAll(
                SCROLLBAR_POLICY,
                HIGHLIGHT_SYMBOL
        );
    }

    // ═══════════════════════════════════════════════════════════════
    // Configuration
    // ═══════════════════════════════════════════════════════════════

    private final List<StyledElement<?>> items = new ArrayList<>();
    private List<T> data;
    private Function<T, StyledElement<?>> itemRenderer;
    private Style highlightStyle;  // null means "use CSS or default"
    private String highlightSymbol;  // null means "use CSS or default"
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private boolean autoScroll;
    private boolean autoScrollToEnd;
    private boolean stickyScroll;
    private ScrollBarPolicy scrollBarPolicy = ScrollBarPolicy.NONE;
    private Color scrollbarThumbColor;
    private Color scrollbarTrackColor;

    // State delegation
    private final ListState listState = new ListState();

    // Cached values from last render for event handling
    private int lastItemCount;
    private int lastViewportHeight;

    /** Creates an empty list element. */
    public ListElement() {
    }

    /**
     * Creates a list element with the given string items.
     *
     * @param items the text items
     */
    public ListElement(String... items) {
        for (String item : items) {
            this.items.add(new TextElement(item));
        }
    }

    /**
     * Creates a list element with the given string items.
     *
     * @param items the text items
     */
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

    // ═══════════════════════════════════════════════════════════════
    // Fluent API
    // ═══════════════════════════════════════════════════════════════

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
        listState.select(Math.max(0, index));
        return this;
    }

    /**
     * Returns the currently selected index.
     *
     * @return the selected index
     */
    public int selected() {
        Integer sel = listState.selected();
        return sel != null ? sel : 0;
    }

    /**
     * Sets the highlight style for selected items.
     *
     * @param style the highlight style
     * @return this element
     */
    public ListElement<T> highlightStyle(Style style) {
        this.highlightStyle = style;
        return this;
    }

    /**
     * Sets the highlight color for selected items.
     *
     * @param color the highlight color
     * @return this element
     */
    public ListElement<T> highlightColor(Color color) {
        this.highlightStyle = Style.EMPTY.fg(color).bold();
        return this;
    }

    /**
     * Sets the symbol displayed before the selected item.
     *
     * @param symbol the highlight symbol
     * @return this element
     */
    public ListElement<T> highlightSymbol(String symbol) {
        this.highlightSymbol = symbol;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the list title
     * @return this element
     */
    public ListElement<T> title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element
     */
    public ListElement<T> rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element
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
            throw new TuiException(
                    "Cannot enable " + requestedMode + ": autoScroll is already enabled. " +
                    "Only one scroll mode (autoScroll, scrollToEnd, stickyScroll) can be active.");
        }
        if (autoScrollToEnd && !"scrollToEnd".equals(requestedMode)) {
            throw new TuiException(
                    "Cannot enable " + requestedMode + ": scrollToEnd is already enabled. " +
                    "Only one scroll mode (autoScroll, scrollToEnd, stickyScroll) can be active.");
        }
        if (stickyScroll && !"stickyScroll".equals(requestedMode)) {
            throw new TuiException(
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

    // ═══════════════════════════════════════════════════════════════
    // Navigation methods
    // ═══════════════════════════════════════════════════════════════

    /**
     * Selects the previous item.
     */
    public void selectPrevious() {
        listState.selectPrevious();
    }

    /**
     * Selects the next item.
     *
     * @param itemCount the total number of items
     */
    public void selectNext(int itemCount) {
        listState.selectNext(itemCount);
    }

    /**
     * Selects the first item.
     */
    public void selectFirst() {
        listState.selectFirst();
    }

    /**
     * Selects the last item.
     *
     * @param itemCount the total number of items
     */
    public void selectLast(int itemCount) {
        listState.selectLast(itemCount);
    }

    // ═══════════════════════════════════════════════════════════════
    // Size calculation
    // ═══════════════════════════════════════════════════════════════

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        int maxWidth = 0;
        List<StyledElement<?>> effectiveItems;
        if (data != null && itemRenderer != null) {
            effectiveItems = new ArrayList<>(data.size());
            for (T item : data) {
                effectiveItems.add(itemRenderer.apply(item));
            }
        } else {
            effectiveItems = items;
        }

        for (StyledElement<?> item : effectiveItems) {
            Size itemSize = item.preferredSize(availableWidth, availableHeight, context);
            maxWidth = Math.max(maxWidth, itemSize.widthOr(0));
        }

        String effectiveSymbol = highlightSymbol != null ? highlightSymbol : DEFAULT_HIGHLIGHT_SYMBOL;
        int symbolWidth = effectiveSymbol.length();
        int borderWidth = (title != null || borderType != null) ? 2 : 0;
        int width = maxWidth + symbolWidth + borderWidth;

        int itemCount;
        if (data != null) {
            itemCount = data.size();
        } else {
            itemCount = items.size();
        }
        int borderHeight = (title != null || borderType != null) ? 2 : 0;
        int height = itemCount + borderHeight;

        return Size.of(width, height);
    }

    @Override
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (title != null) {
            attrs.put("title", title);
        }
        return Collections.unmodifiableMap(attrs);
    }

    // ═══════════════════════════════════════════════════════════════
    // Rendering — delegates to ListWidget
    // ═══════════════════════════════════════════════════════════════

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
            if (title != null || borderType != null) {
                renderBorder(frame, area, context);
            }
            return;
        }

        // Sync selection into ListState
        if (listState.selected() == null) {
            listState.select(0);
        }

        // Render border
        Rect listArea = renderBorder(frame, area, context);
        if (listArea.isEmpty()) {
            return;
        }

        this.lastViewportHeight = listArea.height();

        // Resolve CSS properties
        CssStyleResolver cssResolver = context.resolveStyle(this).orElse(CssStyleResolver.empty());

        // Build and render ListWidget
        ListWidget widget = buildListWidget(effectiveItems, frame, listArea, context, cssResolver);
        frame.renderStatefulWidget(widget, listArea, listState);
    }

    private ListWidget buildListWidget(List<StyledElement<?>> effectiveItems,
                                        Frame frame, Rect listArea,
                                        RenderContext context, CssStyleResolver cssResolver) {
        ListWidget.Builder builder = ListWidget.builder();

        // Convert StyledElements to SizedWidgets
        List<SizedWidget> sizedItems = new ArrayList<>(effectiveItems.size());
        for (StyledElement<?> element : effectiveItems) {
            sizedItems.add(adaptItemElement(element, frame, listArea, context));
        }
        builder.items(sizedItems);

        // Configure highlight
        configureHighlight(builder, context, cssResolver);

        // Configure scroll mode
        builder.scrollMode(resolveScrollMode());

        // Configure scrollbar
        configureScrollbar(builder, context, cssResolver);

        // Configure item style resolver for zebra striping
        builder.itemStyleResolver((index, total) ->
                context.childStyle("item", ChildPosition.of(index, total)));

        return builder.build();
    }

    private void configureHighlight(ListWidget.Builder builder,
                                    RenderContext context, CssStyleResolver cssResolver) {
        // Resolve highlight symbol: explicit > CSS > default
        String effectiveHighlightSymbol = cssResolver.resolve(HIGHLIGHT_SYMBOL, this.highlightSymbol);
        if (effectiveHighlightSymbol == null) {
            effectiveHighlightSymbol = DEFAULT_HIGHLIGHT_SYMBOL;
        }
        builder.highlightSymbol(effectiveHighlightSymbol);

        // Resolve highlight style: explicit > CSS > default
        Style effectiveHighlightStyle = resolveEffectiveStyle(
                context, "item", PseudoClassState.ofSelected(),
                highlightStyle, DEFAULT_HIGHLIGHT_STYLE);
        builder.highlightStyle(effectiveHighlightStyle);
    }

    private ScrollMode resolveScrollMode() {
        if (stickyScroll) {
            return ScrollMode.STICKY_SCROLL;
        } else if (autoScrollToEnd) {
            return ScrollMode.SCROLL_TO_END;
        } else if (autoScroll) {
            return ScrollMode.AUTO_SCROLL;
        }
        return ScrollMode.NONE;
    }

    private void configureScrollbar(ListWidget.Builder builder,
                                    RenderContext context, CssStyleResolver cssResolver) {
        ScrollBarPolicy effectivePolicy = cssResolver.resolve(SCROLLBAR_POLICY, this.scrollBarPolicy);
        builder.scrollBarPolicy(effectivePolicy);

        // Resolve scrollbar styles: explicit > CSS > default
        Style explicitThumbStyle = scrollbarThumbColor != null ? Style.EMPTY.fg(scrollbarThumbColor) : null;
        Style thumbStyle = resolveEffectiveStyle(context, "scrollbar-thumb", explicitThumbStyle, Style.EMPTY);
        if (!thumbStyle.equals(Style.EMPTY)) {
            builder.scrollbarThumbStyle(thumbStyle);
        }

        Style explicitTrackStyle = scrollbarTrackColor != null ? Style.EMPTY.fg(scrollbarTrackColor) : null;
        Style trackStyle = resolveEffectiveStyle(context, "scrollbar-track", explicitTrackStyle, Style.EMPTY);
        if (!trackStyle.equals(Style.EMPTY)) {
            builder.scrollbarTrackStyle(trackStyle);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // StyledElement → SizedWidget adaptation
    // ═══════════════════════════════════════════════════════════════

    private SizedWidget adaptItemElement(StyledElement<?> element,
                                          Frame frame,
                                          Rect listArea,
                                          RenderContext context) {
        int availableWidth = listArea.width();
        Size size = element.preferredSize(availableWidth, -1, context);
        int preferredWidth = size.widthOr(0);
        int preferredHeight = size.heightOr(0);

        // Check for explicit length constraint
        Constraint c = element.constraint();
        if (c instanceof Constraint.Length) {
            preferredHeight = ((Constraint.Length) c).value();
        }

        Widget adapted = createElementAdapter(element, frame, context);

        if (preferredWidth > 0 && preferredHeight > 0) {
            return SizedWidget.of(adapted, preferredWidth, preferredHeight);
        } else if (preferredHeight > 0) {
            return SizedWidget.ofHeight(adapted, preferredHeight);
        } else if (preferredWidth > 0) {
            return SizedWidget.ofWidth(adapted, preferredWidth);
        }
        return SizedWidget.of(adapted);
    }

    private Widget createElementAdapter(StyledElement<?> element, Frame frame, RenderContext context) {
        return (rect, buffer) -> {
            if (!rect.isEmpty()) {
                element.constraint(Constraint.fill());
                context.renderChild(element, frame, rect);
            }
        };
    }

    private Rect renderBorder(Frame frame, Rect area, RenderContext context) {
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
            return block.inner(area);
        }
        return area;
    }

    // ═══════════════════════════════════════════════════════════════
    // Event handling
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
                listState.scrollBy(-1);
                listState.markUserScrolledAway();
            } else {
                selectPrevious();
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.MOVE_DOWN)) {
            if (stickyScroll) {
                listState.scrollBy(1);
                listState.markUserScrolledAway();
            } else {
                selectNext(lastItemCount);
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.PAGE_UP)) {
            int steps = Math.max(1, lastViewportHeight - 1);
            if (stickyScroll) {
                listState.scrollBy(-steps);
                listState.markUserScrolledAway();
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
                listState.scrollBy(steps);
                listState.markUserScrolledAway();
            } else {
                for (int i = 0; i < steps; i++) {
                    selectNext(lastItemCount);
                }
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.HOME)) {
            if (stickyScroll) {
                listState.setOffset(0);
                listState.markUserScrolledAway();
            } else {
                selectFirst();
            }
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.END)) {
            if (stickyScroll) {
                // Going to end resumes auto-scroll; offset set by widget during render
                listState.resumeAutoScroll();
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
                    listState.scrollBy(-3);
                    listState.markUserScrolledAway();
                } else {
                    for (int i = 0; i < 3; i++) {
                        selectPrevious();
                    }
                }
                return EventResult.HANDLED;
            }
            if (event.kind() == MouseEventKind.SCROLL_DOWN) {
                if (stickyScroll) {
                    listState.scrollBy(3);
                    listState.markUserScrolledAway();
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

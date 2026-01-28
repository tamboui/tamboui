/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.PseudoClassState;
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
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.tabs.Tabs;
import dev.tamboui.widgets.tabs.TabsState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A DSL wrapper for the Tabs widget.
 * <p>
 * Displays a horizontal set of tabs with one selected.
 * <pre>{@code
 * tabs("Home", "Settings", "About")
 *     .state(tabsState)
 *     .highlightColor(Color.YELLOW)
 *     .divider(" | ")
 * }</pre>
 */
public final class TabsElement extends StyledElement<TabsElement> {

    private static final Style DEFAULT_HIGHLIGHT_STYLE = Style.EMPTY.reversed();

    private final List<String> titles = new ArrayList<>();
    private TabsState state;
    private Style highlightStyle;  // null means "use CSS or default"
    private String divider = " | ";
    private String paddingLeft = "";
    private String paddingRight = "";
    private String title;
    private BorderType borderType;
    private Color borderColor;

    /** Creates a new empty tabs element. */
    public TabsElement() {
    }

    /**
     * Creates a new tabs element with the given titles.
     *
     * @param titles the tab titles
     */
    public TabsElement(String... titles) {
        this.titles.addAll(Arrays.asList(titles));
    }

    /**
     * Creates a new tabs element with the given titles.
     *
     * @param titles the tab titles
     */
    public TabsElement(List<String> titles) {
        this.titles.addAll(titles);
    }

    /**
     * Sets the tab titles.
     *
     * @param titles the tab titles
     * @return this builder
     */
    public TabsElement titles(String... titles) {
        this.titles.clear();
        this.titles.addAll(Arrays.asList(titles));
        return this;
    }

    /**
     * Sets the tab titles from a list.
     *
     * @param titles the tab titles
     * @return this builder
     */
    public TabsElement titles(List<String> titles) {
        this.titles.clear();
        this.titles.addAll(titles);
        return this;
    }

    /**
     * Adds a tab.
     *
     * @param title the tab title to add
     * @return this builder
     */
    public TabsElement add(String title) {
        this.titles.add(title);
        return this;
    }

    /**
     * Sets the tabs state for selection tracking.
     *
     * @param state the tabs state
     * @return this builder
     */
    public TabsElement state(TabsState state) {
        this.state = state;
        return this;
    }

    /**
     * Sets the selected tab index.
     *
     * @param index the index of the selected tab
     * @return this builder
     */
    public TabsElement selected(int index) {
        this.state = new TabsState(index);
        return this;
    }

    /**
     * Sets the highlight style for the selected tab.
     *
     * @param style the highlight style
     * @return this builder
     */
    public TabsElement highlightStyle(Style style) {
        this.highlightStyle = style;
        return this;
    }

    /**
     * Sets the highlight color for the selected tab.
     *
     * @param color the highlight color
     * @return this builder
     */
    public TabsElement highlightColor(Color color) {
        this.highlightStyle = Style.EMPTY.fg(color).bold();
        return this;
    }

    /**
     * Sets the divider between tabs.
     *
     * @param divider the divider string
     * @return this builder
     */
    public TabsElement divider(String divider) {
        this.divider = divider;
        return this;
    }

    /**
     * Sets the padding around tab titles.
     *
     * @param left the left padding string
     * @param right the right padding string
     * @return this builder
     */
    public TabsElement padding(String left, String right) {
        this.paddingLeft = left;
        this.paddingRight = right;
        return this;
    }

    /**
     * Sets the title for the border.
     *
     * @param title the border title
     * @return this builder
     */
    public TabsElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this builder
     */
    public TabsElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this builder
     */
    public TabsElement borderColor(Color color) {
        this.borderColor = color;
        return this;
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
    public int preferredWidth() {
        if (titles.isEmpty()) {
            return 0;
        }

        int width = 0;

        // Calculate width of all tab titles
        for (String title : titles) {
            width += CharWidth.of(title);
        }

        // Add padding for each tab
        int paddingPerTab = CharWidth.of(paddingLeft) + CharWidth.of(paddingRight);
        width += paddingPerTab * titles.size();

        // Add dividers between tabs (n-1 dividers for n tabs)
        if (titles.size() > 1) {
            width += CharWidth.of(divider) * (titles.size() - 1);
        }

        // Add border width if there's a border
        if (borderType != null || title != null) {
            // Borders add 2 cells (left + right)
            width += 2;
        }

        return width;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Resolve base tab style from CSS
        Style baseTabStyle = context.childStyle("tab");
        Style effectiveTabStyle = baseTabStyle.equals(context.currentStyle())
            ? context.currentStyle()
            : context.currentStyle().patch(baseTabStyle);

        // Resolve highlight style: explicit > CSS > default
        Style effectiveHighlightStyle = resolveEffectiveStyle(
            context, "tab", PseudoClassState.ofSelected(),
            highlightStyle, DEFAULT_HIGHLIGHT_STYLE);

        // Resolve divider style from CSS
        Style dividerCssStyle = context.childStyle("divider");
        Span dividerSpan = dividerCssStyle.equals(context.currentStyle())
            ? Span.raw(divider)
            : Span.styled(divider, dividerCssStyle);

        Tabs.Builder builder = Tabs.builder()
            .titles(titles.toArray(new String[0]))
            .style(effectiveTabStyle)
            .highlightStyle(effectiveHighlightStyle)
            .divider(dividerSpan)
            .padding(paddingLeft, paddingRight);

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
            builder.block(blockBuilder.build());
        }

        Tabs widget = builder.build();
        TabsState effectiveState = state != null ? state : new TabsState(0);
        frame.renderStatefulWidget(widget, area, effectiveState);
    }
}

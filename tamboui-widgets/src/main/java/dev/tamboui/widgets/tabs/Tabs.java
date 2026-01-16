/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tabs;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyKey;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardPropertyKeys;
import dev.tamboui.style.Style;
import dev.tamboui.style.StyledProperty;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widgets.block.Block;
import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A horizontal set of tabs with a single tab selected.
 * <p>
 * Each tab title is stored as a {@link Line} and can be individually styled.
 * The selected tab is highlighted using the highlight style.
 *
 * <pre>{@code
 * Tabs tabs = Tabs.builder()
 *     .titles("Home", "Settings", "About")
 *     .highlightStyle(Style.EMPTY.fg(Color.YELLOW).bold())
 *     .divider(" | ")
 *     .block(Block.bordered())
 *     .build();
 *
 * TabsState state = new TabsState(0); // Select first tab
 * frame.renderStatefulWidget(tabs, area, state);
 * }</pre>
 */
public final class Tabs implements StatefulWidget<TabsState> {

    /**
     * Property key for the selected tab highlight color.
     * <p>
     * CSS property name: {@code highlight-color}
     */
    public static final PropertyKey<Color> HIGHLIGHT_COLOR =
            PropertyKey.of("highlight-color", ColorConverter.INSTANCE);

    private final List<Line> titles;
    private final Block block;
    private final Style style;
    private final Style highlightStyle;
    private final Span divider;
    private final String paddingLeft;
    private final String paddingRight;

    private Tabs(Builder builder) {
        this.titles = listCopyOf(builder.titles);
        this.block = builder.block;
        this.divider = builder.divider;
        this.paddingLeft = builder.paddingLeft;
        this.paddingRight = builder.paddingRight;

        // Resolve style-aware properties
        Color resolvedBg = builder.background.resolve();
        Color resolvedHighlightColor = builder.highlightColor.resolve();

        Style baseStyle = builder.style;
        if (resolvedBg != null) {
            baseStyle = baseStyle.bg(resolvedBg);
        }
        this.style = baseStyle;

        Style baseHighlightStyle = builder.highlightStyle;
        if (resolvedHighlightColor != null) {
            baseHighlightStyle = baseHighlightStyle.fg(resolvedHighlightColor);
        }
        this.highlightStyle = baseHighlightStyle;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates tabs from string titles.
     */
    public static Tabs from(String... titles) {
        return builder().titles(titles).build();
    }

    /**
     * Creates tabs from line titles.
     */
    public static Tabs from(Line... titles) {
        return builder().titles(titles).build();
    }

    /**
     * Returns the number of tabs.
     */
    public int size() {
        return titles.size();
    }

    /**
     * Returns the tab titles.
     */
    public List<Line> titles() {
        return titles;
    }

    @Override
    public void render(Rect area, Buffer buffer, TabsState state) {
        if (area.isEmpty()) {
            return;
        }

        // Apply background style
        buffer.setStyle(area, style);

        // Render block if present
        Rect tabsArea = area;
        if (block != null) {
            block.render(area, buffer);
            tabsArea = block.inner(area);
        }

        if (tabsArea.isEmpty() || titles.isEmpty()) {
            return;
        }

        int x = tabsArea.left();
        int y = tabsArea.top();
        Integer selected = state.selected();

        for (int i = 0; i < titles.size(); i++) {
            // Add divider before tab (except first)
            if (i > 0) {
                if (x + divider.content().length() > tabsArea.right()) {
                    break;
                }
                x = buffer.setSpan(x, y, divider.patchStyle(style));
            }

            Line title = titles.get(i);
            boolean isSelected = selected != null && selected == i;
            Style tabStyle = isSelected ? highlightStyle : style;

            // Add left padding
            if (!paddingLeft.isEmpty()) {
                if (x + paddingLeft.length() > tabsArea.right()) {
                    break;
                }
                x = buffer.setString(x, y, paddingLeft, tabStyle);
            }

            // Render tab title
            Line styledTitle = title.patchStyle(tabStyle);
            int titleWidth = styledTitle.width();
            if (x + titleWidth > tabsArea.right()) {
                // Truncate if needed
                titleWidth = tabsArea.right() - x;
            }
            if (titleWidth <= 0) {
                break;
            }

            // Render each span of the title
            for (Span span : styledTitle.spans()) {
                if (x >= tabsArea.right()) {
                    break;
                }
                int remainingWidth = tabsArea.right() - x;
                String content = span.content();
                if (content.length() > remainingWidth) {
                    content = content.substring(0, remainingWidth);
                }
                x = buffer.setString(x, y, content, span.style());
            }

            // Add right padding
            if (!paddingRight.isEmpty()) {
                if (x + paddingRight.length() <= tabsArea.right()) {
                    x = buffer.setString(x, y, paddingRight, tabStyle);
                }
            }
        }
    }

    public static final class Builder {
        private List<Line> titles = new ArrayList<>();
        private Block block;
        private Style style = Style.EMPTY;
        private Style highlightStyle = Style.EMPTY.reversed();
        private Span divider = Span.raw(" | ");
        private String paddingLeft = "";
        private String paddingRight = "";
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties bound to this builder's resolver
        private final StyledProperty<Color> background =
                StyledProperty.of(StandardPropertyKeys.BACKGROUND, null, () -> styleResolver);
        private final StyledProperty<Color> highlightColor =
                StyledProperty.of(HIGHLIGHT_COLOR, null, () -> styleResolver);

        private Builder() {}

        /**
         * Sets the tab titles from strings.
         */
        public Builder titles(String... titles) {
            this.titles = new ArrayList<>();
            for (String title : titles) {
                this.titles.add(Line.from(title));
            }
            return this;
        }

        /**
         * Sets the tab titles from lines.
         */
        public Builder titles(Line... titles) {
            this.titles = new ArrayList<>(Arrays.asList(titles));
            return this;
        }

        /**
         * Sets the tab titles from a list.
         */
        public Builder titles(List<Line> titles) {
            this.titles = new ArrayList<>(titles);
            return this;
        }

        /**
         * Adds a tab title.
         */
        public Builder addTitle(String title) {
            this.titles.add(Line.from(title));
            return this;
        }

        /**
         * Adds a tab title.
         */
        public Builder addTitle(Line title) {
            this.titles.add(title);
            return this;
        }

        /**
         * Wraps the tabs in a block.
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the base style for unselected tabs.
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the style for the selected tab.
         */
        public Builder highlightStyle(Style style) {
            this.highlightStyle = style;
            return this;
        }

        /**
         * Sets the divider between tabs.
         */
        public Builder divider(String divider) {
            this.divider = Span.raw(divider);
            return this;
        }

        /**
         * Sets the divider between tabs.
         */
        public Builder divider(Span divider) {
            this.divider = divider;
            return this;
        }

        /**
         * Sets the padding on both sides of each tab title.
         */
        public Builder padding(String left, String right) {
            this.paddingLeft = left;
            this.paddingRight = right;
            return this;
        }

        /**
         * Sets the left padding for each tab title.
         */
        public Builder paddingLeft(String padding) {
            this.paddingLeft = padding;
            return this;
        }

        /**
         * Sets the right padding for each tab title.
         */
        public Builder paddingRight(String padding) {
            this.paddingRight = padding;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code background} and {@code highlight-color}
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
         * Sets the background color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.background.set(color);
            return this;
        }

        /**
         * Sets the selected tab highlight color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the highlight color
         * @return this builder
         */
        public Builder highlightColor(Color color) {
            this.highlightColor.set(color);
            return this;
        }

        public Tabs build() {
            return new Tabs(this);
        }
    }
}

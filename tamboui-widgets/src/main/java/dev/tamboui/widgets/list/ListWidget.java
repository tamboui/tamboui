/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.StatefulWidget;
import dev.tamboui.widgets.block.Block;

/**
 * A list widget for displaying selectable items.
 */
public final class ListWidget implements StatefulWidget<ListState> {

    private final List<ListItem> items;
    private final Block block;
    private final Style style;
    private final Style highlightStyle;
    private final String highlightSymbol;

    private ListWidget(Builder builder) {
        this.items = listCopyOf(builder.items);
        this.block = builder.block;
        this.style = builder.style;
        this.highlightStyle = builder.highlightStyle;
        this.highlightSymbol = builder.highlightSymbol;
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

        int symbolWidth = highlightSymbol.length();
        int contentWidth = listArea.width() - symbolWidth;
        if (contentWidth <= 0) {
            return;
        }

        int y = listArea.top();
        int offset = state.offset();
        int currentOffset = 0;

        for (int i = 0; i < items.size() && y < listArea.bottom(); i++) {
            ListItem item = items.get(i);
            int itemHeight = item.height();

            // Skip items before the visible area
            if (currentOffset + itemHeight <= offset) {
                currentOffset += itemHeight;
                continue;
            }

            // Calculate visible portion of this item
            int startLine = Math.max(0, offset - currentOffset);
            int visibleHeight = Math.min(itemHeight - startLine, listArea.bottom() - y);

            boolean isSelected = state.selected() != null && state.selected() == i;
            Style itemStyle = item.style().patch(isSelected ? highlightStyle : Style.EMPTY);

            // Draw highlight symbol
            if (isSelected && symbolWidth > 0) {
                buffer.setString(listArea.left(), y, highlightSymbol, highlightStyle);
            }

            // Draw item content
            int contentX = listArea.left() + symbolWidth;
            List<Line> lines = item.content().lines();

            for (int lineIdx = startLine; lineIdx < startLine + visibleHeight && lineIdx < lines.size(); lineIdx++) {
                Line line = lines.get(lineIdx).patchStyle(itemStyle);
                
                // Truncate line to fit within contentWidth
                int col_x = contentX;
                List<Span> spans = line.spans();
                for (Span span : spans) {
                    if (col_x >= listArea.right()) {
                        break;
                    }
                    String text = span.content();
                    int remainingWidth = Math.min(text.length(), contentWidth - (col_x - contentX));
                    if (remainingWidth > 0) {
                        buffer.setString(col_x, y, text.substring(0, remainingWidth), span.style());
                        col_x += remainingWidth;
                    }
                }
                y++;
            }

            currentOffset += itemHeight;
        }
    }

    public static final class Builder {
        private List<ListItem> items = new ArrayList<>();
        private Block block;
        private Style style = Style.EMPTY;
        private Style highlightStyle = Style.EMPTY.reversed();
        private String highlightSymbol = ">> ";

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

        public Builder highlightSymbol(String highlightSymbol) {
            this.highlightSymbol = highlightSymbol;
            return this;
        }

        public ListWidget build() {
            return new ListWidget(this);
        }
    }
}

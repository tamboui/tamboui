/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.flow;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.Widget;

import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

/**
 * A wrap layout widget where items flow left-to-right and wrap to the
 * next line when exceeding the available width.
 * <p>
 * Useful for tag clouds, button groups, chip lists, and similar layouts
 * where items should wrap naturally.
 * <p>
 * Example usage:
 * <pre>{@code
 * Flow flow = Flow.builder()
 *     .item(tagWidget1, 8)
 *     .item(tagWidget2, 12)
 *     .item(tagWidget3, 6)
 *     .horizontalSpacing(1)
 *     .verticalSpacing(1)
 *     .build();
 *
 * flow.render(area, buffer);
 * }</pre>
 */
public final class Flow implements Widget {

    private final List<FlowItem> items;
    private final int horizontalSpacing;
    private final int verticalSpacing;

    private Flow(Builder builder) {
        this.items = listCopyOf(builder.items);
        this.horizontalSpacing = builder.horizontalSpacing;
        this.verticalSpacing = builder.verticalSpacing;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty() || items.isEmpty()) {
            return;
        }

        int currentX = area.x();
        int currentY = area.y();
        int rowHeight = 0;

        for (FlowItem item : items) {
            int itemWidth = item.width();
            int itemHeight = item.height();

            // Wrap to next line if needed (but not if we're at the start of a line)
            if (currentX + itemWidth > area.right() && currentX != area.x()) {
                currentY += rowHeight + verticalSpacing;
                currentX = area.x();
                rowHeight = 0;
            }

            // Stop if we've gone past the bottom
            if (currentY >= area.bottom()) {
                break;
            }

            // Clamp item height to available space
            int clampedHeight = Math.min(itemHeight, area.bottom() - currentY);
            Rect itemArea = new Rect(currentX, currentY, itemWidth, clampedHeight);
            item.widget().render(itemArea, buffer);

            currentX += itemWidth + horizontalSpacing;
            rowHeight = Math.max(rowHeight, itemHeight);
        }
    }

    /**
     * Builder for {@link Flow}.
     */
    public static final class Builder {
        private final List<FlowItem> items = new ArrayList<>();
        private int horizontalSpacing = 0;
        private int verticalSpacing = 0;

        private Builder() {
        }

        /**
         * Adds an item with the given widget and width (height defaults to 1).
         *
         * @param widget the widget to render
         * @param width  the width in cells
         * @return this builder
         */
        public Builder item(Widget widget, int width) {
            this.items.add(FlowItem.of(widget, width));
            return this;
        }

        /**
         * Adds an item with the given widget, width, and height.
         *
         * @param widget the widget to render
         * @param width  the width in cells
         * @param height the height in cells
         * @return this builder
         */
        public Builder item(Widget widget, int width, int height) {
            this.items.add(FlowItem.of(widget, width, height));
            return this;
        }

        /**
         * Sets the items from a list.
         *
         * @param items the flow items
         * @return this builder
         */
        public Builder items(List<FlowItem> items) {
            this.items.clear();
            this.items.addAll(items);
            return this;
        }

        /**
         * Sets the horizontal gap between items on the same row.
         *
         * @param spacing the horizontal spacing in cells (default: 0)
         * @return this builder
         */
        public Builder horizontalSpacing(int spacing) {
            this.horizontalSpacing = Math.max(0, spacing);
            return this;
        }

        /**
         * Sets the vertical gap between rows.
         *
         * @param spacing the vertical spacing in cells (default: 0)
         * @return this builder
         */
        public Builder verticalSpacing(int spacing) {
            this.verticalSpacing = Math.max(0, spacing);
            return this;
        }

        /**
         * Builds the {@link Flow} widget.
         *
         * @return a new Flow widget
         */
        public Flow build() {
            return new Flow(this);
        }
    }
}

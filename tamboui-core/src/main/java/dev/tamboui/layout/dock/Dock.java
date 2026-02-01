/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.dock;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * A 5-region dock layout widget that arranges children into top, bottom,
 * left, right, and center regions — the most common TUI application structure
 * (header + sidebar + content + footer).
 * <p>
 * The layout algorithm works in two steps:
 * <ol>
 *   <li>Vertical split: {@code [topHeight, fill(), bottomHeight]} to get
 *       topRect, middleRect, bottomRect (skipping null regions)</li>
 *   <li>Horizontal split of middleRect: {@code [leftWidth, fill(), rightWidth]}
 *       to get leftRect, centerRect, rightRect (skipping null regions)</li>
 * </ol>
 * <p>
 * Example usage:
 * <pre>{@code
 * Dock dock = Dock.builder()
 *     .top(headerWidget)
 *     .bottom(footerWidget)
 *     .left(sidebarWidget)
 *     .center(contentWidget)
 *     .topHeight(Constraint.length(3))
 *     .bottomHeight(Constraint.length(1))
 *     .leftWidth(Constraint.length(20))
 *     .build();
 *
 * dock.render(area, buffer);
 * }</pre>
 */
public final class Dock implements Widget {

    private final Widget top;
    private final Widget bottom;
    private final Widget left;
    private final Widget right;
    private final Widget center;
    private final Constraint topHeight;
    private final Constraint bottomHeight;
    private final Constraint leftWidth;
    private final Constraint rightWidth;

    private Dock(Builder builder) {
        this.top = builder.top;
        this.bottom = builder.bottom;
        this.left = builder.left;
        this.right = builder.right;
        this.center = builder.center;
        this.topHeight = builder.topHeight;
        this.bottomHeight = builder.bottomHeight;
        this.leftWidth = builder.leftWidth;
        this.rightWidth = builder.rightWidth;
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
        if (area.isEmpty()) {
            return;
        }
        if (top == null && bottom == null && left == null && right == null && center == null) {
            return;
        }

        // Step 1: Vertical split for top / middle / bottom
        Rect topRect = null;
        Rect middleRect;
        Rect bottomRect = null;

        List<Constraint> vConstraints = new ArrayList<>();
        if (top != null) {
            vConstraints.add(topHeight);
        }
        vConstraints.add(Constraint.fill());
        if (bottom != null) {
            vConstraints.add(bottomHeight);
        }

        List<Rect> vAreas = Layout.vertical()
            .constraints(vConstraints)
            .split(area);

        int idx = 0;
        if (top != null) {
            topRect = vAreas.get(idx++);
        }
        middleRect = vAreas.get(idx++);
        if (bottom != null) {
            bottomRect = vAreas.get(idx);
        }

        // Step 2: Horizontal split of middle for left / center / right
        Rect leftRect = null;
        Rect centerRect = null;
        Rect rightRect = null;

        if (left != null || center != null || right != null) {
            List<Constraint> hConstraints = new ArrayList<>();
            if (left != null) {
                hConstraints.add(leftWidth);
            }
            if (center != null || (left == null && right == null)) {
                hConstraints.add(Constraint.fill());
            } else if (left != null && right != null) {
                // No center, but both sides present — add fill for the gap
                hConstraints.add(Constraint.fill());
            } else {
                hConstraints.add(Constraint.fill());
            }
            if (right != null) {
                hConstraints.add(rightWidth);
            }

            List<Rect> hAreas = Layout.horizontal()
                .constraints(hConstraints)
                .split(middleRect);

            int hIdx = 0;
            if (left != null) {
                leftRect = hAreas.get(hIdx++);
            }
            centerRect = hAreas.get(hIdx++);
            if (right != null) {
                rightRect = hAreas.get(hIdx);
            }
        } else {
            centerRect = middleRect;
        }

        // Render each non-null region
        if (top != null && topRect != null) {
            top.render(topRect, buffer);
        }
        if (bottom != null && bottomRect != null) {
            bottom.render(bottomRect, buffer);
        }
        if (left != null && leftRect != null) {
            left.render(leftRect, buffer);
        }
        if (right != null && rightRect != null) {
            right.render(rightRect, buffer);
        }
        if (center != null && centerRect != null) {
            center.render(centerRect, buffer);
        }
    }

    /**
     * Builder for {@link Dock}.
     */
    public static final class Builder {
        private Widget top;
        private Widget bottom;
        private Widget left;
        private Widget right;
        private Widget center;
        private Constraint topHeight = Constraint.length(1);
        private Constraint bottomHeight = Constraint.length(1);
        private Constraint leftWidth = Constraint.length(10);
        private Constraint rightWidth = Constraint.length(10);

        private Builder() {
        }

        /**
         * Sets the top region widget.
         *
         * @param widget the top widget (e.g., a header bar)
         * @return this builder
         */
        public Builder top(Widget widget) {
            this.top = widget;
            return this;
        }

        /**
         * Sets the bottom region widget.
         *
         * @param widget the bottom widget (e.g., a status bar)
         * @return this builder
         */
        public Builder bottom(Widget widget) {
            this.bottom = widget;
            return this;
        }

        /**
         * Sets the left region widget.
         *
         * @param widget the left widget (e.g., a sidebar)
         * @return this builder
         */
        public Builder left(Widget widget) {
            this.left = widget;
            return this;
        }

        /**
         * Sets the right region widget.
         *
         * @param widget the right widget (e.g., a side panel)
         * @return this builder
         */
        public Builder right(Widget widget) {
            this.right = widget;
            return this;
        }

        /**
         * Sets the center region widget.
         *
         * @param widget the center widget (e.g., main content)
         * @return this builder
         */
        public Builder center(Widget widget) {
            this.center = widget;
            return this;
        }

        /**
         * Sets the height constraint for the top region.
         *
         * @param constraint the height constraint (default: {@code length(1)})
         * @return this builder
         */
        public Builder topHeight(Constraint constraint) {
            this.topHeight = constraint;
            return this;
        }

        /**
         * Sets the height constraint for the bottom region.
         *
         * @param constraint the height constraint (default: {@code length(1)})
         * @return this builder
         */
        public Builder bottomHeight(Constraint constraint) {
            this.bottomHeight = constraint;
            return this;
        }

        /**
         * Sets the width constraint for the left region.
         *
         * @param constraint the width constraint (default: {@code length(10)})
         * @return this builder
         */
        public Builder leftWidth(Constraint constraint) {
            this.leftWidth = constraint;
            return this;
        }

        /**
         * Sets the width constraint for the right region.
         *
         * @param constraint the width constraint (default: {@code length(10)})
         * @return this builder
         */
        public Builder rightWidth(Constraint constraint) {
            this.rightWidth = constraint;
            return this;
        }

        /**
         * Builds the {@link Dock} widget.
         *
         * @return a new Dock widget
         */
        public Dock build() {
            return new Dock(this);
        }
    }
}

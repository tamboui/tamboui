/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.flow;

import dev.tamboui.widget.Widget;

/**
 * An item in a {@link Flow} layout with explicit width and height.
 * <p>
 * At the widget level, explicit sizing is required because the
 * {@link Widget} interface has no {@code preferredWidth()} method.
 * At the element level, {@code FlowElement} auto-measures children
 * via {@code Element.preferredWidth()}.
 */
public final class FlowItem {

    private final Widget widget;
    private final int width;
    private final int height;

    private FlowItem(Widget widget, int width, int height) {
        this.widget = widget;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a flow item with the given widget and width (height defaults to 1).
     *
     * @param widget the widget to render
     * @param width  the width in cells
     * @return a new flow item
     */
    public static FlowItem of(Widget widget, int width) {
        return new FlowItem(widget, width, 1);
    }

    /**
     * Creates a flow item with the given widget, width, and height.
     *
     * @param widget the widget to render
     * @param width  the width in cells
     * @param height the height in cells
     * @return a new flow item
     */
    public static FlowItem of(Widget widget, int width, int height) {
        return new FlowItem(widget, width, height);
    }

    /**
     * Returns the widget.
     *
     * @return the widget
     */
    public Widget widget() {
        return widget;
    }

    /**
     * Returns the item width in cells.
     *
     * @return the width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the item height in cells.
     *
     * @return the height
     */
    public int height() {
        return height;
    }
}

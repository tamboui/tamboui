/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.common;

import java.util.Objects;

import dev.tamboui.widget.Widget;

/**
 * Pairs a {@link Widget} with optional explicit width and height dimensions.
 * <p>
 * This class allows specifying preferred dimensions for widgets in contexts
 * where the widget itself doesn't provide size information. The sentinel value
 * {@link #DEFAULT} indicates "use the default" behavior (typically fill for
 * width, 1 for height).
 *
 * <pre>{@code
 * // Widget with default dimensions
 * SizedWidget.of(myWidget)
 *
 * // Widget with explicit height of 2 lines
 * SizedWidget.ofHeight(myWidget, 2)
 *
 * // Widget with explicit width of 20 columns
 * SizedWidget.ofWidth(myWidget, 20)
 *
 * // Widget with both dimensions specified
 * SizedWidget.of(myWidget, 20, 2)
 * }</pre>
 */
public final class SizedWidget {

    /**
     * Sentinel value meaning "use default" (fill for width, 1 for height).
     */
    public static final int DEFAULT = -1;

    private final Widget widget;
    private final int width;
    private final int height;

    private SizedWidget(Widget widget, int width, int height) {
        this.widget = Objects.requireNonNull(widget, "widget");
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a SizedWidget with default width (fill) and height (1).
     *
     * @param widget the widget to wrap
     * @return a new SizedWidget
     */
    public static SizedWidget of(Widget widget) {
        return new SizedWidget(widget, DEFAULT, DEFAULT);
    }

    /**
     * Creates a SizedWidget with explicit height, default width (fill).
     *
     * @param widget the widget to wrap
     * @param height the explicit height in rows
     * @return a new SizedWidget
     */
    public static SizedWidget ofHeight(Widget widget, int height) {
        return new SizedWidget(widget, DEFAULT, height);
    }

    /**
     * Creates a SizedWidget with explicit width, default height (1).
     *
     * @param widget the widget to wrap
     * @param width the explicit width in columns
     * @return a new SizedWidget
     */
    public static SizedWidget ofWidth(Widget widget, int width) {
        return new SizedWidget(widget, width, DEFAULT);
    }

    /**
     * Creates a SizedWidget with explicit width and height.
     *
     * @param widget the widget to wrap
     * @param width the explicit width in columns, or {@link #DEFAULT}
     * @param height the explicit height in rows, or {@link #DEFAULT}
     * @return a new SizedWidget
     */
    public static SizedWidget of(Widget widget, int width, int height) {
        return new SizedWidget(widget, width, height);
    }

    /**
     * Returns the wrapped widget.
     *
     * @return the widget
     */
    public Widget widget() {
        return widget;
    }

    /**
     * Returns the explicit width, or {@link #DEFAULT} for default behavior.
     *
     * @return the width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the explicit height, or {@link #DEFAULT} for default behavior.
     *
     * @return the height
     */
    public int height() {
        return height;
    }

    /**
     * Returns whether an explicit width was specified.
     *
     * @return true if width is not {@link #DEFAULT}
     */
    public boolean hasExplicitWidth() {
        return width != DEFAULT;
    }

    /**
     * Returns whether an explicit height was specified.
     *
     * @return true if height is not {@link #DEFAULT}
     */
    public boolean hasExplicitHeight() {
        return height != DEFAULT;
    }

    /**
     * Returns the height if explicitly set, or the given default value.
     *
     * @param defaultValue the value to return if height is {@link #DEFAULT}
     * @return the height or default
     */
    public int heightOr(int defaultValue) {
        return height == DEFAULT ? defaultValue : height;
    }

    /**
     * Returns the width if explicitly set, or the given default value.
     *
     * @param defaultValue the value to return if width is {@link #DEFAULT}
     * @return the width or default
     */
    public int widthOr(int defaultValue) {
        return width == DEFAULT ? defaultValue : width;
    }
}

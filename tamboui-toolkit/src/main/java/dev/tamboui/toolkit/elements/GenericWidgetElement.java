/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.widget.Widget;

/**
 * A generic element that wraps any {@link Widget} and exposes it as a {@link StyledElement}.
 * <p>
 * This is useful when you need to use a widget that doesn't have a dedicated element wrapper,
 * allowing it to participate in the toolkit's styling and layout system.
 *
 * <h2>When to Use</h2>
 * <ul>
 *   <li>You have a custom or third-party widget without a dedicated toolkit element</li>
 *   <li>You need quick integration of a widget into the toolkit DSL</li>
 *   <li>You want to apply layout constraints, CSS classes, or event handlers to a widget</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * // Wrap any widget
 * GenericWidgetElement.of(someWidget)
 *     .fg(Color.RED)
 *     .addClass("custom-class")
 *     .fill()
 *
 * // Or use the Toolkit DSL
 * import static dev.tamboui.toolkit.Toolkit.*;
 * widget(someWidget).bold()
 * }</pre>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li><strong>Styling does not propagate:</strong> Styles set on this element (colors, modifiers)
 *       do not affect the widget's internal rendering. The widget renders directly to the buffer
 *       using its own styling logic.</li>
 *   <li><strong>No CSS child selectors:</strong> This element has no sub-components that can be
 *       styled via CSS child selectors.</li>
 *   <li><strong>No preferred size:</strong> This element does not implement {@code preferredWidth()}
 *       or {@code preferredHeight()} since the wrapped widget's size requirements are unknown.</li>
 * </ul>
 * <p>
 * For full styling support, consider creating a dedicated element wrapper for the specific widget type.
 *
 * @param <T> the type of the wrapped widget
 */
public final class GenericWidgetElement<T extends Widget> extends StyledElement<GenericWidgetElement<T>> {

    private final T widget;

    private GenericWidgetElement(T widget) {
        if (widget == null) {
            throw new IllegalArgumentException("Widget cannot be null");
        }
        this.widget = widget;
    }

    /**
     * Creates a new GenericWidgetElement wrapping the given widget.
     *
     * @param <T> the type of the widget
     * @param widget the widget to wrap
     * @return a new GenericWidgetElement
     * @throws IllegalArgumentException if widget is null
     */
    public static <T extends Widget> GenericWidgetElement<T> of(T widget) {
        return new GenericWidgetElement<>(widget);
    }

    /**
     * Creates a new GenericWidgetElement wrapping the given widget.
     * <p>
     * This is an alias for {@link #of(Widget)} intended for static imports.
     *
     * @param <T> the type of the widget
     * @param widget the widget to wrap
     * @return a new GenericWidgetElement
     * @throws IllegalArgumentException if widget is null
     */
    public static <T extends Widget> GenericWidgetElement<T> widget(T widget) {
        return new GenericWidgetElement<>(widget);
    }

    /**
     * Returns the wrapped widget.
     *
     * @return the widget
     */
    public T widget() {
        return widget;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        frame.renderWidget(widget, area);
    }
}
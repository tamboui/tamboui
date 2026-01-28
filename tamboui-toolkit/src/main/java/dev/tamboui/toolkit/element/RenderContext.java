/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;

import java.util.Optional;

/**
 * Context provided during rendering, giving access to focus state and CSS styling.
 * <p>
 * This interface exposes only what user code needs during rendering.
 * Internal framework functionality is handled automatically.
 */
public interface RenderContext {

    /**
     * Returns whether the element with the given ID is currently focused.
     *
     * @param elementId the element ID to check
     * @return true if focused, false otherwise
     */
    boolean isFocused(String elementId);

    /**
     * Returns whether any element is currently focused.
     *
     * @return true if an element is focused
     */
    boolean hasFocus();

    /**
     * Resolves the CSS style for an element.
     * <p>
     * Returns the resolved CSS style if a StyleEngine is configured and matching
     * rules are found, or empty if no CSS styling is applicable.
     *
     * @param element the element to resolve styles for
     * @return the resolved style, or empty if no CSS is applicable
     */
    default Optional<CssStyleResolver> resolveStyle(Styleable element) {
        return Optional.empty();
    }

    /**
     * Resolves the CSS style for a virtual element with the given type and classes.
     * <p>
     * This is useful for resolving styles for sub-elements (like list items)
     * that aren't full Element instances but need CSS styling.
     *
     * @param styleType the element type (e.g., "ListItem")
     * @param cssClasses the CSS classes to apply
     * @return the resolved style, or empty if no CSS is applicable
     */
    default Optional<CssStyleResolver> resolveStyle(String styleType, String... cssClasses) {
        return Optional.empty();
    }

    /**
     * Parses a CSS color value string into a Color.
     * <p>
     * Supports named colors (e.g., "red", "blue"), hex colors (e.g., "#ff0000"),
     * and RGB notation (e.g., "rgb(255,0,0)").
     *
     * @param colorValue the CSS color value string
     * @return the parsed color, or empty if parsing fails
     */
    default Optional<Color> parseColor(String colorValue) {
        return Optional.empty();
    }

    /**
     * Returns the current style from the style stack.
     * <p>
     * This style represents the accumulated styles from parent elements
     * and should be used as the base for rendering operations.
     *
     * @return the current style, or {@link Style#EMPTY} if no style is active
     */
    default Style currentStyle() {
        return Style.EMPTY;
    }

    /**
     * Resolves CSS style for a child element.
     * <p>
     * The child type is derived from the current element's type plus the child name
     * (e.g., for a ListElement rendering, "item" becomes "ListElement-item").
     * <p>
     * Example usage:
     * <pre>{@code
     * Style itemStyle = context.childStyle("item");
     * Style selectedStyle = context.childStyle("item", PseudoClassState.ofSelected());
     * }</pre>
     * <p>
     * This enables CSS selectors like:
     * <pre>{@code
     * ListElement-item { color: white; }
     * ListElement-item:selected { color: cyan; text-style: bold; }
     * #nav ListElement ListElement-item:selected { color: green; }
     * }</pre>
     *
     * @param childName the child name (e.g., "item", "header", "tab")
     * @return the resolved style, merged with the current context style
     */
    default Style childStyle(String childName) {
        return childStyle(childName, dev.tamboui.css.cascade.PseudoClassState.NONE);
    }

    /**
     * Resolves CSS style for a child element with a pseudo-class state.
     * <p>
     * Use this for stateful children like selected items or focused tabs.
     *
     * @param childName the child name (e.g., "item", "row", "tab")
     * @param state the pseudo-class state (e.g., selected, hover, disabled)
     * @return the resolved style, merged with the current context style
     */
    default Style childStyle(String childName, dev.tamboui.css.cascade.PseudoClassState state) {
        return currentStyle();  // fallback when no CSS engine
    }

    /**
     * Resolves CSS style for a child element at a specific position.
     * <p>
     * The position enables CSS pseudo-class matching for {@code :first-child},
     * {@code :last-child}, and {@code :nth-child(even/odd)}.
     * <p>
     * Example usage:
     * <pre>{@code
     * for (int i = 0; i < rows.size(); i++) {
     *     ChildPosition pos = ChildPosition.of(i, rows.size());
     *     Style rowStyle = context.childStyle("row", pos);
     *     // CSS can now match :first-child, :last-child, :nth-child(even), etc.
     * }
     * }</pre>
     *
     * @param childName the child name (e.g., "row", "cell")
     * @param position the position of the child within its siblings
     * @return the resolved style, merged with the current context style
     */
    default Style childStyle(String childName, ChildPosition position) {
        return childStyle(childName, position, dev.tamboui.css.cascade.PseudoClassState.NONE);
    }

    /**
     * Resolves CSS style for a child element at a specific position with additional state.
     * <p>
     * Combines positional pseudo-classes ({@code :first-child}, {@code :last-child},
     * {@code :nth-child}) with state pseudo-classes ({@code :selected}, {@code :hover}).
     * <p>
     * Example usage:
     * <pre>{@code
     * for (int i = 0; i < rows.size(); i++) {
     *     ChildPosition pos = ChildPosition.of(i, rows.size());
     *     boolean isSelected = (i == selectedIndex);
     *     PseudoClassState state = isSelected ? PseudoClassState.ofSelected() : PseudoClassState.NONE;
     *     Style rowStyle = context.childStyle("row", pos, state);
     * }
     * }</pre>
     *
     * @param childName the child name (e.g., "row", "cell")
     * @param position the position of the child within its siblings
     * @param state additional pseudo-class state (e.g., selected, hover)
     * @return the resolved style, merged with the current context style
     */
    default Style childStyle(String childName, ChildPosition position, dev.tamboui.css.cascade.PseudoClassState state) {
        return currentStyle();  // fallback when no CSS engine
    }

    /**
     * Renders a child element within the given area.
     * <p>
     * Container elements should use this method instead of calling
     * {@code child.render()} directly. This allows the infrastructure
     * to handle errors gracefully when fault-tolerant mode is enabled,
     * and ensures that all rendered elements are properly registered with
     * the event router, enabling them to receive key and mouse events.
     * <p>
     * Example usage in a container element:
     * <pre>{@code
     * for (Element child : children) {
     *     context.renderChild(child, frame, childArea);
     * }
     * }</pre>
     *
     * @param child the child element to render
     * @param frame the frame to render into
     * @param area the area allocated for the child
     */
    default void renderChild(Element child, Frame frame, Rect area) {
        child.render(frame, area, this);
    }

    /**
     * Creates an empty context for simple rendering without focus management.
     * Primarily useful for testing.
     *
     * @return an empty render context
     */
    static RenderContext empty() {
        return DefaultRenderContext.createEmpty();
    }
}

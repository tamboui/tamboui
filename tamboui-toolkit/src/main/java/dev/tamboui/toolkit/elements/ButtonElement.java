/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventHandler;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;

/**
 * A button element that wraps a {@link StyledElement} to provide built-in focus styles
 * (default reversed) and key event handling.
 */
public final class ButtonElement extends StyledElement<ButtonElement> {

    private StyledElement<?> innerElement;
    private Style innerElementStyle;
    private Style innerElementFocusedStyle;
    private EventHandler onPress;

    /**
     * Creates a button with the specified inner element.
     *
     * @param innerElement the inner element to be wrapped by the button
     */
    public ButtonElement(StyledElement<?> innerElement) {
        this.innerElement = innerElement;
        this.innerElementStyle = innerElement.getStyle();
        if (this.innerElementStyle.effectiveModifiers().contains(Modifier.REVERSED)) {
            this.innerElementFocusedStyle = innerElement.getStyle().notReversed();
        } else {
            this.innerElementFocusedStyle = innerElement.getStyle().reversed();
        }
        this.focusable();

        this.registerHandlers();
    }

    private void registerHandlers() {
        this.keyHandler =
            (KeyEvent event) -> {
                if (event.isConfirm()) {
                    return this.onPress.handle(event);
                }
                return EventResult.UNHANDLED;
            };
    }

    /**
     * Sets the button's onPress event handler.
     *
     * @param onPress the event handler to be called when the button is pressed
     * @return this button for chaining
     */
    public ButtonElement onPress(EventHandler onPress) {
        this.onPress = onPress;
        return this;
    }

    /**
     * Sets the style to be used for the inner element when focused.
     *
     * @param innerElementFocusedStyle the style to use
     * @return this button for chaining
     */
    public ButtonElement innerElementFocusedStyle(Style innerElementFocusedStyle) {
        this.innerElementFocusedStyle = innerElementFocusedStyle;
        return this;
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return this.innerElement.preferredSize(availableWidth, availableHeight, context);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        // Get current style from context (already resolved by StyledElement.render)
        Style effectiveStyle = context.currentStyle();

        // if focused, toggle style
        boolean isFocused = elementId != null && context.isFocused(elementId);
        if (isFocused) {
            if (effectiveStyle.effectiveModifiers().contains(Modifier.REVERSED)) {
                effectiveStyle.notReversed();
            } else {
                effectiveStyle.reversed();
            }

            this.innerElement.style(innerElementFocusedStyle);
        } else {
            this.innerElement.style(innerElementStyle);
        }

        // Build the block - CSS properties are resolved by the widget
        Block block = Block.builder().style(effectiveStyle).build();

        // Render the block
        frame.renderWidget(block, area);

        context.renderChild(this.innerElement, frame, area);
    }
}

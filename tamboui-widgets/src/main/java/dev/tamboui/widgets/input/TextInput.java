/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyKey;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardPropertyKeys;
import dev.tamboui.style.Style;
import dev.tamboui.style.StyledProperty;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widgets.block.Block;

/**
 * A text input widget for single-line text entry.
 */
public final class TextInput implements StatefulWidget<TextInputState> {

    /**
     * Property key for the cursor color.
     * <p>
     * CSS property name: {@code cursor-color}
     */
    public static final PropertyKey<Color> CURSOR_COLOR =
            PropertyKey.of("cursor-color", ColorConverter.INSTANCE);

    /**
     * Property key for the placeholder text color.
     * <p>
     * CSS property name: {@code placeholder-color}
     */
    public static final PropertyKey<Color> PLACEHOLDER_COLOR =
            PropertyKey.of("placeholder-color", ColorConverter.INSTANCE);

    private final Block block;
    private final Style style;
    private final Style cursorStyle;
    private final String placeholder;
    private final Style placeholderStyle;

    private TextInput(Builder builder) {
        this.block = builder.block;
        this.placeholder = builder.placeholder;

        // Resolve style-aware properties
        Color resolvedBg = builder.background.resolve();
        Color resolvedFg = builder.foreground.resolve();
        Color resolvedCursorColor = builder.cursorColor.resolve();
        Color resolvedPlaceholderColor = builder.placeholderColor.resolve();

        Style baseStyle = builder.style;
        if (resolvedBg != null) {
            baseStyle = baseStyle.bg(resolvedBg);
        }
        if (resolvedFg != null) {
            baseStyle = baseStyle.fg(resolvedFg);
        }
        this.style = baseStyle;

        Style baseCursorStyle = builder.cursorStyle;
        if (resolvedCursorColor != null) {
            baseCursorStyle = baseCursorStyle.bg(resolvedCursorColor);
        }
        this.cursorStyle = baseCursorStyle;

        Style basePlaceholderStyle = builder.placeholderStyle;
        if (resolvedPlaceholderColor != null) {
            basePlaceholderStyle = basePlaceholderStyle.fg(resolvedPlaceholderColor);
        }
        this.placeholderStyle = basePlaceholderStyle;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer, TextInputState state) {
        if (area.isEmpty()) {
            return;
        }

        // Apply background style
        buffer.setStyle(area, style);

        // Render block if present
        Rect inputArea = area;
        if (block != null) {
            block.render(area, buffer);
            inputArea = block.inner(area);
        }

        if (inputArea.isEmpty()) {
            return;
        }

        String text = state.text();
        int cursorPos = state.cursorPosition();

        // Show placeholder if empty
        if (text.isEmpty() && !placeholder.isEmpty()) {
            buffer.setString(inputArea.left(), inputArea.top(), placeholder, placeholderStyle);
            return;
        }

        // Calculate visible portion of text (for scrolling)
        int visibleWidth = inputArea.width();
        int scrollOffset = 0;

        // If cursor is beyond visible area, scroll
        if (cursorPos >= visibleWidth) {
            scrollOffset = cursorPos - visibleWidth + 1;
        }

        // Render visible text
        String visibleText = text.length() > scrollOffset
            ? text.substring(scrollOffset, Math.min(text.length(), scrollOffset + visibleWidth))
            : "";

        buffer.setString(inputArea.left(), inputArea.top(), visibleText, style);

        // Fill remaining space with empty styled cells
        int textEnd = inputArea.left() + visibleText.length();
        for (int x = textEnd; x < inputArea.right(); x++) {
            buffer.set(x, inputArea.top(), new Cell(" ", style));
        }
    }

    /**
     * Renders the widget and sets the cursor position on the frame.
     * Call this instead of render() when this input is focused.
     */
    public void renderWithCursor(Rect area, Buffer buffer, TextInputState state, Frame frame) {
        render(area, buffer, state);

        // Calculate cursor screen position
        Rect inputArea = block != null ? block.inner(area) : area;

        if (inputArea.isEmpty()) {
            return;
        }

        int cursorPos = state.cursorPosition();
        int visibleWidth = inputArea.width();
        int scrollOffset = cursorPos >= visibleWidth ? cursorPos - visibleWidth + 1 : 0;

        int cursorX = inputArea.left() + (cursorPos - scrollOffset);
        int cursorY = inputArea.top();

        // Render cursor as styled cell in buffer (avoids terminal cursor blink issues)
        if (inputArea.contains(cursorX, cursorY)) {
            Cell currentCell = buffer.get(cursorX, cursorY);
            buffer.set(cursorX, cursorY, currentCell.patchStyle(cursorStyle));
        }
    }

    public static final class Builder {
        private Block block;
        private Style style = Style.EMPTY;
        private Style cursorStyle = Style.EMPTY.reversed();
        private String placeholder = "";
        private Style placeholderStyle = Style.EMPTY.dim();
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties bound to this builder's resolver
        private final StyledProperty<Color> background =
                StyledProperty.of(StandardPropertyKeys.BACKGROUND, null, () -> styleResolver);
        private final StyledProperty<Color> foreground =
                StyledProperty.of(StandardPropertyKeys.COLOR, null, () -> styleResolver);
        private final StyledProperty<Color> cursorColor =
                StyledProperty.of(CURSOR_COLOR, null, () -> styleResolver);
        private final StyledProperty<Color> placeholderColor =
                StyledProperty.of(PLACEHOLDER_COLOR, null, () -> styleResolver);

        private Builder() {}

        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder cursorStyle(Style cursorStyle) {
            this.cursorStyle = cursorStyle;
            return this;
        }

        public Builder placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder placeholderStyle(Style placeholderStyle) {
            this.placeholderStyle = placeholderStyle;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code color}, {@code background},
         * {@code cursor-color}, and {@code placeholder-color} will be
         * resolved if not set programmatically.
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
         * Sets the foreground (text) color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the foreground color
         * @return this builder
         */
        public Builder foreground(Color color) {
            this.foreground.set(color);
            return this;
        }

        /**
         * Sets the cursor color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the cursor color
         * @return this builder
         */
        public Builder cursorColor(Color color) {
            this.cursorColor.set(color);
            return this;
        }

        /**
         * Sets the placeholder text color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the placeholder color
         * @return this builder
         */
        public Builder placeholderColor(Color color) {
            this.placeholderColor.set(color);
            return this;
        }

        public TextInput build() {
            return new TextInput(this);
        }
    }
}

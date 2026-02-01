/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
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
    public static final PropertyDefinition<Color> CURSOR_COLOR =
            PropertyDefinition.of("cursor-color", ColorConverter.INSTANCE);

    /**
     * Property key for the placeholder text color.
     * <p>
     * CSS property name: {@code placeholder-color}
     */
    public static final PropertyDefinition<Color> PLACEHOLDER_COLOR =
            PropertyDefinition.of("placeholder-color", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(CURSOR_COLOR, PLACEHOLDER_COLOR);
    }

    private final Block block;
    private final Style style;
    private final Style cursorStyle;
    private final String placeholder;
    private final Style placeholderStyle;

    private TextInput(Builder builder) {
        this.block = builder.block;
        this.placeholder = builder.placeholder;

        // Resolve style-aware properties
        Color resolvedBg = builder.resolveBackground();
        Color resolvedFg = builder.resolveForeground();
        Color resolvedCursorColor = builder.resolveCursorColor();
        Color resolvedPlaceholderColor = builder.resolvePlaceholderColor();

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

    /**
     * Creates a new text input builder.
     *
     * @return a new Builder
     */
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
            String visiblePlaceholder = CharWidth.substringByWidth(placeholder, inputArea.width());
            buffer.setString(inputArea.left(), inputArea.top(), visiblePlaceholder, placeholderStyle);
            return;
        }

        // Calculate visible portion of text (for scrolling)
        int visibleWidth = inputArea.width();

        // Calculate display width of text up to cursor
        String textBeforeCursor = text.substring(0, cursorPos);
        int widthBeforeCursor = CharWidth.of(textBeforeCursor);

        // If cursor is beyond visible area, scroll
        // scrollDisplayOffset tracks how many display columns to skip
        int scrollDisplayOffset = 0;
        if (widthBeforeCursor >= visibleWidth) {
            scrollDisplayOffset = widthBeforeCursor - visibleWidth + 1;
        }

        // Find the character index corresponding to scrollDisplayOffset
        int scrollCharOffset = 0;
        int widthCount = 0;
        while (scrollCharOffset < text.length() && widthCount < scrollDisplayOffset) {
            int cp = text.codePointAt(scrollCharOffset);
            widthCount += CharWidth.of(cp);
            scrollCharOffset += Character.charCount(cp);
        }

        // Render visible text starting from scrollCharOffset
        String textFromScroll = text.substring(scrollCharOffset);
        String visibleText = CharWidth.substringByWidth(textFromScroll, visibleWidth);

        buffer.setString(inputArea.left(), inputArea.top(), visibleText, style);

        // Fill remaining space with empty styled cells
        int visibleTextWidth = CharWidth.of(visibleText);
        int textEnd = inputArea.left() + visibleTextWidth;
        for (int x = textEnd; x < inputArea.right(); x++) {
            buffer.set(x, inputArea.top(), new Cell(" ", style));
        }
    }

    /**
     * Renders the widget and sets the cursor position on the frame.
     * Call this instead of render() when this input is focused.
     *
     * @param area   the area to render in
     * @param buffer the buffer to render to
     * @param state  the text input state
     * @param frame  the frame for cursor positioning
     */
    public void renderWithCursor(Rect area, Buffer buffer, TextInputState state, Frame frame) {
        render(area, buffer, state);

        // Calculate cursor screen position
        Rect inputArea = block != null ? block.inner(area) : area;

        if (inputArea.isEmpty()) {
            return;
        }

        String text = state.text();
        int cursorPos = state.cursorPosition();
        int visibleWidth = inputArea.width();

        // Calculate display width of text before cursor
        String textBeforeCursor = text.substring(0, cursorPos);
        int widthBeforeCursor = CharWidth.of(textBeforeCursor);

        // Calculate scroll offset in display columns
        int scrollDisplayOffset = widthBeforeCursor >= visibleWidth ? widthBeforeCursor - visibleWidth + 1 : 0;

        // Cursor X position is the display width before cursor minus scroll offset
        int cursorX = inputArea.left() + (widthBeforeCursor - scrollDisplayOffset);
        int cursorY = inputArea.top();

        // Render cursor as styled cell in buffer (avoids terminal cursor blink issues)
        if (inputArea.contains(cursorX, cursorY)) {
            Cell currentCell = buffer.get(cursorX, cursorY);
            buffer.set(cursorX, cursorY, currentCell.patchStyle(cursorStyle));

            // Also set terminal cursor position for inline displays
            if (frame != null) {
                frame.setCursorPosition(cursorX, cursorY);
            }
        }
    }

    /**
     * Builder for {@link TextInput}.
     */
    public static final class Builder {
        private Block block;
        private Style style = Style.EMPTY;
        private Style cursorStyle = Style.EMPTY.reversed();
        private String placeholder = "";
        private Style placeholderStyle = Style.EMPTY.dim();
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties (resolved via styleResolver in build())
        private Color background;
        private Color foreground;
        private Color cursorColor;
        private Color placeholderColor;

        private Builder() {}

        /**
         * Wraps the text input in a block.
         *
         * @param block the block to wrap in
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the base style.
         *
         * @param style the base style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the cursor style.
         *
         * @param cursorStyle the cursor style
         * @return this builder
         */
        public Builder cursorStyle(Style cursorStyle) {
            this.cursorStyle = cursorStyle;
            return this;
        }

        /**
         * Sets the placeholder text shown when the input is empty.
         *
         * @param placeholder the placeholder text
         * @return this builder
         */
        public Builder placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * Sets the placeholder text style.
         *
         * @param placeholderStyle the placeholder style
         * @return this builder
         */
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
            this.background = color;
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
            this.foreground = color;
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
            this.cursorColor = color;
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
            this.placeholderColor = color;
            return this;
        }

        /**
         * Builds the text input.
         *
         * @return a new TextInput
         */
        public TextInput build() {
            return new TextInput(this);
        }

        // Resolution helpers
        private Color resolveBackground() {
            return styleResolver.resolve(StandardProperties.BACKGROUND, background);
        }

        private Color resolveForeground() {
            return styleResolver.resolve(StandardProperties.COLOR, foreground);
        }

        private Color resolveCursorColor() {
            return styleResolver.resolve(CURSOR_COLOR, cursorColor);
        }

        private Color resolvePlaceholderColor() {
            return styleResolver.resolve(PLACEHOLDER_COLOR, placeholderColor);
        }
    }
}

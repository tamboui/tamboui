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
 * A text area widget for multi-line text entry.
 */
public final class TextArea implements StatefulWidget<TextAreaState> {

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

    /**
     * Property key for the line number gutter color.
     * <p>
     * CSS property name: {@code line-number-color}
     */
    public static final PropertyDefinition<Color> LINE_NUMBER_COLOR =
            PropertyDefinition.of("line-number-color", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(CURSOR_COLOR, PLACEHOLDER_COLOR, LINE_NUMBER_COLOR);
    }

    private final Block block;
    private final Style style;
    private final Style cursorStyle;
    private final String placeholder;
    private final Style placeholderStyle;
    private final boolean showLineNumbers;
    private final Style lineNumberStyle;

    private TextArea(Builder builder) {
        this.block = builder.block;
        this.placeholder = builder.placeholder;
        this.showLineNumbers = builder.showLineNumbers;

        // Resolve style-aware properties
        Color resolvedBg = builder.resolveBackground();
        Color resolvedFg = builder.resolveForeground();
        Color resolvedCursorColor = builder.resolveCursorColor();
        Color resolvedPlaceholderColor = builder.resolvePlaceholderColor();
        Color resolvedLineNumberColor = builder.resolveLineNumberColor();

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

        Style baseLineNumberStyle = builder.lineNumberStyle;
        if (resolvedLineNumberColor != null) {
            baseLineNumberStyle = baseLineNumberStyle.fg(resolvedLineNumberColor);
        }
        this.lineNumberStyle = baseLineNumberStyle;
    }

    /**
     * Creates a new text area builder.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer, TextAreaState state) {
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

        // Calculate line number gutter width
        int gutterWidth = 0;
        if (showLineNumbers) {
            int lineDigits = String.valueOf(state.lineCount()).length();
            gutterWidth = Math.max(2, lineDigits) + 2; // digits + space + separator
        }

        Rect textArea = inputArea;
        if (gutterWidth > 0 && inputArea.width() > gutterWidth) {
            textArea = new Rect(
                inputArea.left() + gutterWidth,
                inputArea.top(),
                inputArea.width() - gutterWidth,
                inputArea.height()
            );
        }

        String text = state.text();
        int visibleHeight = textArea.height();
        int visibleWidth = textArea.width();

        // Show placeholder if empty
        if (text.isEmpty() && !placeholder.isEmpty()) {
            buffer.setString(textArea.left(), textArea.top(), placeholder, placeholderStyle);
            return;
        }

        // Ensure cursor is visible
        state.ensureCursorVisible(visibleHeight, visibleWidth);

        // Render visible lines
        int scrollRow = state.scrollRow();
        int scrollCol = state.scrollCol();

        for (int y = 0; y < visibleHeight; y++) {
            int lineIndex = scrollRow + y;
            int screenY = textArea.top() + y;

            // Render line number if enabled
            if (showLineNumbers && gutterWidth > 0) {
                if (lineIndex < state.lineCount()) {
                    String lineNum = String.format("%" + (gutterWidth - 2) + "d ", lineIndex + 1);
                    buffer.setString(inputArea.left(), screenY, lineNum, lineNumberStyle);
                    buffer.set(inputArea.left() + gutterWidth - 1, screenY,
                        new Cell("|", lineNumberStyle));
                } else {
                    // Empty line number area
                    for (int x = inputArea.left(); x < inputArea.left() + gutterWidth; x++) {
                        buffer.set(x, screenY, new Cell(" ", lineNumberStyle));
                    }
                }
            }

            if (lineIndex < state.lineCount()) {
                String line = state.getLine(lineIndex);

                // Calculate visible portion of line
                // scrollCol is a character offset; convert to proper substring then truncate by width
                String visibleText = "";
                if (scrollCol < line.length()) {
                    String lineFromScroll = line.substring(scrollCol);
                    visibleText = CharWidth.substringByWidth(lineFromScroll, visibleWidth);
                }

                buffer.setString(textArea.left(), screenY, visibleText, style);

                // Fill remaining space
                int visibleTextWidth = CharWidth.of(visibleText);
                int textEnd = textArea.left() + visibleTextWidth;
                for (int x = textEnd; x < textArea.right(); x++) {
                    buffer.set(x, screenY, new Cell(" ", style));
                }
            } else {
                // Empty line below content
                for (int x = textArea.left(); x < textArea.right(); x++) {
                    buffer.set(x, screenY, new Cell(" ", style));
                }
            }
        }
    }

    /**
     * Renders the widget and sets the cursor position on the frame.
     * Call this instead of render() when this input is focused.
     *
     * @param area   the area to render in
     * @param buffer the buffer to render to
     * @param state  the text area state
     * @param frame  the frame for cursor positioning
     */
    public void renderWithCursor(Rect area, Buffer buffer, TextAreaState state, Frame frame) {
        render(area, buffer, state);

        // Calculate cursor screen position
        Rect inputArea = block != null ? block.inner(area) : area;

        if (inputArea.isEmpty()) {
            return;
        }

        int gutterWidth = 0;
        if (showLineNumbers) {
            int lineDigits = String.valueOf(state.lineCount()).length();
            gutterWidth = Math.max(2, lineDigits) + 2;
        }

        Rect textArea = inputArea;
        if (gutterWidth > 0 && inputArea.width() > gutterWidth) {
            textArea = new Rect(
                inputArea.left() + gutterWidth,
                inputArea.top(),
                inputArea.width() - gutterWidth,
                inputArea.height()
            );
        }

        int cursorRow = state.cursorRow();
        int cursorCol = state.cursorCol();
        int scrollRow = state.scrollRow();
        int scrollCol = state.scrollCol();

        // Check if cursor is visible
        int relativeRow = cursorRow - scrollRow;
        int relativeCol = cursorCol - scrollCol;

        if (relativeRow >= 0 && relativeRow < textArea.height() &&
            relativeCol >= 0 && relativeCol < textArea.width()) {

            int cursorX = textArea.left() + relativeCol;
            int cursorY = textArea.top() + relativeRow;

            // Render cursor as styled cell in buffer (avoids terminal cursor blink issues)
            Cell currentCell = buffer.get(cursorX, cursorY);
            buffer.set(cursorX, cursorY, currentCell.patchStyle(cursorStyle));
        }
    }

    /**
     * Builder for {@link TextArea}.
     */
    public static final class Builder {
        private Block block;
        private Style style = Style.EMPTY;
        private Style cursorStyle = Style.EMPTY.reversed();
        private String placeholder = "";
        private Style placeholderStyle = Style.EMPTY.dim();
        private boolean showLineNumbers = false;
        private Style lineNumberStyle = Style.EMPTY.dim();
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties (resolved via styleResolver in build())
        private Color background;
        private Color foreground;
        private Color cursorColor;
        private Color placeholderColor;
        private Color lineNumberColor;

        private Builder() {}

        /**
         * Wraps the text area in a block.
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
         * Sets the placeholder text shown when the text area is empty.
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
         * Sets whether to show line numbers.
         *
         * @param show true to show line numbers
         * @return this builder
         */
        public Builder showLineNumbers(boolean show) {
            this.showLineNumbers = show;
            return this;
        }

        /**
         * Sets the line number style.
         *
         * @param style the line number style
         * @return this builder
         */
        public Builder lineNumberStyle(Style style) {
            this.lineNumberStyle = style;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code color}, {@code background},
         * {@code cursor-color}, {@code placeholder-color}, and {@code line-number-color}
         * will be resolved if not set programmatically.
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
         * Sets the line number gutter color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the line number color
         * @return this builder
         */
        public Builder lineNumberColor(Color color) {
            this.lineNumberColor = color;
            return this;
        }

        /**
         * Builds the text area.
         *
         * @return a new TextArea
         */
        public TextArea build() {
            return new TextArea(this);
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

        private Color resolveLineNumberColor() {
            return styleResolver.resolve(LINE_NUMBER_COLOR, lineNumberColor);
        }
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widgets.block.Block;

/**
 * A text input widget for single-line text entry.
 */
public final class TextInput implements StatefulWidget<TextInputState> {

    private final Block block;
    private final Style style;
    private final Style cursorStyle;
    private final String placeholder;
    private final Style placeholderStyle;

    private TextInput(Builder builder) {
        this.block = builder.block;
        this.style = builder.style;
        this.cursorStyle = builder.cursorStyle;
        this.placeholder = builder.placeholder;
        this.placeholderStyle = builder.placeholderStyle;
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

        // Set cursor style at cursor position
        if (inputArea.contains(cursorX, cursorY)) {
            Cell currentCell = buffer.get(cursorX, cursorY);
            buffer.set(cursorX, cursorY, currentCell.patchStyle(cursorStyle));
            frame.setCursorPosition(new Position(cursorX, cursorY));
        }
    }

    public static final class Builder {
        private Block block;
        private Style style = Style.EMPTY;
        private Style cursorStyle = Style.EMPTY.reversed();
        private String placeholder = "";
        private Style placeholderStyle = Style.EMPTY.dim();

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

        public TextInput build() {
            return new TextInput(this);
        }
    }
}

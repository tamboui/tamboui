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
 * A text area widget for multi-line text entry.
 */
public final class TextArea implements StatefulWidget<TextAreaState> {

    private final Block block;
    private final Style style;
    private final Style cursorStyle;
    private final String placeholder;
    private final Style placeholderStyle;
    private final boolean showLineNumbers;
    private final Style lineNumberStyle;

    private TextArea(Builder builder) {
        this.block = builder.block;
        this.style = builder.style;
        this.cursorStyle = builder.cursorStyle;
        this.placeholder = builder.placeholder;
        this.placeholderStyle = builder.placeholderStyle;
        this.showLineNumbers = builder.showLineNumbers;
        this.lineNumberStyle = builder.lineNumberStyle;
    }

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
                String visibleText = "";
                if (scrollCol < line.length()) {
                    int end = Math.min(line.length(), scrollCol + visibleWidth);
                    visibleText = line.substring(scrollCol, end);
                }

                buffer.setString(textArea.left(), screenY, visibleText, style);

                // Fill remaining space
                int textEnd = textArea.left() + visibleText.length();
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

            // Set cursor style at cursor position
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
        private boolean showLineNumbers = false;
        private Style lineNumberStyle = Style.EMPTY.dim();

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

        public Builder showLineNumbers(boolean show) {
            this.showLineNumbers = show;
            return this;
        }

        public Builder lineNumberStyle(Style style) {
            this.lineNumberStyle = style;
            return this;
        }

        public TextArea build() {
            return new TextArea(this);
        }
    }
}

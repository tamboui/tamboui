/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.input.TextArea;
import dev.tamboui.widgets.input.TextAreaState;

/**
 * A DSL wrapper for the TextArea widget.
 * <p>
 * A multi-line text input field with scrolling support.
 * <pre>{@code
 * textArea(textState)
 *     .placeholder("Enter text...")
 *     .title("Description")
 *     .showLineNumbers()
 *     .rounded()
 * }</pre>
 */
public final class TextAreaElement extends StyledElement<TextAreaElement> {

    private TextAreaState state;
    private Style cursorStyle = Style.EMPTY.reversed();
    private String placeholder = "";
    private Style placeholderStyle = Style.EMPTY.dim();
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private Color focusedBorderColor;
    private boolean showCursor = true;
    private boolean showLineNumbers = false;
    private Style lineNumberStyle = Style.EMPTY.dim();
    private TextChangeListener changeListener;

    public TextAreaElement() {
        this.state = new TextAreaState();
    }

    public TextAreaElement(TextAreaState state) {
        this.state = state != null ? state : new TextAreaState();
    }

    /**
     * Sets the text area state.
     */
    public TextAreaElement state(TextAreaState state) {
        this.state = state != null ? state : new TextAreaState();
        return this;
    }

    /**
     * Returns the current state.
     */
    public TextAreaState getState() {
        return state;
    }

    /**
     * Sets the initial text.
     */
    public TextAreaElement text(String text) {
        if (state != null && text != null) {
            state.setText(text);
        }
        return this;
    }

    /**
     * Sets the placeholder text.
     */
    public TextAreaElement placeholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    /**
     * Sets the placeholder style.
     */
    public TextAreaElement placeholderStyle(Style style) {
        this.placeholderStyle = style;
        return this;
    }

    /**
     * Sets the placeholder color.
     */
    public TextAreaElement placeholderColor(Color color) {
        this.placeholderStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the cursor style.
     */
    public TextAreaElement cursorStyle(Style style) {
        this.cursorStyle = style;
        return this;
    }

    /**
     * Sets whether to show the cursor.
     */
    public TextAreaElement showCursor(boolean show) {
        this.showCursor = show;
        return this;
    }

    /**
     * Enables line number display.
     */
    public TextAreaElement showLineNumbers() {
        this.showLineNumbers = true;
        return this;
    }

    /**
     * Sets the line number style.
     */
    public TextAreaElement lineNumberStyle(Style style) {
        this.lineNumberStyle = style;
        return this;
    }

    /**
     * Sets the title for the border.
     */
    public TextAreaElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     */
    public TextAreaElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     */
    public TextAreaElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the border color to use when focused.
     * If not set, no special focused border styling is applied
     * (CSS :focused pseudo-class can be used instead).
     */
    public TextAreaElement focusedBorderColor(Color color) {
        this.focusedBorderColor = color;
        return this;
    }

    /**
     * Sets a listener for text changes.
     */
    public TextAreaElement onTextChange(TextChangeListener listener) {
        this.changeListener = listener;
        return this;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    /**
     * Handles a key event for text area input.
     */
    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (!focused) {
            return EventResult.UNHANDLED;
        }

        boolean handled = handleTextAreaKey(state, event);
        if (handled && changeListener != null) {
            changeListener.onTextChange(state.text());
        }
        return handled ? EventResult.HANDLED : EventResult.UNHANDLED;
    }

    /**
     * Handles common key events for text area input.
     */
    private static boolean handleTextAreaKey(TextAreaState state, KeyEvent event) {
        switch (event.code()) {
            case BACKSPACE:
                state.deleteBackward();
                return true;
            case DELETE:
                state.deleteForward();
                return true;
            case LEFT:
                state.moveCursorLeft();
                return true;
            case RIGHT:
                state.moveCursorRight();
                return true;
            case UP:
                state.moveCursorUp();
                return true;
            case DOWN:
                state.moveCursorDown();
                return true;
            case HOME:
                state.moveCursorToLineStart();
                return true;
            case END:
                state.moveCursorToLineEnd();
                return true;
            case ENTER:
                state.insert('\n');
                return true;
            case TAB:
                state.insert("    "); // 4 spaces for tab
                return true;
            case CHAR:
                // Don't consume characters with Ctrl or Alt modifiers - those are control sequences
                if (event.modifiers().ctrl() || event.modifiers().alt()) {
                    return false;
                }
                char c = event.character();
                if (c >= 32 && c < 127) {
                    state.insert(c);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        boolean isFocused = elementId != null && context.isFocused(elementId);

        TextArea.Builder builder = TextArea.builder()
            .style(context.currentStyle())
            .cursorStyle(cursorStyle)
            .placeholder(placeholder)
            .placeholderStyle(placeholderStyle)
            .showLineNumbers(showLineNumbers)
            .lineNumberStyle(lineNumberStyle);

        Color effectiveBorderColor = isFocused && focusedBorderColor != null
                ? focusedBorderColor
                : borderColor;

        if (title != null || borderType != null || effectiveBorderColor != null) {
            Block.Builder blockBuilder = Block.builder().borders(Borders.ALL);
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (effectiveBorderColor != null) {
                blockBuilder.borderStyle(Style.EMPTY.fg(effectiveBorderColor));
            }
            builder.block(blockBuilder.build());
        }

        TextArea widget = builder.build();

        if (showCursor && isFocused) {
            widget.renderWithCursor(area, frame.buffer(), state, frame);
        } else {
            frame.renderStatefulWidget(widget, area, state);
        }
    }

    /**
     * Listener for text changes in the text area.
     */
    @FunctionalInterface
    public interface TextChangeListener {
        void onTextChange(String newText);
    }
}

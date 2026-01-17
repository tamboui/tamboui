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
import dev.tamboui.toolkit.id.IdGenerator;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;

import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;

/**
 * A DSL wrapper for the TextInput widget.
 * <p>
 * A single-line text input field. This element is always focusable to receive
 * keyboard input for text editing.
 * <pre>{@code
 * textInput(inputState)
 *     .placeholder("Enter name...")
 *     .title("Name")
 *     .rounded()
 * }</pre>
 *
 * <h2>CSS Child Selectors</h2>
 * <p>
 * The following child selectors can be used to style sub-components:
 * <ul>
 *   <li>{@code TextInputElement-cursor} - The cursor style (default: reversed)</li>
 *   <li>{@code TextInputElement-placeholder} - The placeholder text style (default: dim)</li>
 * </ul>
 * <p>
 * Example CSS:
 * <pre>{@code
 * TextInputElement-cursor { text-style: reversed; background: yellow; }
 * TextInputElement-placeholder { color: gray; text-style: italic; }
 * }</pre>
 * <p>
 * Note: Programmatic styles set via {@link #cursorStyle(Style)} or {@link #placeholderStyle(Style)}
 * take precedence over CSS styles.
 */
public final class TextInputElement extends StyledElement<TextInputElement> {

    private static final Style DEFAULT_CURSOR_STYLE = Style.EMPTY.reversed();
    private static final Style DEFAULT_PLACEHOLDER_STYLE = Style.EMPTY.dim();

    private TextInputState state;
    private Style cursorStyle;
    private String placeholder = "";
    private Style placeholderStyle;
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private boolean showCursor = true;

    public TextInputElement() {
        this.state = new TextInputState();
        ensureId();
    }

    public TextInputElement(TextInputState state) {
        this.state = state != null ? state : new TextInputState();
        ensureId();
    }

    private void ensureId() {
        if (elementId == null) {
            elementId = IdGenerator.newId(this);
        }
    }

    /**
     * TextInputElement is always focusable to receive keyboard input.
     *
     * @return always returns true
     */
    @Override
    public boolean isFocusable() {
        return true;
    }

    /**
     * Sets the text input state.
     */
    public TextInputElement state(TextInputState state) {
        this.state = state != null ? state : new TextInputState();
        return this;
    }

    /**
     * Sets the initial text.
     */
    public TextInputElement text(String text) {
        if (state != null && text != null) {
            state.setText(text);
        }
        return this;
    }

    /**
     * Sets the placeholder text.
     */
    public TextInputElement placeholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    /**
     * Sets the placeholder style.
     */
    public TextInputElement placeholderStyle(Style style) {
        this.placeholderStyle = style;
        return this;
    }

    /**
     * Sets the placeholder color.
     */
    public TextInputElement placeholderColor(Color color) {
        this.placeholderStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the cursor style.
     */
    public TextInputElement cursorStyle(Style style) {
        this.cursorStyle = style;
        return this;
    }

    /**
     * Sets whether to show the cursor.
     */
    public TextInputElement showCursor(boolean show) {
        this.showCursor = show;
        return this;
    }

    /**
     * Sets the title for the border.
     */
    public TextInputElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     */
    public TextInputElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     */
    public TextInputElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Handles a key event for text input.
     * <p>
     * Handles: character input, backspace, delete, left/right arrows, home/end.
     * Only processes events when focused.
     *
     * @param event the key event
     * @param focused whether this element is currently focused
     * @return HANDLED if the event was processed, UNHANDLED otherwise
     */
    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (!focused) {
            return EventResult.UNHANDLED;
        }
        return handleTextInputKey(state, event) ? EventResult.HANDLED : EventResult.UNHANDLED;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Resolve styles with priority: explicit > CSS > default
        Style effectiveCursorStyle = resolveEffectiveStyle(context, "cursor", cursorStyle, DEFAULT_CURSOR_STYLE);
        Style effectivePlaceholderStyle = resolveEffectiveStyle(context, "placeholder", placeholderStyle, DEFAULT_PLACEHOLDER_STYLE);

        TextInput.Builder builder = TextInput.builder()
            .style(context.currentStyle())
            .cursorStyle(effectiveCursorStyle)
            .placeholder(placeholder)
            .placeholderStyle(effectivePlaceholderStyle);

        if (title != null || borderType != null) {
            Block.Builder blockBuilder = Block.builder().borders(Borders.ALL);
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (borderColor != null) {
                blockBuilder.borderStyle(Style.EMPTY.fg(borderColor));
            }
            builder.block(blockBuilder.build());
        }

        TextInput widget = builder.build();

        if (showCursor) {
            widget.renderWithCursor(area, frame.buffer(), state, frame);
        } else {
            frame.renderStatefulWidget(widget, area, state);
        }
    }
}

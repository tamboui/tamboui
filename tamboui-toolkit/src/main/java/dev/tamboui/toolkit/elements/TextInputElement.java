/*
 * Copyright TamboUI Contributors
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private boolean cursorRequiresFocus = true;
    private Runnable onSubmit;

    /** Creates a new text input element with a default state. */
    public TextInputElement() {
        this.state = new TextInputState();
    }

    /**
     * Creates a new text input element with the given state.
     *
     * @param state the text input state, or null for a default state
     */
    public TextInputElement(TextInputState state) {
        this.state = state != null ? state : new TextInputState();
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
     *
     * @param state the text input state
     * @return this builder
     */
    public TextInputElement state(TextInputState state) {
        this.state = state != null ? state : new TextInputState();
        return this;
    }

    /**
     * Sets the initial text.
     *
     * @param text the initial text content
     * @return this builder
     */
    public TextInputElement text(String text) {
        if (state != null && text != null) {
            state.setText(text);
        }
        return this;
    }

    /**
     * Sets the placeholder text.
     *
     * @param placeholder the placeholder text
     * @return this builder
     */
    public TextInputElement placeholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    /**
     * Sets the placeholder style.
     *
     * @param style the placeholder style
     * @return this builder
     */
    public TextInputElement placeholderStyle(Style style) {
        this.placeholderStyle = style;
        return this;
    }

    /**
     * Sets the placeholder color.
     *
     * @param color the placeholder color
     * @return this builder
     */
    public TextInputElement placeholderColor(Color color) {
        this.placeholderStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the cursor style.
     *
     * @param style the cursor style
     * @return this builder
     */
    public TextInputElement cursorStyle(Style style) {
        this.cursorStyle = style;
        return this;
    }

    /**
     * Sets whether to show the cursor.
     *
     * @param show true to show the cursor
     * @return this builder
     */
    public TextInputElement showCursor(boolean show) {
        this.showCursor = show;
        return this;
    }

    /**
     * Sets whether the cursor requires focus to be displayed.
     * <p>
     * When {@code true} (default), the cursor is only shown when this element is focused.
     * When {@code false}, the cursor is shown whenever {@link #showCursor(boolean)} is true,
     * regardless of focus state. This is useful when the text input is inside a focusable
     * container that handles key events manually.
     *
     * @param requiresFocus true to require focus for cursor display
     * @return this element for chaining
     */
    public TextInputElement cursorRequiresFocus(boolean requiresFocus) {
        this.cursorRequiresFocus = requiresFocus;
        return this;
    }

    /**
     * Sets the title for the border.
     *
     * @param title the border title
     * @return this builder
     */
    public TextInputElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this builder
     */
    public TextInputElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this builder
     */
    public TextInputElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    @Override
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (title != null) {
            attrs.put("title", title);
        }
        if (placeholder != null && !placeholder.isEmpty()) {
            attrs.put("placeholder", placeholder);
        }
        return Collections.unmodifiableMap(attrs);
    }

    /**
     * Sets a callback to be invoked when Enter is pressed.
     * <p>
     * Use this for form submission or to move to the next field.
     *
     * @param onSubmit the callback to invoke on Enter
     * @return this element for chaining
     */
    public TextInputElement onSubmit(Runnable onSubmit) {
        this.onSubmit = onSubmit;
        return this;
    }

    /**
     * Handles a key event for text input.
     * <p>
     * Handles: character input, backspace, delete, left/right arrows, home/end.
     * Enter triggers the onSubmit callback if set.
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
        // Handle Enter key - call onSubmit callback if set
        if (event.isConfirm() && onSubmit != null) {
            onSubmit.run();
            return EventResult.HANDLED;
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
            Block.Builder blockBuilder = Block.builder()
                    .borders(Borders.ALL)
                    .styleResolver(styleResolver(context));
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (borderColor != null) {
                blockBuilder.borderColor(borderColor);
            }
            builder.block(blockBuilder.build());
        }

        TextInput widget = builder.build();

        // Determine if cursor should be shown
        boolean isFocused = elementId != null && context.isFocused(elementId);
        boolean shouldShowCursor = showCursor && (!cursorRequiresFocus || isFocused);
        if (shouldShowCursor) {
            widget.renderWithCursor(area, frame.buffer(), state, frame);
        } else {
            frame.renderStatefulWidget(widget, area, state);
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.PasteEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.input.TextArea;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.syntax.RegexSyntaxHighlighter;
import dev.tamboui.widgets.syntax.SyntaxHighlighter;
import dev.tamboui.widgets.syntax.SyntaxTheme;

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
 *
 * <h2>CSS Child Selectors</h2>
 * <p>
 * The following child selectors can be used to style sub-components:
 * <ul>
 *   <li>{@code TextAreaElement-cursor} - The cursor style (default: reversed)</li>
 *   <li>{@code TextAreaElement-placeholder} - The placeholder text style (default: dim)</li>
 *   <li>{@code TextAreaElement-line-number} - The line number style (default: dim)</li>
 * </ul>
 * <p>
 * The {@code text-overflow} CSS property selects the overflow mode ({@code clip},
 * {@code wrap-word}, {@code wrap-character}); a programmatic {@link #overflow(Overflow)}
 * value takes precedence. Ellipsis values fall back to clip for text areas.</p>
 * <p>
 * Example CSS:
 * <pre>{@code
 * TextAreaElement-cursor { text-style: reversed; background: cyan; }
 * TextAreaElement-placeholder { color: gray; text-style: italic; }
 * TextAreaElement-line-number { color: #666666; }
 * }</pre>
 * <p>
 * Note: Programmatic styles set via the corresponding setter methods take precedence over CSS styles.
 */
public final class TextAreaElement extends StyledElement<TextAreaElement> {

    private static final Style DEFAULT_CURSOR_STYLE = Style.EMPTY.reversed();
    private static final Style DEFAULT_PLACEHOLDER_STYLE = Style.EMPTY.dim();
    private static final Style DEFAULT_LINE_NUMBER_STYLE = Style.EMPTY.dim();

    private TextAreaState state;
    private Style cursorStyle;
    private String placeholder = "";
    private Style placeholderStyle;
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private Color focusedBorderColor;
    private boolean showCursor = true;
    private boolean showLineNumbers = false;
    private Style lineNumberStyle;
    private Overflow overflow;
    private Overflow renderedOverflow;
    private SyntaxHighlighter highlighter;
    private String highlightLanguage;
    private SyntaxTheme highlightTheme;
    private TextChangeListener changeListener;

    /** Creates a new text area element with a default state. */
    public TextAreaElement() {
        this.state = new TextAreaState();
    }

    /**
     * Creates a new text area element with the given state.
     *
     * @param state the text area state, or null for a default state
     */
    public TextAreaElement(TextAreaState state) {
        this.state = state != null ? state : new TextAreaState();
    }

    /**
     * Sets the text area state.
     *
     * @param state the text area state
     * @return this builder
     */
    public TextAreaElement state(TextAreaState state) {
        this.state = state != null ? state : new TextAreaState();
        return this;
    }

    /**
     * Returns the current state.
     *
     * @return the text area state
     */
    public TextAreaState getState() {
        return state;
    }

    /**
     * Sets the initial text.
     *
     * @param text the initial text content
     * @return this builder
     */
    public TextAreaElement text(String text) {
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
    public TextAreaElement placeholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    /**
     * Sets the placeholder style.
     *
     * @param style the placeholder style
     * @return this builder
     */
    public TextAreaElement placeholderStyle(Style style) {
        this.placeholderStyle = style;
        return this;
    }

    /**
     * Sets the placeholder color.
     *
     * @param color the placeholder color
     * @return this builder
     */
    public TextAreaElement placeholderColor(Color color) {
        this.placeholderStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the cursor style.
     *
     * @param style the cursor style
     * @return this builder
     */
    public TextAreaElement cursorStyle(Style style) {
        this.cursorStyle = style;
        return this;
    }

    /**
     * Sets whether to show the cursor.
     *
     * @param show true to show the cursor
     * @return this builder
     */
    public TextAreaElement showCursor(boolean show) {
        this.showCursor = show;
        return this;
    }

    /**
     * Enables line number display.
     *
     * @return this builder
     */
    public TextAreaElement showLineNumbers() {
        this.showLineNumbers = true;
        return this;
    }

    /**
     * Sets the line number style.
     *
     * @param style the line number style
     * @return this builder
     */
    public TextAreaElement lineNumberStyle(Style style) {
        this.lineNumberStyle = style;
        return this;
    }

    /**
     * Sets the overflow (wrap) mode.
     * <p>
     * {@link Overflow#CLIP} (the default) preserves horizontal-scroll behavior. {@code
     * WRAP_WORD}/{@code WRAP_CHARACTER} wrap long lines across multiple screen rows instead,
     * and Up/Down move the cursor by visual row rather than logical line. The truncating modes
     * ({@code ELLIPSIS}, {@code ELLIPSIS_START}, {@code ELLIPSIS_MIDDLE}) would hide text the
     * caret can still reach, so a text area falls back to {@code CLIP} for them.
     *
     * @param overflow the overflow mode
     * @return this builder
     */
    public TextAreaElement overflow(Overflow overflow) {
        this.overflow = overflow;
        return this;
    }

    /**
     * Disables wrapping (horizontal scroll instead). This is the default.
     *
     * @return this builder
     */
    public TextAreaElement clip() {
        return overflow(Overflow.CLIP);
    }

    /**
     * Wraps long lines at word boundaries.
     *
     * @return this builder
     */
    public TextAreaElement wrapWord() {
        return overflow(Overflow.WRAP_WORD);
    }

    /**
     * Wraps long lines at character boundaries.
     *
     * @return this builder
     */
    public TextAreaElement wrapCharacter() {
        return overflow(Overflow.WRAP_CHARACTER);
    }

    /**
     * Enables syntax highlighting with the built-in highlighter and default theme.
     *
     * @param language the language identifier or alias (e.g. {@code java}, {@code css})
     * @return this builder
     */
    public TextAreaElement highlighter(String language) {
        return highlighter(RegexSyntaxHighlighter.defaults(), language);
    }

    /**
     * Enables syntax highlighting with the built-in highlighter and a custom theme.
     *
     * @param language the language identifier or alias
     * @param theme the token palette
     * @return this builder
     */
    public TextAreaElement highlighter(String language, SyntaxTheme theme) {
        this.highlighter = RegexSyntaxHighlighter.defaults();
        this.highlightLanguage = language;
        this.highlightTheme = Objects.requireNonNull(theme, "theme");
        return this;
    }

    /**
     * Enables syntax highlighting of the content (clip mode only; wrapped
     * modes render unstyled). See {@link TextArea.Builder#highlighter}.
     *
     * @param highlighter the highlighter (e.g. {@code RegexSyntaxHighlighter.defaults()})
     * @param language the language identifier or alias (e.g. {@code java}, {@code css})
     * @return this builder
     */
    public TextAreaElement highlighter(SyntaxHighlighter highlighter, String language) {
        this.highlighter = highlighter;
        this.highlightLanguage = language;
        this.highlightTheme = null;
        return this;
    }

    /**
     * Sets the title for the border.
     *
     * @param title the border title
     * @return this builder
     */
    public TextAreaElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this builder
     */
    public TextAreaElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this builder
     */
    public TextAreaElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the border color to use when focused.
     * If not set, no special focused border styling is applied
     * (CSS :focused pseudo-class can be used instead).
     *
     * @param color the focused border color
     * @return this builder
     */
    public TextAreaElement focusedBorderColor(Color color) {
        this.focusedBorderColor = color;
        return this;
    }

    /**
     * Sets a listener for text changes.
     *
     * @param listener the text change listener
     * @return this builder
     */
    public TextAreaElement onTextChange(TextChangeListener listener) {
        this.changeListener = listener;
        return this;
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        // Calculate max line width from content
        int maxWidth = 0;
        int lineCount = 1;
        if (state != null) {
            String text = state.text();
            for (String line : text.split("\n", -1)) {
                maxWidth = Math.max(maxWidth, line.length());
            }
            lineCount = text.isEmpty() ? 1 : text.split("\n", -1).length;
        }
        // Add minimum width, line number width if applicable, and border
        int lineNumWidth = showLineNumbers ? 5 : 0;
        int borderWidth = (title != null || borderType != null) ? 2 : 0;
        int width = Math.max(maxWidth, 20) + lineNumWidth + borderWidth;
        // Add border height
        int borderHeight = (title != null || borderType != null) ? 2 : 0;
        int height = lineCount + borderHeight;

        // Wrapped lines take more than one row each; ask the widget, which knows the width its
        // border and line-number gutter leave for text.
        Overflow effectiveOverflow = resolveOverflow(context);
        if (availableWidth > 0 && state != null
                && (effectiveOverflow == Overflow.WRAP_WORD || effectiveOverflow == Overflow.WRAP_CHARACTER)) {
            boolean isFocused = context != null && elementId != null && context.isFocused(elementId);
            height = TextArea.builder()
                    .showLineNumbers(showLineNumbers)
                    .overflow(effectiveOverflow)
                    .block(buildBlock(context, isFocused))
                    .build()
                    .preferredHeight(availableWidth, state);
        }
        return Size.of(width, height);
    }

    @Override
    public boolean isFocusable() {
        return true;
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
     * Handles a key event for text area input.
     * <p>
     * Note: The {@code focused} parameter is informational only.
     * If the event reached this element, it should be processed.
     */
    @Override
    public EventResult handlePasteEvent(PasteEvent event) {
        state.insert(event.text());
        if (changeListener != null) {
            changeListener.onTextChange(state.text());
        }
        return EventResult.HANDLED;
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Text input requires focus - only handle events when focused
        // to avoid multiple text areas in the same container all receiving input
        if (!focused) {
            return EventResult.UNHANDLED;
        }
        // Wrap against the width the widget last rendered with, so Up/Down move by the same
        // visual rows the user is looking at.
        boolean handled = handleTextAreaKey(state, event, state.lastRenderedWidth(),
                renderedOverflow != null ? renderedOverflow : overflow);
        if (handled && changeListener != null) {
            changeListener.onTextChange(state.text());
        }
        return handled ? EventResult.HANDLED : EventResult.UNHANDLED;
    }

    /**
     * Handles common key events for text area input.
     */
    private static boolean handleTextAreaKey(TextAreaState state, KeyEvent event, int visibleWidth, Overflow overflow) {
        return dev.tamboui.toolkit.Toolkit.handleTextAreaKey(state, event, visibleWidth, overflow);
    }

    /**
     * Resolves the overflow mode: programmatic value takes precedence, then the
     * {@code text-overflow} CSS property, then {@link Overflow#CLIP}. The resolved
     * value is also used by key handling (Up/Down by visual row), which caches the
     * value seen at the last render since no CSS context is available at event time.
     */
    private Overflow resolveOverflow(RenderContext context) {
        if (overflow != null) {
            return overflow;
        }
        if (context != null) {
            return context.resolveStyle(this)
                    .flatMap(resolver -> resolver.get(StandardProperties.TEXT_OVERFLOW))
                    .orElse(Overflow.CLIP);
        }
        return Overflow.CLIP;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        boolean isFocused = elementId != null && context.isFocused(elementId);

        // Resolve styles with priority: explicit > CSS > default
        Overflow effectiveOverflow = resolveOverflow(context);
        renderedOverflow = effectiveOverflow;
        Style effectiveCursorStyle = resolveEffectiveStyle(context, "cursor", cursorStyle, DEFAULT_CURSOR_STYLE);
        Style effectivePlaceholderStyle = resolveEffectiveStyle(context, "placeholder", placeholderStyle, DEFAULT_PLACEHOLDER_STYLE);
        Style effectiveLineNumberStyle = resolveEffectiveStyle(context, "line-number", lineNumberStyle, DEFAULT_LINE_NUMBER_STYLE);

        TextArea.Builder builder = TextArea.builder()
            .style(context.currentStyle())
            .cursorStyle(effectiveCursorStyle)
            .placeholder(placeholder)
            .placeholderStyle(effectivePlaceholderStyle)
            .showLineNumbers(showLineNumbers)
            .lineNumberStyle(effectiveLineNumberStyle)
            .overflow(effectiveOverflow)
            .block(buildBlock(context, isFocused));

        if (highlighter != null) {
            SyntaxTheme effectiveTheme = highlightTheme != null ? highlightTheme : resolveSyntaxTheme(context);
            builder.highlighter(highlighter, highlightLanguage, effectiveTheme);
        }

        TextArea widget = builder.build();

        // The widget records the content width it used on the state; see
        // TextAreaState.lastRenderedWidth(), which key handling reads back.
        if (showCursor && isFocused) {
            widget.renderWithCursor(area, frame.buffer(), state, frame);
        } else {
            frame.renderStatefulWidget(widget, area, state);
        }
    }

    /** Returns the border block to draw around the text, or null when there is none. */
    private Block buildBlock(RenderContext context, boolean isFocused) {
        Color effectiveBorderColor = isFocused && focusedBorderColor != null
                ? focusedBorderColor
                : borderColor;

        if (title == null && borderType == null && effectiveBorderColor == null) {
            return null;
        }
        Block.Builder blockBuilder = Block.builder()
                .borders(Borders.ALL)
                .styleResolver(context != null ? styleResolver(context) : null);
        if (title != null) {
            blockBuilder.title(Title.from(title));
        }
        if (borderType != null) {
            blockBuilder.borderType(borderType);
        }
        if (effectiveBorderColor != null) {
            blockBuilder.borderColor(effectiveBorderColor);
        }
        return blockBuilder.build();
    }

    /**
     * Listener for text changes in the text area.
     */
    @FunctionalInterface
    public interface TextChangeListener {
        /**
         * Called when the text content changes.
         *
         * @param newText the new text content
         */
        void onTextChange(String newText);
    }
}

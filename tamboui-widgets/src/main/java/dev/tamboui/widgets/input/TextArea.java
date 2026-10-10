/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.syntax.RegexSyntaxHighlighter;
import dev.tamboui.widgets.syntax.SyntaxHighlighter;
import dev.tamboui.widgets.syntax.SyntaxTheme;

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
    private final Overflow overflow;
    private final SyntaxHighlighter highlighter;
    private final String highlightLanguage;
    private final SyntaxTheme highlightTheme;

    private TextArea(Builder builder) {
        this.block = builder.block;
        this.placeholder = builder.placeholder;
        this.showLineNumbers = builder.showLineNumbers;
        this.overflow = builder.resolveOverflow();

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
        this.highlighter = builder.highlighter;
        this.highlightLanguage = builder.highlightLanguage;
        this.highlightTheme = builder.highlightTheme;
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

        int gutterWidth = gutterWidth(state);
        Rect textArea = textAreaRect(inputArea, gutterWidth);

        String text = state.text();
        int visibleHeight = textArea.height();
        int visibleWidth = textArea.width();

        // Publish the width actually used, so callers outside the render pass (key handling in
        // the toolkit element, for one) wrap against the same value instead of recomputing it.
        state.lastRenderedWidth(visibleWidth);

        // Show placeholder if empty
        if (text.isEmpty() && !placeholder.isEmpty()) {
            String visiblePlaceholder = CharWidth.substringByWidth(placeholder, textArea.width());
            buffer.setString(textArea.left(), textArea.top(), visiblePlaceholder, placeholderStyle);
            return;
        }

        // Ensure cursor is visible
        state.ensureCursorVisible(visibleHeight, visibleWidth, overflow);

        if (TextAreaState.isWrapping(overflow)) {
            renderWrapped(inputArea, textArea, gutterWidth, buffer, state, visibleHeight, visibleWidth);
        } else {
            renderClipped(inputArea, textArea, gutterWidth, buffer, state, visibleHeight, visibleWidth);
        }
    }

    /** Width of the line-number gutter: digits + space + separator, or 0 when hidden. */
    private int gutterWidth(TextAreaState state) {
        if (!showLineNumbers) {
            return 0;
        }
        int lineDigits = String.valueOf(state.lineCount()).length();
        return Math.max(2, lineDigits) + 2;
    }

    /** The area left for text inside {@code inputArea} once the gutter is carved off. */
    private static Rect textAreaRect(Rect inputArea, int gutterWidth) {
        if (gutterWidth <= 0 || inputArea.width() <= gutterWidth) {
            return inputArea;
        }
        return new Rect(
            inputArea.left() + gutterWidth,
            inputArea.top(),
            inputArea.width() - gutterWidth,
            inputArea.height()
        );
    }

    private void renderClipped(Rect inputArea, Rect textArea, int gutterWidth, Buffer buffer, TextAreaState state,
                                int visibleHeight, int visibleWidth) {
        int scrollRow = state.scrollRow();
        int scrollCol = state.scrollCol();

        // With a highlighter configured, style the full text once per render; the
        // highlighter emits one Line per logical line. Rows are mapped by index
        // with a per-line plain fallback, since conventions for a trailing
        // newline differ (the state counts a final empty line, highlighters
        // typically trim it).
        List<Line> styledLines = highlighter != null
            ? highlighter.highlight(state.text(), highlightLanguage, style, highlightTheme)
            : null;

        for (int y = 0; y < visibleHeight; y++) {
            int lineIndex = scrollRow + y;
            int screenY = textArea.top() + y;
            boolean hasLine = lineIndex < state.lineCount();

            renderGutterCell(inputArea, gutterWidth, screenY, hasLine ? lineIndex + 1 : -1, buffer);

            if (hasLine) {
                String line = state.getLine(lineIndex);

                Line styledLine = styledLines != null && lineIndex < styledLines.size()
                    ? styledLines.get(lineIndex)
                    : null;

                int textEnd;
                if (styledLine != null) {
                    textEnd = renderStyledLine(buffer, textArea.left(), screenY,
                        styledLine, scrollCol, visibleWidth);
                } else {
                    // Calculate visible portion of line
                    // scrollCol is a character offset; convert to proper substring then truncate by width
                    String visibleText = "";
                    if (scrollCol < line.length()) {
                        String lineFromScroll = line.substring(scrollCol);
                        visibleText = CharWidth.substringByWidth(lineFromScroll, visibleWidth);
                    }

                    buffer.setString(textArea.left(), screenY, visibleText, style);
                    textEnd = textArea.left() + CharWidth.of(visibleText);
                }

                // Fill remaining space
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

    private void renderWrapped(Rect inputArea, Rect textArea, int gutterWidth, Buffer buffer, TextAreaState state,
                                int visibleHeight, int visibleWidth) {
        List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(visibleWidth, overflow);
        int scrollRow = state.scrollRow();

        for (int y = 0; y < visibleHeight; y++) {
            int rowIndex = scrollRow + y;
            int screenY = textArea.top() + y;

            if (rowIndex < rows.size()) {
                TextAreaState.DisplayRow row = rows.get(rowIndex);
                String line = state.getLine(row.logicalRow());
                String visibleText = line.substring(row.startCol(), row.endCol());

                // Only the first wrapped row of a logical line shows its line number.
                int lineNumber = row.startCol() == 0 ? row.logicalRow() + 1 : -1;
                renderGutterCell(inputArea, gutterWidth, screenY, lineNumber, buffer);

                buffer.setString(textArea.left(), screenY, visibleText, style);

                int visibleTextWidth = CharWidth.of(visibleText);
                int textEnd = textArea.left() + visibleTextWidth;
                for (int x = textEnd; x < textArea.right(); x++) {
                    buffer.set(x, screenY, new Cell(" ", style));
                }
            } else {
                renderGutterCell(inputArea, gutterWidth, screenY, -1, buffer);
                for (int x = textArea.left(); x < textArea.right(); x++) {
                    buffer.set(x, screenY, new Cell(" ", style));
                }
            }
        }
    }

    /**
     * Renders a styled line from character offset {@code fromChar}, truncated to
     * {@code maxWidth} display columns, walking spans so token styles survive
     * horizontal scrolling.
     *
     * @return the buffer column after the last rendered character
     */
    private static int renderStyledLine(Buffer buffer, int x, int y, Line line, int fromChar, int maxWidth) {
        int col = x;
        int skip = fromChar;
        int remaining = maxWidth;
        for (Span span : line.spans()) {
            String content = span.content();
            if (skip >= content.length()) {
                skip -= content.length();
                continue;
            }
            String piece = skip > 0 ? content.substring(skip) : content;
            skip = 0;
            String fit = CharWidth.substringByWidth(piece, remaining);
            if (fit.isEmpty()) {
                break;
            }
            col = buffer.setString(col, y, fit, span.style());
            remaining -= CharWidth.of(fit);
            if (remaining <= 0) {
                break;
            }
        }
        return col;
    }

    private void renderGutterCell(Rect inputArea, int gutterWidth, int screenY, int lineNumber, Buffer buffer) {
        if (!showLineNumbers || gutterWidth <= 0) {
            return;
        }
        if (lineNumber > 0) {
            String lineNum = String.format("%" + (gutterWidth - 2) + "d ", lineNumber);
            buffer.setString(inputArea.left(), screenY, lineNum, lineNumberStyle);
            buffer.set(inputArea.left() + gutterWidth - 1, screenY, new Cell("|", lineNumberStyle));
        } else {
            for (int x = inputArea.left(); x < inputArea.left() + gutterWidth; x++) {
                buffer.set(x, screenY, new Cell(" ", lineNumberStyle));
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

        Rect textArea = textAreaRect(inputArea, gutterWidth(state));

        int scrollRow = state.scrollRow();
        int scrollCol = state.scrollCol();

        int relativeRow;
        int relativeCol;
        if (TextAreaState.isWrapping(overflow)) {
            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(textArea.width(), overflow);
            int cursorDisplayIndex = state.findCursorDisplayRowIndex(rows, textArea.width());
            TextAreaState.DisplayRow row = rows.get(cursorDisplayIndex);

            relativeRow = cursorDisplayIndex - scrollRow;
            String cursorLine = state.getLine(row.logicalRow());
            // A cursor snapped forward past a word-wrap break sits before this row's start.
            int cursorCol = Math.max(row.startCol(), state.cursorCol());
            relativeCol = CharWidth.of(cursorLine.substring(row.startCol(), cursorCol));
        } else {
            int cursorRow = state.cursorRow();
            int cursorCol = state.cursorCol();

            // Check if cursor is visible
            relativeRow = cursorRow - scrollRow;
            // Convert char-offset cursor column to display column relative to scroll
            if (cursorCol < scrollCol) {
                relativeCol = -1; // cursor is left of the viewport
            } else {
                String cursorLine = state.getLine(cursorRow);
                int from = Math.min(scrollCol, cursorLine.length());
                int to = Math.min(cursorCol, cursorLine.length());
                relativeCol = CharWidth.of(cursorLine.substring(from, to));
            }
        }

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
     * Returns the height, in rows, needed to show all of {@code state}'s text without scrolling
     * when rendered {@code width} columns wide: one row per display row (see
     * {@link TextAreaState#computeDisplayRows}), plus the rows taken by the block, if any.
     * <p>
     * This uses the same border and gutter math as {@link #render}, so layout code can size a
     * wrapping text area without re-deriving it.
     *
     * @param width the width of the area the widget will be rendered into
     * @param state the text area state
     * @return the height that fits the whole text
     */
    public int preferredHeight(int width, TextAreaState state) {
        // Tall enough that the block's insets are the only thing taken off the height.
        Rect area = new Rect(0, 0, width, Short.MAX_VALUE);
        Rect inputArea = block != null ? block.inner(area) : area;
        int textWidth = textAreaRect(inputArea, gutterWidth(state)).width();
        int rows = state.computeDisplayRows(textWidth, overflow).size();
        return rows + area.height() - inputArea.height();
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
        private Overflow overflow;
        private SyntaxHighlighter highlighter;
        private String highlightLanguage;
        private SyntaxTheme highlightTheme = SyntaxTheme.DEFAULTS;
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
         * Sets the overflow (wrap) mode, mirroring {@code Paragraph.Builder.overflow}.
         * <p>
         * {@link Overflow#CLIP} (the default) preserves today's horizontal-scroll behavior.
         * {@code WRAP_WORD}/{@code WRAP_CHARACTER} wrap long lines across multiple screen rows
         * instead of scrolling horizontally. The truncating modes ({@code ELLIPSIS},
         * {@code ELLIPSIS_START}, {@code ELLIPSIS_MIDDLE}) would hide text the caret can still
         * reach, so a text area falls back to {@code CLIP} for them.
         *
         * @param overflow the overflow mode
         * @return this builder
         */
        public Builder overflow(Overflow overflow) {
            this.overflow = overflow;
            return this;
        }

        /**
         * Enables syntax highlighting with the built-in highlighter and default theme.
         *
         * @param language the language identifier or alias (e.g. {@code java}, {@code css})
         * @return this builder
         */
        public Builder highlighter(String language) {
            return highlighter(RegexSyntaxHighlighter.defaults(), language, SyntaxTheme.DEFAULTS);
        }

        /**
         * Enables syntax highlighting with the built-in highlighter and a custom theme.
         *
         * @param language the language identifier or alias
         * @param theme the token palette
         * @return this builder
         */
        public Builder highlighter(String language, SyntaxTheme theme) {
            return highlighter(RegexSyntaxHighlighter.defaults(), language, theme);
        }

        /**
         * Enables syntax highlighting of the text area content using the
         * {@link SyntaxTheme#DEFAULTS default theme}.
         * <p>
         * The full text is highlighted on each render, so this is intended for
         * code-editor-sized content; combine with the highlighter's max line
         * length guard for untrusted input. Highlighting currently applies in
         * {@link Overflow#CLIP} mode only; wrapped modes render unstyled.
         *
         * @param highlighter the highlighter (e.g. {@code RegexSyntaxHighlighter.defaults()})
         * @param language the language identifier or alias (e.g. {@code java}, {@code css})
         * @return this builder
         */
        public Builder highlighter(SyntaxHighlighter highlighter, String language) {
            return highlighter(highlighter, language, SyntaxTheme.DEFAULTS);
        }

        /**
         * Enables syntax highlighting of the text area content with a custom theme.
         *
         * @param highlighter the highlighter
         * @param language the language identifier or alias
         * @param theme the token palette
         * @return this builder
         */
        public Builder highlighter(SyntaxHighlighter highlighter, String language, SyntaxTheme theme) {
            this.highlighter = highlighter;
            this.highlightLanguage = language;
            this.highlightTheme = theme;
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

        private Overflow resolveOverflow() {
            Overflow resolved = styleResolver.resolve(StandardProperties.TEXT_OVERFLOW, overflow);
            // Normalize here so the rest of the widget only ever sees a mode it implements;
            // `text-overflow: ellipsis` from a shared stylesheet must not silently char-wrap.
            return TextAreaState.isWrapping(resolved) ? resolved : Overflow.CLIP;
        }
    }
}

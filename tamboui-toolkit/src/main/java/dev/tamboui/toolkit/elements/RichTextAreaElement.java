/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.RichTextState;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.common.ScrollBarPolicy;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

/**
 * A toolkit element for displaying styled text with scrolling support.
 * <p>
 * This element provides internal scroll state management and keyboard/mouse event handling.
 * It accepts pre-styled {@link Text} objects, enabling syntax highlighting, markup rendering,
 * and other rich text features.
 * <p>
 * Unlike {@link TextElement}, this element:
 * <ul>
 *   <li>Accepts {@link Text} objects with multiple styled {@link Span}s</li>
 *   <li>Supports vertical scrolling with automatic scroll state</li>
 *   <li>Handles keyboard navigation (UP/DOWN/PAGE_UP/PAGE_DOWN/HOME/END)</li>
 *   <li>Handles mouse scroll events</li>
 *   <li>Optionally displays line numbers</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * import static dev.tamboui.toolkit.Toolkit.*;
 *
 * Text styledText = Text.from(
 *     Line.from(Span.styled("function", keywordStyle), Span.raw(" foo() {")),
 *     Line.from(Span.raw("    return "), Span.styled("42", numberStyle), Span.raw(";")),
 *     Line.from(Span.raw("}"))
 * );
 *
 * richTextArea(styledText)
 *     .overflow(Overflow.WRAP_WORD)
 *     .showLineNumbers()
 *     .scrollbar(ScrollBarPolicy.AS_NEEDED)
 * }</pre>
 *
 * <h2>CSS Child Selectors</h2>
 * <ul>
 *   <li>{@code RichTextAreaElement-line-number} - The line number style (default: dim)</li>
 *   <li>{@code RichTextAreaElement-scrollbar-thumb} - The scrollbar thumb style</li>
 *   <li>{@code RichTextAreaElement-scrollbar-track} - The scrollbar track style</li>
 * </ul>
 *
 * @see Text for creating styled text
 * @see RichTextState for scroll state management
 */
public final class RichTextAreaElement extends StyledElement<RichTextAreaElement> {

    private static final Style DEFAULT_LINE_NUMBER_STYLE = Style.EMPTY.dim();

    private Text text;
    private Overflow overflow;
    private Alignment alignment;
    private boolean showLineNumbers;
    private Style lineNumberStyle;
    private String lineNumberSeparator = " ";
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private ScrollBarPolicy scrollBarPolicy = ScrollBarPolicy.NONE;
    private Color scrollbarThumbColor;
    private Color scrollbarTrackColor;
    private Color focusedBorderColor;

    // Internal scroll state
    private final RichTextState state;

    /**
     * Creates a RichTextAreaElement with empty text.
     */
    public RichTextAreaElement() {
        this(Text.empty());
    }

    /**
     * Creates a RichTextAreaElement from a string.
     *
     * @param content the text content
     */
    public RichTextAreaElement(String content) {
        this(content != null ? Text.from(content) : Text.empty());
    }

    /**
     * Creates a RichTextAreaElement from styled text.
     *
     * @param text the styled text to display
     */
    public RichTextAreaElement(Text text) {
        this.text = text != null ? text : Text.empty();
        this.state = new RichTextState();
    }

    /**
     * Sets the text to display.
     *
     * @param text the styled text
     * @return this element for chaining
     */
    public RichTextAreaElement text(Text text) {
        this.text = text != null ? text : Text.empty();
        return this;
    }

    /**
     * Sets the text from a string.
     *
     * @param content the text content
     * @return this element for chaining
     */
    public RichTextAreaElement text(String content) {
        this.text = content != null ? Text.from(content) : Text.empty();
        return this;
    }

    /**
     * Sets the overflow mode.
     *
     * @param overflow the overflow mode
     * @return this element for chaining
     */
    public RichTextAreaElement overflow(Overflow overflow) {
        this.overflow = overflow;
        return this;
    }

    /**
     * Convenience method to clip text at boundaries.
     *
     * @return this element for chaining
     */
    public RichTextAreaElement clip() {
        this.overflow = Overflow.CLIP;
        return this;
    }

    /**
     * Convenience method to wrap at word boundaries.
     *
     * @return this element for chaining
     */
    public RichTextAreaElement wrapWord() {
        this.overflow = Overflow.WRAP_WORD;
        return this;
    }

    /**
     * Convenience method to wrap at character boundaries.
     *
     * @return this element for chaining
     */
    public RichTextAreaElement wrapCharacter() {
        this.overflow = Overflow.WRAP_CHARACTER;
        return this;
    }

    /**
     * Convenience method to truncate with ellipsis at end.
     *
     * @return this element for chaining
     */
    public RichTextAreaElement ellipsis() {
        this.overflow = Overflow.ELLIPSIS;
        return this;
    }

    /**
     * Sets the text alignment.
     *
     * @param alignment the alignment
     * @return this element for chaining
     */
    public RichTextAreaElement alignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * Centers the text horizontally.
     *
     * @return this element for chaining
     */
    public RichTextAreaElement centered() {
        this.alignment = Alignment.CENTER;
        return this;
    }

    /**
     * Aligns text to the right.
     *
     * @return this element for chaining
     */
    public RichTextAreaElement right() {
        this.alignment = Alignment.RIGHT;
        return this;
    }

    /**
     * Enables line numbers.
     *
     * @return this element for chaining
     */
    public RichTextAreaElement showLineNumbers() {
        this.showLineNumbers = true;
        return this;
    }

    /**
     * Sets whether to show line numbers.
     *
     * @param show true to show line numbers
     * @return this element for chaining
     */
    public RichTextAreaElement showLineNumbers(boolean show) {
        this.showLineNumbers = show;
        return this;
    }

    /**
     * Sets the line number style.
     *
     * @param style the style for line numbers
     * @return this element for chaining
     */
    public RichTextAreaElement lineNumberStyle(Style style) {
        this.lineNumberStyle = style;
        return this;
    }

    /**
     * Sets the separator between line numbers and content.
     *
     * @param separator the separator string
     * @return this element for chaining
     */
    public RichTextAreaElement lineNumberSeparator(String separator) {
        this.lineNumberSeparator = separator != null ? separator : " ";
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the title text
     * @return this element for chaining
     */
    public RichTextAreaElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element for chaining
     */
    public RichTextAreaElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border type.
     *
     * @param borderType the border type
     * @return this element for chaining
     */
    public RichTextAreaElement borderType(BorderType borderType) {
        this.borderType = borderType;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element for chaining
     */
    public RichTextAreaElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Enables a scrollbar (always visible).
     *
     * @return this element for chaining
     */
    public RichTextAreaElement scrollbar() {
        this.scrollBarPolicy = ScrollBarPolicy.ALWAYS;
        return this;
    }

    /**
     * Sets the scrollbar policy.
     *
     * @param policy the scrollbar display policy
     * @return this element for chaining
     */
    public RichTextAreaElement scrollbar(ScrollBarPolicy policy) {
        this.scrollBarPolicy = policy != null ? policy : ScrollBarPolicy.NONE;
        return this;
    }

    /**
     * Sets the scrollbar thumb color.
     *
     * @param color the thumb color
     * @return this element for chaining
     */
    public RichTextAreaElement scrollbarThumbColor(Color color) {
        this.scrollbarThumbColor = color;
        return this;
    }

    /**
     * Sets the scrollbar track color.
     *
     * @param color the track color
     * @return this element for chaining
     */
    public RichTextAreaElement scrollbarTrackColor(Color color) {
        this.scrollbarTrackColor = color;
        return this;
    }

    /**
     * Sets the border color when the element is focused.
     *
     * @param color the focused border color
     * @return this element for chaining
     */
    public RichTextAreaElement focusedBorderColor(Color color) {
        this.focusedBorderColor = color;
        return this;
    }

    /**
     * Scrolls to show the specified line.
     *
     * @param line the line number (0-based)
     * @return this element for chaining
     */
    public RichTextAreaElement scrollToLine(int line) {
        state.scrollToLine(line);
        return this;
    }

    /**
     * Ensures the specified line is visible, scrolling if necessary.
     *
     * @param line the line number (0-based)
     * @return this element for chaining
     */
    public RichTextAreaElement ensureLineVisible(int line) {
        state.ensureLineVisible(line);
        return this;
    }

    /**
     * Returns the internal scroll state for programmatic control.
     *
     * @return the scroll state
     */
    public RichTextState state() {
        return state;
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        // Calculate width: max line width from text
        int maxWidth = 0;
        for (Line line : text.lines()) {
            maxWidth = Math.max(maxWidth, line.width());
        }
        // Add line number width and border
        int lineNumWidth = showLineNumbers ? String.valueOf(text.lines().size()).length() + lineNumberSeparator.length() : 0;
        int borderWidth = (title != null || borderType != null) ? 2 : 0;
        int width = maxWidth + lineNumWidth + borderWidth;

        // Calculate height
        int height;
        Overflow effectiveOverflow = overflow != null ? overflow : Overflow.CLIP;
        if (availableWidth > 0 && (effectiveOverflow == Overflow.WRAP_CHARACTER || effectiveOverflow == Overflow.WRAP_WORD)) {
            // Calculate wrapped height
            int totalLines = 0;
            for (Line line : text.lines()) {
                int lineWidth = line.width();
                if (lineWidth <= availableWidth || availableWidth <= 0) {
                    totalLines++;
                } else {
                    totalLines += (lineWidth + availableWidth - 1) / availableWidth;
                }
            }
            height = Math.max(1, totalLines);
        } else {
            height = text.height();
        }

        return Size.of(width, height);
    }

    @Override
    public Constraint constraint() {
        if (layoutConstraint != null) {
            return layoutConstraint;
        }
        // For wrapping modes, use min constraint to allow growth for wrapped text
        Overflow currentOverflow = overflow != null ? overflow : Overflow.CLIP;
        if (currentOverflow == Overflow.WRAP_CHARACTER || currentOverflow == Overflow.WRAP_WORD) {
            return Constraint.min(text.height());
        }
        return null;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Render border/block if needed
        Rect contentArea = area;
        if (title != null || borderType != null || focusedBorderColor != null) {
            boolean isFocused = elementId != null && context.isFocused(elementId);
            Color effectiveBorderColor = isFocused && focusedBorderColor != null
                    ? focusedBorderColor
                    : borderColor;

            Block.Builder blockBuilder = Block.builder()
                    .borders(Borders.ALL)
                    .styleResolver(styleResolver(context));
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (effectiveBorderColor != null) {
                blockBuilder.borderColor(effectiveBorderColor);
            }
            Block block = blockBuilder.build();
            block.render(area, frame.buffer());
            contentArea = block.inner(area);
        }

        if (contentArea.isEmpty()) {
            return;
        }

        // Calculate line number width if showing line numbers
        int lineNumberWidth = 0;
        if (showLineNumbers) {
            int totalLines = text.lines().size();
            lineNumberWidth = String.valueOf(totalLines).length() + lineNumberSeparator.length();
        }

        // Determine if we should show scrollbar
        boolean reserveScrollbarSpace = scrollBarPolicy == ScrollBarPolicy.ALWAYS
                || (scrollBarPolicy == ScrollBarPolicy.AS_NEEDED && text.height() > contentArea.height());

        // Calculate the actual rendering area for text (excluding scrollbar and line numbers)
        Rect textRenderArea = contentArea;
        if (reserveScrollbarSpace && contentArea.width() > 1) {
            textRenderArea = new Rect(
                contentArea.left(),
                contentArea.top(),
                contentArea.width() - 1,
                contentArea.height()
            );
        }

        // Calculate line number area and text content area
        Rect lineNumberArea = null;
        Rect textContentArea = textRenderArea;
        if (showLineNumbers && lineNumberWidth > 0 && textRenderArea.width() > lineNumberWidth) {
            lineNumberArea = new Rect(
                textRenderArea.left(),
                textRenderArea.top(),
                lineNumberWidth,
                textRenderArea.height()
            );
            textContentArea = new Rect(
                textRenderArea.left() + lineNumberWidth,
                textRenderArea.top(),
                textRenderArea.width() - lineNumberWidth,
                textRenderArea.height()
            );
        }

        // Update state with content dimensions
        state.setContentHeight(text.lines().size());
        state.setContentWidth(maxLineWidth(text.lines()));
        state.setViewportHeight(textContentArea.height());
        state.setViewportWidth(textContentArea.width());

        // Get visible lines based on scroll position
        int scrollRow = state.scrollRow();
        int startLine = Math.min(scrollRow, text.lines().size());
        int endLine = Math.min(startLine + textContentArea.height(), text.lines().size());

        // Resolve line number style
        Style effectiveLineNumberStyle = resolveEffectiveStyle(
            context, "line-number", lineNumberStyle, DEFAULT_LINE_NUMBER_STYLE);

        // Render line numbers if enabled
        if (showLineNumbers && lineNumberArea != null) {
            int lineNumDigits = lineNumberWidth - lineNumberSeparator.length();
            for (int i = 0; i < endLine - startLine; i++) {
                int lineIndex = startLine + i;
                int y = lineNumberArea.top() + i;
                String lineNum = String.format("%" + lineNumDigits + "d%s",
                    lineIndex + 1, lineNumberSeparator);
                frame.buffer().setString(lineNumberArea.left(), y, lineNum, effectiveLineNumberStyle);
            }
        }

        // Create visible text for Paragraph (only the lines that should be visible)
        Text visibleText;
        if (startLine == 0 && endLine == text.lines().size()) {
            visibleText = text;
        } else {
            visibleText = Text.from(text.lines().subList(startLine, endLine));
        }

        // Build and render the Paragraph widget
        Paragraph.Builder paragraphBuilder = Paragraph.builder()
                .text(visibleText)
                .style(context.currentStyle())
                .styleResolver(styleResolver(context));

        if (overflow != null) {
            paragraphBuilder.overflow(overflow);
        }
        if (alignment != null) {
            paragraphBuilder.alignment(alignment);
        }

        Paragraph paragraph = paragraphBuilder.build();
        paragraph.render(textContentArea, frame.buffer());

        // Render scrollbar if needed
        boolean showScrollbar = scrollBarPolicy == ScrollBarPolicy.ALWAYS
                || (scrollBarPolicy == ScrollBarPolicy.AS_NEEDED && state.isScrollable());

        if (showScrollbar && contentArea.width() > 1) {
            Rect scrollbarArea = new Rect(
                contentArea.right() - 1,
                contentArea.top(),
                1,
                contentArea.height()
            );

            ScrollbarState scrollbarState = new ScrollbarState()
                .contentLength(state.contentHeight())
                .viewportContentLength(state.viewportHeight())
                .position(state.scrollRow());

            // Resolve scrollbar styles
            Style explicitThumbStyle = scrollbarThumbColor != null ? Style.EMPTY.fg(scrollbarThumbColor) : null;
            Style explicitTrackStyle = scrollbarTrackColor != null ? Style.EMPTY.fg(scrollbarTrackColor) : null;
            Style thumbStyle = resolveEffectiveStyle(context, "scrollbar-thumb", explicitThumbStyle, Style.EMPTY);
            Style trackStyle = resolveEffectiveStyle(context, "scrollbar-track", explicitTrackStyle, Style.EMPTY);

            Scrollbar.Builder scrollbarBuilder = Scrollbar.builder()
                .orientation(ScrollbarOrientation.VERTICAL_RIGHT);
            if (!thumbStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.thumbStyle(thumbStyle);
            }
            if (!trackStyle.equals(Style.EMPTY)) {
                scrollbarBuilder.trackStyle(trackStyle);
            }

            frame.renderStatefulWidget(scrollbarBuilder.build(), scrollbarArea, scrollbarState);
        }
    }

    /**
     * Returns the maximum width of any line in the list.
     */
    private int maxLineWidth(java.util.List<Line> lines) {
        int max = 0;
        for (Line line : lines) {
            max = Math.max(max, line.width());
        }
        return max;
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        EventResult result = super.handleKeyEvent(event, focused);
        if (result.isHandled()) {
            return result;
        }

        if (!focused) {
            return EventResult.UNHANDLED;
        }

        if (event.matches(Actions.MOVE_UP)) {
            state.scrollUp();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.MOVE_DOWN)) {
            state.scrollDown();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.PAGE_UP)) {
            state.pageUp();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.PAGE_DOWN)) {
            state.pageDown();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.HOME)) {
            state.scrollToTop();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.END)) {
            state.scrollToBottom();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.MOVE_LEFT)) {
            state.scrollLeft();
            return EventResult.HANDLED;
        }

        if (event.matches(Actions.MOVE_RIGHT)) {
            state.scrollRight();
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        EventResult result = super.handleMouseEvent(event);
        if (result.isHandled()) {
            return result;
        }

        if (event.kind() == MouseEventKind.SCROLL_UP) {
            state.scrollUp(3);
            return EventResult.HANDLED;
        }

        if (event.kind() == MouseEventKind.SCROLL_DOWN) {
            state.scrollDown(3);
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }
}

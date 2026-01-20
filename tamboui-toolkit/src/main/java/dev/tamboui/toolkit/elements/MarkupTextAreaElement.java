/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.RenderContext;
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
import dev.tamboui.widgets.scrollbar.Scrollbar;
import dev.tamboui.widgets.scrollbar.ScrollbarOrientation;
import dev.tamboui.widgets.scrollbar.ScrollbarState;
import dev.tamboui.widgets.text.Overflow;
import dev.tamboui.widgets.text.RichTextState;

import java.util.ArrayList;
import java.util.List;


/**
 * A toolkit element that parses BBCode-style markup and displays styled text as Elements.
 * <p>
 * This element parses markup and renders each styled segment as a separate {@link TextElement},
 * allowing TFX effects to target individual segments using CSS class selectors. Tag names
 * from the markup become CSS classes on the corresponding TextElements.
 * <p>
 * Supported markup:
 * <ul>
 *   <li>Built-in modifiers: {@code [bold]}, {@code [italic]}, {@code [underlined]},
 *       {@code [dim]}, {@code [reversed]}, {@code [crossed-out]}</li>
 *   <li>Built-in colors: {@code [red]}, {@code [green]}, {@code [blue]}, {@code [yellow]},
 *       {@code [cyan]}, {@code [magenta]}, {@code [white]}, {@code [black]}, {@code [gray]}</li>
 *   <li>Custom tags: unknown tags become CSS classes, e.g., {@code [looping]} becomes
 *       {@code addClass("looping")} on the TextElement</li>
 *   <li>Escaped brackets: {@code [[} produces {@code [}, and {@code ]]} produces {@code ]}</li>
 *   <li>Nested tags: {@code [red][bold]text[/bold][/red]}</li>
 * </ul>
 * <p>
 * Example usage with TFX effects:
 * <pre>{@code
 * import static dev.tamboui.toolkit.Toolkit.*;
 *
 * // Tag names become CSS classes for TFX targeting
 * markupTextArea("""
 *     [gray]Words with [/gray][looping]looping[/looping][gray] colors[/gray]
 *     [gray]and smooth [/gray][fx]effects[/fx]
 *     """)
 *     .title("Effects Demo")
 *     .rounded()
 *
 * // TFX effects target by CSS class
 * effects.addEffectBySelector(".looping", Fx.fadeTo(...).pingPong());
 * effects.addEffectBySelector(".fx", Fx.fadeTo(...).pingPong());
 * }</pre>
 *
 * @see MarkupParser for markup syntax details
 */
public final class MarkupTextAreaElement extends StyledElement<MarkupTextAreaElement> {

    /**
     * Policy for displaying the scrollbar.
     */
    public enum ScrollBarPolicy {
        /** Never show the scrollbar. */
        NONE,
        /** Always show the scrollbar. */
        ALWAYS,
        /** Show the scrollbar only when content exceeds the viewport. */
        AS_NEEDED
    }

    private static final Style DEFAULT_LINE_NUMBER_STYLE = Style.EMPTY.dim();

    private String markup;
    private MarkupParser.StyleResolver customResolver;
    private Text parsedText;
    private boolean textDirty = true;

    // Rendering options
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
     * Creates a MarkupTextAreaElement with empty content.
     */
    public MarkupTextAreaElement() {
        this("");
    }

    /**
     * Creates a MarkupTextAreaElement with the given markup content.
     *
     * @param markup the markup text to parse and display
     */
    public MarkupTextAreaElement(String markup) {
        this.markup = markup != null ? markup : "";
        this.state = new RichTextState();
    }

    /**
     * Sets the markup content.
     *
     * @param markup the markup text to parse and display
     * @return this element for chaining
     */
    public MarkupTextAreaElement markup(String markup) {
        this.markup = markup != null ? markup : "";
        this.textDirty = true;
        return this;
    }

    /**
     * Sets a custom style resolver for tags not covered by built-in styles.
     *
     * @param resolver the custom resolver
     * @return this element for chaining
     */
    public MarkupTextAreaElement customResolver(MarkupParser.StyleResolver resolver) {
        this.customResolver = resolver;
        this.textDirty = true;
        return this;
    }

    /**
     * Sets the overflow mode.
     *
     * @param overflow the overflow mode
     * @return this element for chaining
     */
    public MarkupTextAreaElement overflow(Overflow overflow) {
        this.overflow = overflow;
        return this;
    }

    /**
     * Convenience method to clip text at boundaries.
     *
     * @return this element for chaining
     */
    public MarkupTextAreaElement clip() {
        this.overflow = Overflow.CLIP;
        return this;
    }

    /**
     * Convenience method to wrap at word boundaries.
     *
     * @return this element for chaining
     */
    public MarkupTextAreaElement wrapWord() {
        this.overflow = Overflow.WRAP_WORD;
        return this;
    }

    /**
     * Convenience method to wrap at character boundaries.
     *
     * @return this element for chaining
     */
    public MarkupTextAreaElement wrapCharacter() {
        this.overflow = Overflow.WRAP_CHARACTER;
        return this;
    }

    /**
     * Convenience method to truncate with ellipsis at end.
     *
     * @return this element for chaining
     */
    public MarkupTextAreaElement ellipsis() {
        this.overflow = Overflow.ELLIPSIS;
        return this;
    }

    /**
     * Sets the text alignment.
     *
     * @param alignment the alignment
     * @return this element for chaining
     */
    public MarkupTextAreaElement alignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * Centers the text horizontally.
     *
     * @return this element for chaining
     */
    public MarkupTextAreaElement centered() {
        this.alignment = Alignment.CENTER;
        return this;
    }

    /**
     * Aligns text to the right.
     *
     * @return this element for chaining
     */
    public MarkupTextAreaElement right() {
        this.alignment = Alignment.RIGHT;
        return this;
    }

    /**
     * Enables line numbers.
     *
     * @return this element for chaining
     */
    public MarkupTextAreaElement showLineNumbers() {
        this.showLineNumbers = true;
        return this;
    }

    /**
     * Sets whether to show line numbers.
     *
     * @param show true to show line numbers
     * @return this element for chaining
     */
    public MarkupTextAreaElement showLineNumbers(boolean show) {
        this.showLineNumbers = show;
        return this;
    }

    /**
     * Sets the line number style.
     *
     * @param style the style for line numbers
     * @return this element for chaining
     */
    public MarkupTextAreaElement lineNumberStyle(Style style) {
        this.lineNumberStyle = style;
        return this;
    }

    /**
     * Sets the separator between line numbers and content.
     *
     * @param separator the separator string
     * @return this element for chaining
     */
    public MarkupTextAreaElement lineNumberSeparator(String separator) {
        this.lineNumberSeparator = separator != null ? separator : " ";
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the title text
     * @return this element for chaining
     */
    public MarkupTextAreaElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element for chaining
     */
    public MarkupTextAreaElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border type.
     *
     * @param borderType the border type
     * @return this element for chaining
     */
    public MarkupTextAreaElement borderType(BorderType borderType) {
        this.borderType = borderType;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element for chaining
     */
    public MarkupTextAreaElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the border color when the element is focused.
     *
     * @param color the focused border color
     * @return this element for chaining
     */
    public MarkupTextAreaElement focusedBorderColor(Color color) {
        this.focusedBorderColor = color;
        return this;
    }

    /**
     * Enables a scrollbar (always visible).
     *
     * @return this element for chaining
     */
    public MarkupTextAreaElement scrollbar() {
        this.scrollBarPolicy = ScrollBarPolicy.ALWAYS;
        return this;
    }

    /**
     * Sets the scrollbar policy.
     *
     * @param policy the scrollbar display policy
     * @return this element for chaining
     */
    public MarkupTextAreaElement scrollbar(ScrollBarPolicy policy) {
        this.scrollBarPolicy = policy != null ? policy : ScrollBarPolicy.NONE;
        return this;
    }

    /**
     * Sets the scrollbar policy using RichTextAreaElement's policy type.
     *
     * @param policy the scrollbar display policy
     * @return this element for chaining
     */
    public MarkupTextAreaElement scrollbar(RichTextAreaElement.ScrollBarPolicy policy) {
        if (policy == null) {
            this.scrollBarPolicy = ScrollBarPolicy.NONE;
            return this;
        }
        switch (policy) {
            case NONE:
                this.scrollBarPolicy = ScrollBarPolicy.NONE;
                break;
            case ALWAYS:
                this.scrollBarPolicy = ScrollBarPolicy.ALWAYS;
                break;
            case AS_NEEDED:
                this.scrollBarPolicy = ScrollBarPolicy.AS_NEEDED;
                break;
            default:
                this.scrollBarPolicy = ScrollBarPolicy.NONE;
                break;
        }
        return this;
    }

    /**
     * Sets the scrollbar thumb color.
     *
     * @param color the thumb color
     * @return this element for chaining
     */
    public MarkupTextAreaElement scrollbarThumbColor(Color color) {
        this.scrollbarThumbColor = color;
        return this;
    }

    /**
     * Sets the scrollbar track color.
     *
     * @param color the track color
     * @return this element for chaining
     */
    public MarkupTextAreaElement scrollbarTrackColor(Color color) {
        this.scrollbarTrackColor = color;
        return this;
    }

    /**
     * Scrolls to show the specified line.
     *
     * @param line the line number (0-based)
     * @return this element for chaining
     */
    public MarkupTextAreaElement scrollToLine(int line) {
        state.scrollToLine(line);
        return this;
    }

    /**
     * Ensures the specified line is visible, scrolling if necessary.
     *
     * @param line the line number (0-based)
     * @return this element for chaining
     */
    public MarkupTextAreaElement ensureLineVisible(int line) {
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

    /**
     * Returns the parsed text (useful for inspection or modification).
     *
     * @return the parsed text
     */
    public Text parsedText() {
        ensureTextParsed();
        return parsedText;
    }

    @Override
    public Constraint constraint() {
        if (layoutConstraint != null) {
            return layoutConstraint;
        }
        // For wrapping modes, use min constraint to allow growth for wrapped text
        ensureTextParsed();
        Overflow currentOverflow = overflow != null ? overflow : Overflow.CLIP;
        if (currentOverflow == Overflow.WRAP_CHARACTER || currentOverflow == Overflow.WRAP_WORD) {
            return Constraint.min(parsedText.height());
        }
        return null;
    }

    @Override
    public int preferredHeight() {
        ensureTextParsed();
        return parsedText.height();
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        ensureTextParsed();
        Overflow effectiveOverflow = overflow != null ? overflow : Overflow.CLIP;
        if (effectiveOverflow != Overflow.WRAP_CHARACTER && effectiveOverflow != Overflow.WRAP_WORD) {
            return parsedText.height();
        }

        // Calculate wrapped height
        int totalLines = 0;
        for (Line line : parsedText.lines()) {
            int lineWidth = line.width();
            if (lineWidth <= availableWidth || availableWidth <= 0) {
                totalLines++;
            } else {
                totalLines += (lineWidth + availableWidth - 1) / availableWidth;
            }
        }
        return Math.max(1, totalLines);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Parse markup with the combined resolver (including context for TCSS)
        MarkupParser.StyleResolver combinedResolver = createCombinedResolver(context);
        parsedText = MarkupParser.parse(markup, combinedResolver);
        textDirty = false;

        // Get lines from parsed text
        List<Line> parsedLines = parsedText.lines();

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
            int totalLines = parsedLines.size();
            lineNumberWidth = String.valueOf(totalLines).length() + lineNumberSeparator.length();
        }

        // Determine if we should show scrollbar
        boolean reserveScrollbarSpace = scrollBarPolicy == ScrollBarPolicy.ALWAYS
                || (scrollBarPolicy == ScrollBarPolicy.AS_NEEDED && parsedLines.size() > contentArea.height());

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
        state.setContentHeight(parsedLines.size());
        state.setContentWidth(maxLineWidth(parsedLines));
        state.setViewportHeight(textContentArea.height());
        state.setViewportWidth(textContentArea.width());

        // Get visible lines based on scroll position
        int scrollRow = state.scrollRow();
        int startLine = Math.min(scrollRow, parsedLines.size());
        int endLine = Math.min(startLine + textContentArea.height(), parsedLines.size());

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

        // Build and render element-based content
        renderElementBasedContent(frame, textContentArea, context, parsedLines, startLine, endLine);

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
     * Renders the parsed content as Element-based rendering.
     * Each span becomes a TextElement with CSS classes for TFX targeting.
     */
    private void renderElementBasedContent(Frame frame,
                                           Rect area,
                                           RenderContext context,
                                           List<Line> parsedLines,
                                           int startLine,
                                           int endLine) {
        if (area.isEmpty()) {
            return;
        }

        // For word wrapping modes, we need to handle wrapping across the visible lines
        if (overflow == Overflow.WRAP_WORD || overflow == Overflow.WRAP_CHARACTER) {
            renderWrappedContent(frame, area, context, parsedLines, startLine, endLine);
            return;
        }

        // Calculate heights for visible lines
        int[] heights = new int[endLine - startLine];
        for (int i = 0; i < heights.length; i++) {
            heights[i] = 1; // Each line takes 1 row
        }

        // Split area vertically for each visible line
        Constraint[] constraints = new Constraint[heights.length];
        for (int i = 0; i < heights.length; i++) {
            constraints[i] = Constraint.length(heights[i]);
        }

        List<Rect> lineAreas = Layout.vertical()
            .constraints(constraints)
            .split(area);

        // Render each visible line
        for (int i = 0; i < lineAreas.size() && (startLine + i) < endLine; i++) {
            int lineIndex = startLine + i;
            Line line = parsedLines.get(lineIndex);
            Rect lineArea = lineAreas.get(i);

            renderLine(frame, lineArea, context, line);
        }
    }

    /**
     * Renders content with word wrapping support.
     */
    private void renderWrappedContent(Frame frame,
                                      Rect area,
                                      RenderContext context,
                                      List<Line> parsedLines,
                                      int startLine,
                                      int endLine) {
        if (area.isEmpty()) {
            return;
        }

        int availableWidth = area.width();
        int currentY = area.top();
        int maxY = area.top() + area.height();

        // Process each visible source line
        for (int lineIndex = startLine; lineIndex < endLine && currentY < maxY; lineIndex++) {
            Line line = parsedLines.get(lineIndex);

            // Break the line into wrapped spans
            List<List<WrappedSpan>> wrappedRows = wrapLine(line, availableWidth);

            // Render each wrapped row
            for (List<WrappedSpan> row : wrappedRows) {
                if (currentY >= maxY) {
                    break;
                }

                Rect rowArea = new Rect(area.left(), currentY, availableWidth, 1);
                renderWrappedRow(frame, rowArea, context, row);
                currentY++;
            }
        }
    }

    /**
     * Wraps a line's spans to fit within the given width.
     * Returns a list of rows, each row being a list of wrapped spans.
     */
    private List<List<WrappedSpan>> wrapLine(Line line, int maxWidth) {
        List<List<WrappedSpan>> rows = new ArrayList<>();
        List<WrappedSpan> currentRow = new ArrayList<>();
        int currentWidth = 0;

        for (Span span : line.spans()) {
            String text = span.content();
            Tags tags = span.style().extension(Tags.class, Tags.empty());

            if (overflow == Overflow.WRAP_WORD) {
                // Word wrapping: break at word boundaries
                int start = 0;
                while (start < text.length()) {
                    // Find the next word boundary
                    int wordEnd = start;
                    while (wordEnd < text.length() && !Character.isWhitespace(text.charAt(wordEnd))) {
                        wordEnd++;
                    }

                    // Include trailing whitespace with the word
                    int spanEnd = wordEnd;
                    while (spanEnd < text.length() && Character.isWhitespace(text.charAt(spanEnd))) {
                        spanEnd++;
                    }

                    String wordWithSpace = text.substring(start, spanEnd);
                    int wordWidth = wordWithSpace.length();

                    // Check if we need to wrap
                    if (currentWidth + wordWidth > maxWidth && currentWidth > 0) {
                        // Trim trailing spaces from current row before wrapping
                        trimTrailingSpaces(currentRow);
                        rows.add(currentRow);
                        currentRow = new ArrayList<>();
                        currentWidth = 0;
                    }

                    // Add the word to current row (trim leading spaces if at start of line)
                    if (currentWidth == 0) {
                        wordWithSpace = trimLeadingSpaces(wordWithSpace);
                        wordWidth = wordWithSpace.length();
                    }

                    if (!wordWithSpace.isEmpty()) {
                        currentRow.add(new WrappedSpan(wordWithSpace, tags, span.style()));
                        currentWidth += wordWidth;
                    }

                    start = spanEnd;
                }
            } else {
                // Character wrapping: break at character boundaries
                int start = 0;
                while (start < text.length()) {
                    int remaining = maxWidth - currentWidth;
                    if (remaining <= 0) {
                        rows.add(currentRow);
                        currentRow = new ArrayList<>();
                        currentWidth = 0;
                        remaining = maxWidth;
                    }

                    int chunkEnd = Math.min(start + remaining, text.length());
                    String chunk = text.substring(start, chunkEnd);
                    currentRow.add(new WrappedSpan(chunk, tags, span.style()));
                    currentWidth += chunk.length();
                    start = chunkEnd;
                }
            }
        }

        // Add any remaining content
        if (!currentRow.isEmpty()) {
            trimTrailingSpaces(currentRow);
            rows.add(currentRow);
        }

        // Ensure at least one empty row for empty lines
        if (rows.isEmpty()) {
            rows.add(new ArrayList<>());
        }

        return rows;
    }

    private String trimLeadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }

    private void trimTrailingSpaces(List<WrappedSpan> row) {
        if (row.isEmpty()) {
            return;
        }
        // Trim trailing spaces from the last span
        WrappedSpan last = row.get(row.size() - 1);
        String text = last.text;
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (end < text.length()) {
            if (end == 0) {
                row.remove(row.size() - 1);
                trimTrailingSpaces(row); // Recurse to trim next span
            } else {
                row.set(row.size() - 1, new WrappedSpan(text.substring(0, end), last.tags, last.style));
            }
        }
    }

    /**
     * Renders a wrapped row of spans.
     */
    private void renderWrappedRow(Frame frame, Rect area, RenderContext context, List<WrappedSpan> row) {
        if (area.isEmpty()) {
            return;
        }

        int currentX = area.left();
        for (WrappedSpan wrappedSpan : row) {
            int spanWidth = wrappedSpan.text.length();
            int availableWidth = area.right() - currentX;
            if (availableWidth <= 0) {
                break;
            }

            String text = wrappedSpan.text;
            if (spanWidth > availableWidth) {
                text = text.substring(0, availableWidth);
                spanWidth = text.length();
            }

            Rect spanArea = new Rect(currentX, area.top(), spanWidth, 1);

            TextElement textElement = new TextElement(text);
            textElement.style(wrappedSpan.style);

            for (String tag : wrappedSpan.tags.values()) {
                textElement.addClass(tag);
            }

            textElement.fit();
            context.renderChild(textElement, frame, spanArea);

            currentX += spanWidth;
        }
    }

    /**
     * Helper class to represent a wrapped span with its metadata.
     */
    private static class WrappedSpan {
        final String text;
        final Tags tags;
        final Style style;

        WrappedSpan(String text, Tags tags, Style style) {
            this.text = text;
            this.tags = tags;
            this.style = style;
        }
    }

    /**
     * Renders a single parsed line as a row of TextElements.
     */
    private void renderLine(Frame frame, Rect area, RenderContext context, Line line) {
        if (area.isEmpty() || line.spans().isEmpty()) {
            return;
        }

        // For alignment support, calculate total line width
        int totalWidth = 0;
        for (Span span : line.spans()) {
            totalWidth += span.content().length();
        }

        // Calculate starting x position based on alignment
        int startX = area.left();
        if (alignment == Alignment.CENTER && totalWidth < area.width()) {
            startX = area.left() + (area.width() - totalWidth) / 2;
        } else if (alignment == Alignment.RIGHT && totalWidth < area.width()) {
            startX = area.left() + area.width() - totalWidth;
        }

        // Render each span as a TextElement
        int currentX = startX;
        for (Span span : line.spans()) {
            String text = span.content();
            int spanWidth = text.length();

            // Check if span fits in remaining space
            int availableWidth = area.right() - currentX;
            if (availableWidth <= 0) {
                break;
            }

            // Handle overflow/truncation
            if (spanWidth > availableWidth) {
                if (overflow == Overflow.ELLIPSIS) {
                    if (availableWidth >= 3) {
                        text = text.substring(0, availableWidth - 3) + "...";
                    } else {
                        text = text.substring(0, availableWidth);
                    }
                } else {
                    text = text.substring(0, availableWidth);
                }
                spanWidth = text.length();
            }

            // Create area for this span
            Rect spanArea = new Rect(currentX, area.top(), spanWidth, 1);

            // Create TextElement with CSS classes from tags
            TextElement textElement = new TextElement(text);
            textElement.style(span.style());

            // Add tag names as CSS classes
            Tags tags = span.style().extension(Tags.class, Tags.empty());
            for (String tag : tags.values()) {
                textElement.addClass(tag);
            }

            // Use fit constraint for inline rendering
            textElement.fit();

            // Render the element
            context.renderChild(textElement, frame, spanArea);

            currentX += spanWidth;
        }
    }

    /**
     * Returns the maximum width of any line in the parsed content.
     */
    private int maxLineWidth(List<Line> lines) {
        int max = 0;
        for (Line line : lines) {
            max = Math.max(max, line.width());
        }
        return max;
    }

    private void ensureTextParsed() {
        if (textDirty || parsedText == null) {
            MarkupParser.StyleResolver resolver = createSimpleResolver();
            parsedText = MarkupParser.parse(markup, resolver);
            textDirty = false;
        }
    }

    private MarkupParser.StyleResolver createSimpleResolver() {
        return tagName -> {
            if (customResolver != null) {
                return customResolver.resolve(tagName);
            }
            return null;
        };
    }

    private MarkupParser.StyleResolver createCombinedResolver(RenderContext context) {
        return tagName -> {
            // 1. Check custom resolver
            if (customResolver != null) {
                Style customStyle = customResolver.resolve(tagName);
                if (customStyle != null) {
                    return customStyle;
                }
            }

            // 2. Check TCSS via context (unknown tags are treated as CSS class names)
            // Use resolveStyle with the tag name as a CSS class
            return context.resolveStyle(null, tagName)
                    .map(CssStyleResolver::toStyle)
                    .orElse(null);
        };
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

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * A simple inline element for displaying styled {@link Text}.
 * <p>
 * This element is similar to {@link TextElement} but accepts pre-styled
 * {@link Text} objects with multiple {@link Span}s, enabling syntax
 * highlighting, markup rendering, and other rich text features.
 * <p>
 * For scrollable rich text with borders, see {@link RichTextAreaElement}.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * import static dev.tamboui.toolkit.Toolkit.*;
 *
 * Text styledText = Text.from(
 *     Line.from(Span.styled("Hello", Style.EMPTY.bold()),
 *               Span.raw(" "),
 *               Span.styled("World", Style.EMPTY.fg(Color.CYAN)))
 * );
 *
 * richText(styledText)
 *     .centered()
 * }</pre>
 *
 * @see RichTextAreaElement for scrollable rich text areas
 * @see TextElement for plain text
 * @see Text for creating styled text
 */
public final class RichTextElement extends StyledElement<RichTextElement> {

    private Text text;
    private Overflow overflow;
    private Alignment alignment;

    /**
     * Creates an empty rich text element.
     */
    public RichTextElement() {
        this.text = Text.empty();
    }

    /**
     * Creates a rich text element with the specified text.
     *
     * @param text
     *            the styled text to display
     */
    public RichTextElement(Text text) {
        this.text = text != null ? text : Text.empty();
    }

    /**
     * Creates a rich text element from a plain string.
     *
     * @param content
     *            the text content
     */
    public RichTextElement(String content) {
        this.text = content != null && !content.isEmpty() ? Text.raw(content) : Text.empty();
    }

    /**
     * Sets the text content.
     *
     * @param text
     *            the styled text to display
     * @return this element for chaining
     */
    public RichTextElement text(Text text) {
        this.text = text != null ? text : Text.empty();
        return this;
    }

    /**
     * Returns the current text content.
     *
     * @return the text
     */
    public Text text() {
        return text;
    }

    /**
     * Sets the overflow mode for text that doesn't fit.
     *
     * @param overflow
     *            the overflow mode
     * @return this element for chaining
     */
    public RichTextElement overflow(Overflow overflow) {
        this.overflow = overflow;
        return this;
    }

    /**
     * Sets word wrapping mode.
     *
     * @return this element for chaining
     */
    public RichTextElement wrapWord() {
        this.overflow = Overflow.WRAP_WORD;
        return this;
    }

    /**
     * Sets character wrapping mode.
     *
     * @return this element for chaining
     */
    public RichTextElement wrapCharacter() {
        this.overflow = Overflow.WRAP_CHARACTER;
        return this;
    }

    /**
     * Truncates with ellipsis at the end.
     *
     * @return this element for chaining
     */
    public RichTextElement ellipsis() {
        this.overflow = Overflow.ELLIPSIS;
        return this;
    }

    /**
     * Truncates with ellipsis at the start.
     *
     * @return this element for chaining
     */
    public RichTextElement ellipsisStart() {
        this.overflow = Overflow.ELLIPSIS_START;
        return this;
    }

    /**
     * Truncates with ellipsis in the middle.
     *
     * @return this element for chaining
     */
    public RichTextElement ellipsisMiddle() {
        this.overflow = Overflow.ELLIPSIS_MIDDLE;
        return this;
    }

    /**
     * Sets the text alignment.
     *
     * @param alignment
     *            the alignment
     * @return this element for chaining
     */
    public RichTextElement alignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * Centers the text horizontally.
     *
     * @return this element for chaining
     */
    public RichTextElement centered() {
        this.alignment = Alignment.CENTER;
        return this;
    }

    /**
     * Aligns text to the right.
     *
     * @return this element for chaining
     */
    public RichTextElement right() {
        this.alignment = Alignment.RIGHT;
        return this;
    }

    @Override
    public Constraint constraint() {
        if (layoutConstraint != null) {
            return layoutConstraint;
        }

        Overflow currentOverflow = overflow != null ? overflow : Overflow.CLIP;
        if (currentOverflow == Overflow.WRAP_CHARACTER || currentOverflow == Overflow.WRAP_WORD) {
            return Constraint.min(text.lines().size());
        }

        return null;
    }

    @Override
    public int preferredWidth() {
        int maxWidth = 0;
        for (Line line : text.lines()) {
            int width = 0;
            for (Span span : line.spans()) {
                width += span.width();
            }
            maxWidth = Math.max(maxWidth, width);
        }
        return maxWidth;
    }

    @Override
    public int preferredHeight() {
        return text.lines().size();
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (text.lines().isEmpty()) {
            return;
        }

        Style effectiveStyle = context.currentStyle();

        StylePropertyResolver resolver = context.resolveStyle(this)
                .map(r -> (StylePropertyResolver) r).orElse(StylePropertyResolver.empty());

        Paragraph.Builder paragraphBuilder = Paragraph.builder().text(text).style(effectiveStyle)
                .styleResolver(resolver);

        if (alignment != null) {
            paragraphBuilder.alignment(alignment);
        }
        if (overflow != null) {
            paragraphBuilder.overflow(overflow);
        }

        Paragraph paragraph = paragraphBuilder.build();
        frame.renderWidget(paragraph, area);
    }
}

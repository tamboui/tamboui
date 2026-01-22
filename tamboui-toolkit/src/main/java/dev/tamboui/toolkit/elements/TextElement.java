/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.text.Overflow;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * A simple text element that displays styled text.
 * <p>
 * CSS properties {@code text-overflow} and {@code text-align} are automatically
 * resolved through the underlying {@link Paragraph} widget.
 */
public final class TextElement extends StyledElement<TextElement> {

    private final String content;
    private Overflow overflow;
    private Alignment alignment;

    public TextElement(String content) {
        this.content = content != null ? content : "";
    }

    public TextElement(Object value) {
        this.content = value != null ? String.valueOf(value) : "";
    }

    /**
     * Returns the text content.
     */
    public String content() {
        return content;
    }

    /**
     * Sets the overflow mode for text that doesn't fit.
     *
     * @param overflow the overflow mode
     * @return this element for chaining
     */
    public TextElement overflow(Overflow overflow) {
        this.overflow = overflow;
        return this;
    }

    /**
     * Convenience method to truncate with ellipsis at the end: "Long text..."
     *
     * @return this element for chaining
     */
    public TextElement ellipsis() {
        this.overflow = Overflow.ELLIPSIS;
        return this;
    }

    /**
     * Convenience method to truncate with ellipsis at the start: "...ong text"
     *
     * @return this element for chaining
     */
    public TextElement ellipsisStart() {
        this.overflow = Overflow.ELLIPSIS_START;
        return this;
    }

    /**
     * Convenience method to truncate with ellipsis in the middle: "Long...text"
     *
     * @return this element for chaining
     */
    public TextElement ellipsisMiddle() {
        this.overflow = Overflow.ELLIPSIS_MIDDLE;
        return this;
    }

    /**
     * Sets the text alignment.
     *
     * @param alignment the alignment
     * @return this element for chaining
     */
    public TextElement alignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * Centers the text horizontally.
     *
     * @return this element for chaining
     */
    public TextElement centered() {
        this.alignment = Alignment.CENTER;
        return this;
    }

    /**
     * Aligns text to the right.
     *
     * @return this element for chaining
     */
    public TextElement right() {
        this.alignment = Alignment.RIGHT;
        return this;
    }

    /**
     * Returns the layout constraint for this text element.
     * <p>
     * If no explicit constraint is set, a sensible default is calculated based on
     * the content and overflow mode:
     * <ul>
     *   <li>For wrapping modes (WRAP_WORD, WRAP_CHARACTER): uses {@code min(lineCount)}
     *       to ensure at least minimum height while allowing growth for wrapped content.</li>
     *   <li>For non-wrapping modes (CLIP, ELLIPSIS, etc.): returns {@code null} to let
     *       the container decide (typically using {@code fill()}).</li>
     * </ul>
     *
     * @return the constraint for this element
     */
    @Override
    public Constraint constraint() {
        if (layoutConstraint != null) {
            return layoutConstraint;
        }

        // For wrapping modes, use min constraint to allow growth for wrapped text
        // For non-wrapping modes, return null to let the container decide
        Overflow currentOverflow = overflow != null ? overflow : Overflow.CLIP;
        if (currentOverflow == Overflow.WRAP_CHARACTER || currentOverflow == Overflow.WRAP_WORD) {
            return Constraint.min(countLines());
        }

        return null;
    }

    /**
     * Calculates a height constraint based on content line count and overflow mode.
     * Used by vertical containers (Column) to determine the height for this text element.
     *
     * @return height constraint based on line count
     */
    Constraint calculateHeightConstraint() {
        int lineCount = countLines();
        Overflow currentOverflow = overflow != null ? overflow : Overflow.CLIP;
        if (currentOverflow == Overflow.WRAP_CHARACTER || currentOverflow == Overflow.WRAP_WORD) {
            return Constraint.min(lineCount);
        }
        return Constraint.length(lineCount);
    }

    private int countLines() {
        int lineCount = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lineCount++;
            }
        }
        return lineCount;
    }

    /**
     * Calculates the height needed for this text when rendered with wrapping at the given width.
     * <p>
     * For non-wrapping modes, returns the natural line count.
     * For wrapping modes, calculates how many lines will be produced after wrapping.
     *
     * @param width the available width for rendering
     * @param effectiveOverflow the overflow mode to use
     * @return the number of lines needed
     */
    private int calculateWrappedHeight(int width, Overflow effectiveOverflow) {
        if (width <= 0 || content.isEmpty()) {
            return 1;
        }

        if (effectiveOverflow != Overflow.WRAP_CHARACTER && effectiveOverflow != Overflow.WRAP_WORD) {
            return countLines();
        }

        // For wrapping modes, calculate how many lines we'll need
        int totalLines = 0;
        int start = 0;
        for (int i = 0; i <= content.length(); i++) {
            if (i == content.length() || content.charAt(i) == '\n') {
                // Process this logical line
                int lineLength = i - start;
                if (lineLength == 0) {
                    totalLines++;
                } else {
                    // Calculate wrapped lines for this segment
                    totalLines += (lineLength + width - 1) / width;
                }
                start = i + 1;
            }
        }
        return Math.max(1, totalLines);
    }

    @Override
    public int preferredWidth() {
        // For multi-line text, return the longest line
        int maxWidth = 0;
        int currentWidth = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                maxWidth = Math.max(maxWidth, currentWidth);
                currentWidth = 0;
            } else {
                currentWidth++;
            }
        }
        return Math.max(maxWidth, currentWidth);
    }

    @Override
    public int preferredHeight() {
        return countLines();
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        Overflow effectiveOverflow = resolveOverflow(context);
        return calculateWrappedHeight(availableWidth, effectiveOverflow);
    }

    /**
     * Resolves the overflow mode: programmatic value takes precedence, then CSS, then default.
     */
    private Overflow resolveOverflow(RenderContext context) {
        if (overflow != null) {
            return overflow;
        }
        if (context != null) {
            return context.resolveStyle(this)
                    .flatMap(resolver -> resolver.get(Paragraph.TEXT_OVERFLOW))
                    .orElse(Overflow.CLIP);
        }
        return Overflow.CLIP;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (content.isEmpty()) {
            return;
        }

        // Get the current style from the context (already resolved by StyledElement.render)
        Style effectiveStyle = context.currentStyle();

        // Get the CSS resolver for this element
        StylePropertyResolver resolver = context.resolveStyle(this)
                .map(r -> (StylePropertyResolver) r)
                .orElse(StylePropertyResolver.empty());

        // Build paragraph - CSS properties are resolved by the widget
        Paragraph.Builder paragraphBuilder = Paragraph.builder()
                .text(Text.from(Line.from(Span.styled(content, effectiveStyle))))
                .style(effectiveStyle)
                .styleResolver(resolver);

        // Set programmatic overrides if specified
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

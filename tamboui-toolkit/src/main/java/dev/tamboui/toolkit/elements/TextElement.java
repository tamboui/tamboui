/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.ResolvedStyle;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.text.Overflow;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * A simple text element that displays styled text.
 */
public final class TextElement extends StyledElement<TextElement> {

    private final String content;
    private Overflow overflow = Overflow.CLIP;
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
        if (overflow == Overflow.WRAP_CHARACTER || overflow == Overflow.WRAP_WORD) {
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
        if (overflow == Overflow.WRAP_CHARACTER || overflow == Overflow.WRAP_WORD) {
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

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (content.isEmpty()) {
            return;
        }

        // Get the current style from the context (already resolved by StyledElement.render)
        Style effectiveStyle = context.currentStyle();

        // Get alignment: programmatic > CSS > left (default)
        Alignment effectiveAlignment = this.alignment;
        if (effectiveAlignment == null) {
            effectiveAlignment = context.resolveStyle(this)
                .flatMap(ResolvedStyle::alignment)
                .orElse(Alignment.LEFT);
        }

        // Create a styled span and render as a paragraph
        Span span = Span.styled(content, effectiveStyle);
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(Line.from(span)))
            .style(effectiveStyle)
            .alignment(effectiveAlignment)
            .overflow(overflow)
            .build();
        frame.renderWidget(paragraph, area);
    }

}

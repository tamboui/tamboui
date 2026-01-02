/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Rect;
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

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty() || content.isEmpty()) {
            return;
        }

        // Create a styled span and render as a paragraph
        Span span = Span.styled(content, style);
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(Line.from(span)))
            .overflow(overflow)
            .build();
        frame.renderWidget(paragraph, area);
    }
}

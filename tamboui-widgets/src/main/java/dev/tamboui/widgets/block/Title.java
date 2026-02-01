/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import dev.tamboui.layout.Alignment;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.style.Overflow;

/**
 * A title for a block, with optional alignment and overflow handling.
 */
public final class Title {

    private final Line content;
    private final Alignment alignment;
    private final Overflow overflow;

    /**
     * Creates a new title.
     *
     * @param content   the title content
     * @param alignment the title alignment
     * @param overflow  the overflow handling strategy
     */
    public Title(Line content, Alignment alignment, Overflow overflow) {
        this.content = content;
        this.alignment = alignment;
        this.overflow = overflow;
    }

    /**
     * Creates a left-aligned title from a string.
     *
     * @param text the title text
     * @return a new Title
     */
    public static Title from(String text) {
        return new Title(Line.from(text), Alignment.LEFT, Overflow.CLIP);
    }

    /**
     * Creates a left-aligned title from a span.
     *
     * @param span the title span
     * @return a new Title
     */
    public static Title from(Span span) {
        return new Title(Line.from(span), Alignment.LEFT, Overflow.CLIP);
    }

    /**
     * Creates a left-aligned title from a line.
     *
     * @param line the title line
     * @return a new Title
     */
    public static Title from(Line line) {
        return new Title(line, Alignment.LEFT, Overflow.CLIP);
    }

    /**
     * Returns a new title with the given alignment.
     *
     * @param alignment the alignment
     * @return a new Title with the specified alignment
     */
    public Title alignment(Alignment alignment) {
        return new Title(content, alignment, overflow);
    }

    /**
     * Returns a new left-aligned title.
     *
     * @return a new Title aligned to the left
     */
    public Title left() {
        return alignment(Alignment.LEFT);
    }

    /**
     * Returns a new center-aligned title.
     *
     * @return a new Title aligned to the center
     */
    public Title centered() {
        return alignment(Alignment.CENTER);
    }

    /**
     * Returns a new right-aligned title.
     *
     * @return a new Title aligned to the right
     */
    public Title right() {
        return alignment(Alignment.RIGHT);
    }

    /**
     * Returns a new title with the given overflow mode.
     *
     * @param overflow the overflow mode
     * @return a new Title with the specified overflow
     */
    public Title overflow(Overflow overflow) {
        return new Title(content, alignment, overflow);
    }

    /**
     * Truncate with ellipsis at end if title doesn't fit: "Long title..."
     *
     * @return a new Title with end ellipsis overflow
     */
    public Title ellipsis() {
        return overflow(Overflow.ELLIPSIS);
    }

    /**
     * Truncate with ellipsis at start if title doesn't fit: "...ong title"
     *
     * @return a new Title with start ellipsis overflow
     */
    public Title ellipsisStart() {
        return overflow(Overflow.ELLIPSIS_START);
    }

    /**
     * Truncate with ellipsis in middle if title doesn't fit: "Long...itle"
     *
     * @return a new Title with middle ellipsis overflow
     */
    public Title ellipsisMiddle() {
        return overflow(Overflow.ELLIPSIS_MIDDLE);
    }

    /**
     * Returns the title content.
     *
     * @return the content
     */
    public Line content() {
        return content;
    }

    /**
     * Returns the title alignment.
     *
     * @return the alignment
     */
    public Alignment alignment() {
        return alignment;
    }

    /**
     * Returns the overflow mode.
     *
     * @return the overflow mode
     */
    public Overflow overflow() {
        return overflow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Title)) {
            return false;
        }
        Title title = (Title) o;
        return content.equals(title.content) && alignment == title.alignment && overflow == title.overflow;
    }

    @Override
    public int hashCode() {
        int result = content.hashCode();
        result = 31 * result + alignment.hashCode();
        result = 31 * result + overflow.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("Title[content=%s, alignment=%s, overflow=%s]", content, alignment, overflow);
    }
}

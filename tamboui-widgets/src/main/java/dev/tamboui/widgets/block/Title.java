/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import dev.tamboui.layout.Alignment;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.text.Overflow;

/**
 * A title for a block, with optional alignment and overflow handling.
 */
public final class Title {

    private final Line content;
    private final Alignment alignment;
    private final Overflow overflow;

    public Title(Line content, Alignment alignment, Overflow overflow) {
        this.content = content;
        this.alignment = alignment;
        this.overflow = overflow;
    }

    public static Title from(String text) {
        return new Title(Line.from(text), Alignment.LEFT, Overflow.CLIP);
    }

    public static Title from(Span span) {
        return new Title(Line.from(span), Alignment.LEFT, Overflow.CLIP);
    }

    public static Title from(Line line) {
        return new Title(line, Alignment.LEFT, Overflow.CLIP);
    }

    public Title alignment(Alignment alignment) {
        return new Title(content, alignment, overflow);
    }

    public Title left() {
        return alignment(Alignment.LEFT);
    }

    public Title centered() {
        return alignment(Alignment.CENTER);
    }

    public Title right() {
        return alignment(Alignment.RIGHT);
    }

    public Title overflow(Overflow overflow) {
        return new Title(content, alignment, overflow);
    }

    /**
     * Truncate with ellipsis at end if title doesn't fit: "Long title..."
     */
    public Title ellipsis() {
        return overflow(Overflow.ELLIPSIS);
    }

    /**
     * Truncate with ellipsis at start if title doesn't fit: "...ong title"
     */
    public Title ellipsisStart() {
        return overflow(Overflow.ELLIPSIS_START);
    }

    /**
     * Truncate with ellipsis in middle if title doesn't fit: "Long...itle"
     */
    public Title ellipsisMiddle() {
        return overflow(Overflow.ELLIPSIS_MIDDLE);
    }

    public Line content() {
        return content;
    }

    public Alignment alignment() {
        return alignment;
    }

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

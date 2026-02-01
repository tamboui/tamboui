/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.stack;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.ContentAlignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.Widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

/**
 * An overlapping layers widget where children render on top of each other
 * using a painter's algorithm (last child on top).
 * <p>
 * Essential for dialogs, popups, floating overlays, and any scenario
 * where UI elements need to overlap.
 * <p>
 * Example usage:
 * <pre>{@code
 * Stack stack = Stack.builder()
 *     .children(backgroundWidget, contentWidget, dialogWidget)
 *     .alignment(ContentAlignment.CENTER)
 *     .build();
 *
 * stack.render(area, buffer);
 * }</pre>
 */
public final class Stack implements Widget {

    private final List<Widget> children;
    private final ContentAlignment alignment;

    private Stack(Builder builder) {
        this.children = listCopyOf(builder.children);
        this.alignment = builder.alignment;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty() || children.isEmpty()) {
            return;
        }

        for (Widget child : children) {
            // For STRETCH, render into full area; for positional alignment,
            // the child gets the full area (since we don't know preferred sizes
            // at the widget level — alignment is primarily useful at element level
            // where preferred sizes are available)
            child.render(area, buffer);
        }
    }

    /**
     * Returns the alignment for this stack.
     *
     * @return the content alignment
     */
    public ContentAlignment alignment() {
        return alignment;
    }

    /**
     * Builder for {@link Stack}.
     */
    public static final class Builder {
        private final List<Widget> children = new ArrayList<>();
        private ContentAlignment alignment = ContentAlignment.STRETCH;

        private Builder() {
        }

        /**
         * Sets the children widgets.
         *
         * @param children the child widgets
         * @return this builder
         */
        public Builder children(Widget... children) {
            this.children.clear();
            this.children.addAll(Arrays.asList(children));
            return this;
        }

        /**
         * Sets the children widgets from a list.
         *
         * @param children the child widgets
         * @return this builder
         */
        public Builder children(List<Widget> children) {
            this.children.clear();
            this.children.addAll(children);
            return this;
        }

        /**
         * Sets how children are aligned within the stack.
         * <p>
         * Default is {@link ContentAlignment#STRETCH} which fills
         * children to the full area.
         *
         * @param alignment the alignment mode
         * @return this builder
         */
        public Builder alignment(ContentAlignment alignment) {
            this.alignment = alignment;
            return this;
        }

        /**
         * Builds the {@link Stack} widget.
         *
         * @return a new Stack widget
         */
        public Stack build() {
            return new Stack(this);
        }
    }
}

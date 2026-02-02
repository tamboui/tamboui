/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.style.Tags;
import dev.tamboui.widget.RawOutputCapable;
import dev.tamboui.widget.StatefulWidget;
import dev.tamboui.widget.Widget;

/**
 * A frame represents a single render cycle.
 * Widgets are rendered to the frame's buffer.
 */
public final class Frame {

    private final Buffer buffer;
    private final Rect area;
    private final OutputStream rawOutput;
    private Position cursorPosition;
    private boolean cursorVisible;
    private final Deque<String> contextKeyStack = new ArrayDeque<>();

    Frame(Buffer buffer, OutputStream rawOutput) {
        this.buffer = buffer;
        this.area = buffer.area();
        this.rawOutput = rawOutput;
        this.cursorPosition = null;
        this.cursorVisible = false;
    }

    /**
     * Creates a frame for testing purposes.
     * This allows tests to create frames without going through Terminal.
     *
     * @param buffer the buffer to render to
     * @return a new frame backed by the given buffer
     */
    public static Frame forTesting(Buffer buffer) {
        return new Frame(buffer, null);
    }

    /**
     * Returns the area available for rendering.
     *
     * @return the rendering area
     */
    public Rect area() {
        return area;
    }

    /**
     * Returns the underlying buffer.
     *
     * @return the buffer
     */
    public Buffer buffer() {
        return buffer;
    }

    /**
     * Returns the terminal width.
     *
     * @return the terminal width
     */
    public int width() {
        return area.width();
    }

    /**
     * Returns the terminal height.
     *
     * @return the terminal height
     */
    public int height() {
        return area.height();
    }

    /**
     * Renders a widget to the given area.
     * <p>
     * If the widget implements {@link RawOutputCapable}, the raw output stream
     * will be passed to enable native terminal protocol rendering.
     *
     * @param widget the widget to render
     * @param area the area to render within
     */
    public void renderWidget(Widget widget, Rect area) {
        if (widget instanceof RawOutputCapable) {
            ((RawOutputCapable) widget).render(area, buffer, rawOutput);
        } else {
            widget.render(area, buffer);
        }
    }

    /**
     * Renders a stateful widget to the given area.
     *
     * @param <S> the state type
     * @param widget the stateful widget to render
     * @param area the area to render within
     * @param state the widget state
     */
    public <S> void renderStatefulWidget(StatefulWidget<S> widget, Rect area, S state) {
        widget.render(area, buffer, state);
    }

    /**
     * Sets the cursor position. The cursor will be shown at this position
     * after the frame is drawn.
     *
     * @param position the cursor position
     */
    public void setCursorPosition(Position position) {
        this.cursorPosition = position;
        this.cursorVisible = true;
    }

    /**
     * Sets the cursor position using x and y coordinates.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void setCursorPosition(int x, int y) {
        setCursorPosition(new Position(x, y));
    }

    /**
     * Returns the cursor position if set.
     *
     * @return the cursor position, or empty if not set
     */
    public Optional<Position> cursorPosition() {
        return Optional.ofNullable(cursorPosition);
    }

    /**
     * Clears the cursor position, hiding the cursor.
     */
    public void clearCursor() {
        this.cursorPosition = null;
        this.cursorVisible = false;
    }

    /**
     * Returns whether the cursor should be visible.
     *
     * @return true if the cursor should be visible, false otherwise
     */
    boolean isCursorVisible() {
        return cursorVisible;
    }

    /**
     * Returns the raw output stream for native protocol rendering.
     * <p>
     * Package-private: use {@link FrameInternal#rawOutput(Frame)} to access.
     *
     * @return the raw output stream, or null if not available
     */
    OutputStream rawOutput() {
        return rawOutput;
    }

    /**
     * Sets the styled area registry for tracking styled content.
     * <p>
     * When set, styled content written to the buffer with Tags will be
     * automatically registered in the registry for effect targeting.
     *
     * @param registry the registry, or null to disable tracking
     */
    public void setStyledAreaRegistry(StyledAreaRegistry registry) {
        if (registry == null) {
            buffer.setStyledContentListener(null);
        } else {
            buffer.setStyledContentListener((style, area) -> {
                Tags tags = style.extension(Tags.class, Tags.empty());
                if (!tags.isEmpty()) {
                    registry.register(style, area, currentContextKey());
                }
            });
        }
    }

    /**
     * Pushes a context key onto the stack.
     * <p>
     * The context key is used to associate styled areas with a parent context.
     * Typically, this is called when entering an element's render scope with
     * the element's ID as the context key.
     *
     * @param contextKey the context key (typically an element ID)
     */
    public void pushContextKey(String contextKey) {
        if (contextKey != null) {
            contextKeyStack.push(contextKey);
        }
    }

    /**
     * Pops the most recent context key from the stack.
     * <p>
     * This should be called when exiting an element's render scope.
     */
    public void popContextKey() {
        if (!contextKeyStack.isEmpty()) {
            contextKeyStack.pop();
        }
    }

    /**
     * Returns the current context key (top of the stack).
     * <p>
     * This is used to associate styled areas with their containing element.
     *
     * @return the current context key, or null if no context is active
     */
    public String currentContextKey() {
        return contextKeyStack.isEmpty() ? null : contextKeyStack.peek();
    }
}

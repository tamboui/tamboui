/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;

/**
 * An element that displays an error placeholder when another element fails to
 * render.
 * <p>
 * Used by fault-tolerant rendering to show a visual indication that an element
 * encountered an error during rendering, while allowing the rest of the UI to
 * continue.
 *
 * <pre>{@code
 * // Typically created automatically during fault-tolerant rendering
 * ErrorPlaceholder placeholder = ErrorPlaceholder.from(new RuntimeException("Widget failed"),
 *         "my-widget");
 * }</pre>
 */
public final class ErrorPlaceholder implements Element {

    private final Throwable cause;
    private final String elementId;

    private ErrorPlaceholder(Throwable cause, String elementId) {
        this.cause = cause;
        this.elementId = elementId;
    }

    /**
     * Creates an ErrorPlaceholder for the given exception.
     *
     * @param cause
     *            the exception that occurred
     * @param elementId
     *            the ID of the element that failed, may be null
     * @return a new ErrorPlaceholder
     */
    public static ErrorPlaceholder from(Throwable cause, String elementId) {
        return new ErrorPlaceholder(cause, elementId);
    }

    /**
     * Creates an ErrorPlaceholder for the given exception.
     *
     * @param cause
     *            the exception that occurred
     * @return a new ErrorPlaceholder
     */
    public static ErrorPlaceholder from(Throwable cause) {
        return from(cause, null);
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        Buffer buffer = frame.buffer();

        // Build title with element ID if available
        String title = elementId != null ? " Error: " + elementId + " " : " Error ";

        // Truncate error message to fit
        String message = cause.getClass().getSimpleName();
        if (cause.getMessage() != null) {
            String shortMessage = cause.getMessage();
            if (shortMessage.length() > 30) {
                shortMessage = shortMessage.substring(0, 27) + "...";
            }
            message = message + ": " + shortMessage;
        }

        // Create a minimal block with the error info
        Block block = Block.builder().title(title).borders(Borders.ALL).borderType(BorderType.PLAIN)
                .borderColor(Color.RED).build();

        block.render(area, buffer);
        Rect inner = block.inner(area);

        if (!inner.isEmpty()) {
            // Show error icon and message
            // Tag error output so StyledAreaRegistry can target it (e.g. for effects/CSS).
            Style errorStyle = Style.EMPTY.fg(Color.RED).withExtension(Tags.class,
                    Tags.of("error"));
            Line errorLine = Line.from(new Span("!", errorStyle.bold()), Span.raw(" " + message));

            // Truncate if needed
            if (inner.width() > 0) {
                buffer.setLine(inner.left(), inner.top(), errorLine);
            }
        }
    }

    /**
     * Returns the exception that caused this placeholder.
     *
     * @return the cause
     */
    public Throwable cause() {
        return cause;
    }

    /**
     * Returns the ID of the element that failed, if available.
     *
     * @return the element ID, or null
     */
    public String elementId() {
        return elementId;
    }
}

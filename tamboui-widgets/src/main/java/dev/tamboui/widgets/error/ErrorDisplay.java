/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;

/**
 * A widget that displays error information with a stack trace.
 * <p>
 * Used to show exceptions that occur during rendering. The widget displays:
 * <ul>
 * <li>Exception type in red</li>
 * <li>Error message</li>
 * <li>Scrollable stack trace</li>
 * <li>Footer with instructions</li>
 * </ul>
 *
 * <pre>{@code
 * ErrorDisplay display = ErrorDisplay.builder().error(exception).scroll(scrollOffset)
 *         .title(" ERROR ").footer(" Press 'q' to quit, arrows to scroll ").build();
 *
 * display.render(area, buffer);
 * }</pre>
 */
public final class ErrorDisplay implements Widget {

    private final Throwable error;
    private final String title;
    private final String footer;
    private final int scroll;
    private final Color borderColor;

    private ErrorDisplay(Builder builder) {
        this.error = builder.error;
        this.title = builder.title;
        this.footer = builder.footer;
        this.scroll = builder.scroll;
        this.borderColor = builder.borderColor;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an ErrorDisplay for the given exception with default settings.
     *
     * @param error
     *            the exception to display
     * @return a new ErrorDisplay
     */
    public static ErrorDisplay from(Throwable error) {
        return builder().error(error).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty() || error == null) {
            return;
        }

        // Build content lines
        List<Line> lines = buildContentLines();

        // Create the block with border
        Block block = Block.builder().title(title).titleBottom(footer).borders(Borders.ALL)
                .borderType(BorderType.ROUNDED).borderColor(borderColor).build();

        // Render the block
        block.render(area, buffer);
        Rect inner = block.inner(area);

        if (inner.isEmpty()) {
            return;
        }

        // Calculate visible lines based on scroll
        int visibleHeight = inner.height();
        int maxScroll = Math.max(0, lines.size() - visibleHeight);
        int actualScroll = Math.min(scroll, maxScroll);

        // Render visible lines
        for (int i = 0; i < visibleHeight && (actualScroll + i) < lines.size(); i++) {
            Line line = lines.get(actualScroll + i);
            buffer.setLine(inner.left(), inner.top() + i, line);
        }
    }

    private List<Line> buildContentLines() {
        List<Line> lines = new ArrayList<Line>();

        // Exception type
        lines.add(
                Line.from(new Span(error.getClass().getName(), Style.EMPTY.fg(Color.RED).bold())));
        lines.add(Line.from(Span.raw("")));

        // Message
        String message = error.getMessage();
        if (message != null && !message.isEmpty()) {
            lines.add(Line.from(new Span("Message: ", Style.EMPTY.bold()), Span.raw(message)));
            lines.add(Line.from(Span.raw("")));
        }

        // Stack trace
        lines.add(Line.from(new Span("Stack trace:", Style.EMPTY.bold())));

        String stackTrace = formatStackTrace(error);
        String[] stackLines = stackTrace.split("\n");
        for (String stackLine : stackLines) {
            lines.add(Line.from(Span.raw(stackLine)));
        }

        return lines;
    }

    private static String formatStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    /**
     * Returns the total number of content lines.
     * <p>
     * Useful for calculating scroll bounds.
     *
     * @return the number of lines
     */
    public int lineCount() {
        return buildContentLines().size();
    }

    /**
     * Builder for {@link ErrorDisplay}.
     */
    public static final class Builder {
        private Throwable error;
        private String title = " ERROR ";
        private String footer = " Press 'q' to quit, arrows to scroll ";
        private int scroll = 0;
        private Color borderColor = Color.RED;

        private Builder() {
        }

        /**
         * Sets the error to display.
         *
         * @param error
         *            the exception
         * @return this builder
         */
        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        /**
         * Sets the title shown at the top of the border.
         *
         * @param title
         *            the title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the footer shown at the bottom of the border.
         *
         * @param footer
         *            the footer
         * @return this builder
         */
        public Builder footer(String footer) {
            this.footer = footer;
            return this;
        }

        /**
         * Sets the scroll offset.
         *
         * @param scroll
         *            the number of lines to scroll
         * @return this builder
         */
        public Builder scroll(int scroll) {
            this.scroll = Math.max(0, scroll);
            return this;
        }

        /**
         * Sets the border color.
         *
         * @param color
         *            the border color
         * @return this builder
         */
        public Builder borderColor(Color color) {
            this.borderColor = color;
            return this;
        }

        /**
         * Builds the ErrorDisplay.
         *
         * @return a new ErrorDisplay
         */
        public ErrorDisplay build() {
            return new ErrorDisplay(this);
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Overflow;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Computes toast dimensions and wraps message text for display.
 */
public final class ToastSizing {

    /** Default maximum inner content width for toast layout. */
    public static final int DEFAULT_MAX_WIDTH = 40;

    private ToastSizing() {
    }

    /**
     * Wraps a message into lines that fit within the given display width.
     * <p>
     * Word boundaries are respected; wide characters are measured via {@link CharWidth}.
     *
     * @param message the message text
     * @param maxWidth the maximum display width per line
     * @return wrapped lines (never {@code null})
     */
    public static List<String> wrapMessage(String message, int maxWidth) {
        Objects.requireNonNull(message, "message");
        if (maxWidth <= 0) {
            return Collections.emptyList();
        }
        if (message.isEmpty()) {
            return Collections.singletonList("");
        }

        int estimatedLines = (CharWidth.of(message) / maxWidth) + 2;
        int scratchHeight = Math.max(64, estimatedLines);
        Rect area = new Rect(0, 0, maxWidth, scratchHeight);
        Buffer buffer = Buffer.empty(area);

        Paragraph.builder()
                .text(Text.from(message))
                .overflow(Overflow.WRAP_WORD)
                .build()
                .render(area, buffer);

        List<String> lines = new ArrayList<>();
        boolean sawContent = false;
        for (int y = area.top(); y < area.bottom(); y++) {
            String line = readRow(buffer, area.left(), y, area.width());
            if (!line.isEmpty()) {
                lines.add(line);
                sawContent = true;
            } else if (sawContent) {
                break;
            }
        }

        if (lines.isEmpty()) {
            return Collections.singletonList("");
        }
        return Collections.unmodifiableList(lines);
    }

    /**
     * Measures the total width and height of a toast for layout.
     * <p>
     * Height includes top and bottom borders, optional title and progress rows, and wrapped message lines.
     * Width is the inner content width (excluding left/right borders), capped to {@code maxWidth}.
     *
     * @param toast the toast definition
     * @param maxWidth the maximum inner content width
     * @return the measured dimension
     */
    public static Dimension measure(Toast toast, int maxWidth) {
        Objects.requireNonNull(toast, "toast");
        if (maxWidth <= 0) {
            return new Dimension(0, 0);
        }

        List<String> messageLines = wrapMessage(toast.message(), maxWidth);

        int innerWidth = 0;
        for (String line : messageLines) {
            innerWidth = Math.max(innerWidth, CharWidth.of(line));
        }

        if (hasTitleRow(toast)) {
            innerWidth = Math.max(innerWidth, CharWidth.of(titleBandText(toast)));
        }

        innerWidth = Math.min(innerWidth, maxWidth);

        int height = 2;
        if (hasTitleRow(toast)) {
            height += 1;
        }
        height += messageLines.size();
        if (!toast.sticky()) {
            height += 1;
        }

        return new Dimension(innerWidth, height);
    }

    private static boolean hasTitleRow(Toast toast) {
        String title = toast.title();
        return title != null && !title.isEmpty();
    }

    private static String titleBandText(Toast toast) {
        StringBuilder band = new StringBuilder();
        band.append(toast.type().name());
        if (toast.titleLayout() == TitleLayout.GAPPED) {
            band.append(' ');
        }
        band.append(toast.titleSeparator().separatorText());
        if (toast.titleLayout() == TitleLayout.GAPPED) {
            band.append(' ');
        }
        band.append(toast.title());
        return band.toString();
    }

    private static String readRow(Buffer buffer, int x, int y, int width) {
        StringBuilder row = new StringBuilder();
        for (int col = 0; col < width; col++) {
            row.append(buffer.get(x + col, y).symbol());
        }
        return trimTrailingSpaces(row.toString());
    }

    private static String trimTrailingSpaces(String text) {
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == ' ') {
            end--;
        }
        return text.substring(0, end);
    }

    /**
     * Width and height of a toast for layout purposes.
     */
    public static final class Dimension {

        private final int width;
        private final int height;

        /**
         * Creates a dimension with the given width and height.
         *
         * @param width the inner content width
         * @param height the total height including borders
         */
        public Dimension(int width, int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Returns the inner content width.
         *
         * @return the width
         */
        public int width() {
            return width;
        }

        /**
         * Returns the total height including borders.
         *
         * @return the height
         */
        public int height() {
            return height;
        }
    }
}

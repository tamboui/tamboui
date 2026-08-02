/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

/**
 * Stateless widget that paints one toast from an immutable {@link ToastRenderSnapshot}.
 */
public final class ToastWidget implements Widget {

    private static final String[] UNICODE_BLOCKS = {
        " ",
        "▏",
        "▎",
        "▍",
        "▌",
        "▋",
        "▊",
        "▉",
        "█"
    };

    /** Shared renderer instance for snapshot-driven painting. */
    public static final ToastWidget INSTANCE = new ToastWidget();

    private final ToastRenderSnapshot snapshot;

    private ToastWidget() {
        this.snapshot = null;
    }

    /**
     * Creates a widget bound to a single render snapshot.
     *
     * @param snapshot the per-frame render model
     */
    public ToastWidget(ToastRenderSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    /**
     * Renders a toast snapshot into the buffer.
     *
     * @param area the target area
     * @param buffer the buffer to paint into
     * @param snapshot the render snapshot
     */
    public void render(Rect area, Buffer buffer, ToastRenderSnapshot snapshot) {
        renderInternal(area, buffer, snapshot);
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (snapshot == null) {
            throw new IllegalStateException("ToastWidget requires a snapshot; use new ToastWidget(snapshot)");
        }
        renderInternal(area, buffer, snapshot);
    }

    private void renderInternal(Rect area, Buffer buffer, ToastRenderSnapshot snapshot) {
        if (area.isEmpty()) {
            return;
        }

        Block block = Block.builder()
                .borders(snapshot.borderMode().borders())
                .borderStyle(snapshot.typeStyle())
                .build();
        block.render(area, buffer);

        Rect inner = block.inner(area);
        if (inner.isEmpty()) {
            return;
        }

        Rect contentArea = contentArea(inner, snapshot.borderMode());
        if (contentArea.isEmpty()) {
            return;
        }

        int row = contentArea.top();
        int contentBottom = contentArea.bottom();

        if (hasTitleRow(snapshot)) {
            renderTitleRow(buffer, contentArea, row, snapshot);
            row++;
        }

        for (String line : snapshot.messageLines()) {
            if (row >= contentBottom) {
                break;
            }
            buffer.setString(contentArea.left(), row, line, snapshot.messageStyle());
            row++;
        }

        if (showsProgress(snapshot) && row < contentBottom) {
            renderProgressRow(buffer, contentArea, row, snapshot);
        }
    }

    private static Rect contentArea(Rect inner, BorderMode borderMode) {
        if (borderMode == BorderMode.FULL) {
            return inner;
        }
        if (inner.height() <= 2) {
            return new Rect(0, 0, 0, 0);
        }
        return new Rect(inner.left(), inner.top() + 1, inner.width(), inner.height() - 2);
    }

    private static boolean hasTitleRow(ToastRenderSnapshot snapshot) {
        String title = snapshot.title();
        return title != null && !title.isEmpty();
    }

    private static boolean showsProgress(ToastRenderSnapshot snapshot) {
        return !Double.isNaN(snapshot.remainingFraction());
    }

    private static void renderTitleRow(Buffer buffer, Rect contentArea, int row, ToastRenderSnapshot snapshot) {
        String text = titleBandText(snapshot);
        int textWidth = CharWidth.of(text);
        int x = contentArea.left();

        if (snapshot.titleAlignment() == TitleAlignment.CENTER) {
            int offset = Math.max(0, (contentArea.width() - textWidth) / 2);
            x += offset;
        }

        if (snapshot.highlightTitle()) {
            Style bandStyle = snapshot.typeStyle().reversed();
            buffer.setStyle(new Rect(contentArea.left(), row, contentArea.width(), 1), bandStyle);
        }

        buffer.setString(x, row, text, snapshot.titleStyle());
    }

    private static String titleBandText(ToastRenderSnapshot snapshot) {
        StringBuilder band = new StringBuilder();
        band.append(snapshot.type().name());
        if (snapshot.titleLayout() == TitleLayout.GAPPED) {
            band.append(' ');
        }
        band.append(snapshot.titleSeparator().separatorText());
        if (snapshot.titleLayout() == TitleLayout.GAPPED) {
            band.append(' ');
        }
        band.append(snapshot.title());
        return band.toString();
    }

    private static void renderProgressRow(Buffer buffer, Rect contentArea, int row, ToastRenderSnapshot snapshot) {
        int width = contentArea.width();
        if (width <= 0) {
            return;
        }

        double fraction = clamp(snapshot.remainingFraction(), 0.0, 1.0);
        Style style = snapshot.progressBarStyle();

        switch (snapshot.progressStyle()) {
            case FULL_BLOCK:
                renderFullBlockProgress(buffer, contentArea.left(), row, width, fraction, style);
                break;
            case HALF_BLOCK:
                renderHalfBlockProgress(buffer, contentArea.left(), row, width, fraction, style);
                break;
            case MINIMAL:
                renderMinimalProgress(buffer, contentArea.left(), row, width, fraction, style);
                break;
            default:
                break;
        }
    }

    private static void renderFullBlockProgress(
            Buffer buffer, int x, int y, int width, double fraction, Style style) {
        double filledWidth = width * fraction;
        int fullCells = (int) filledWidth;

        for (int col = 0; col < fullCells && col < width; col++) {
            buffer.set(x + col, y, new Cell(UNICODE_BLOCKS[8], style));
        }

        if (fullCells < width) {
            double fractional = filledWidth - fullCells;
            int blockIndex = (int) (fractional * 8);
            if (blockIndex > 0) {
                buffer.set(x + fullCells, y, new Cell(UNICODE_BLOCKS[blockIndex], style));
            }
        }
    }

    private static void renderHalfBlockProgress(
            Buffer buffer, int x, int y, int width, double fraction, Style style) {
        double filledWidth = width * fraction;
        int fullCells = (int) filledWidth;

        for (int col = 0; col < fullCells && col < width; col++) {
            buffer.set(x + col, y, new Cell("▀", style));
        }

        if (fullCells < width) {
            double fractional = filledWidth - fullCells;
            String symbol = fractional >= 0.5 ? "▀" : "▄";
            if (fractional > 0.0) {
                buffer.set(x + fullCells, y, new Cell(symbol, style));
            }
        }
    }

    private static void renderMinimalProgress(
            Buffer buffer, int x, int y, int width, double fraction, Style style) {
        int filledCells = (int) Math.ceil(width * fraction);
        for (int col = 0; col < filledCells && col < width; col++) {
            buffer.set(x + col, y, new Cell("-", style));
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

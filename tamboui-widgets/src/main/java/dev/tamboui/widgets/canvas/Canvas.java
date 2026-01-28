/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

import java.util.List;
import java.util.function.Consumer;

/**
 * A widget for drawing arbitrary shapes on a terminal grid.
 * <p>
 * The Canvas widget provides a mathematical coordinate system where
 * shapes can be drawn using floating-point coordinates. The widget
 * handles the transformation from canvas space to terminal cells.
 *
 * <pre>{@code
 * Canvas canvas = Canvas.builder()
 *     .xBounds(-180, 180)
 *     .yBounds(-90, 90)
 *     .marker(Marker.BRAILLE)
 *     .block(Block.bordered().title(Title.from("World Map")))
 *     .paint(ctx -> {
 *         ctx.draw(new Circle(0, 0, 50, Color.RED));
 *         ctx.draw(new Line(-50, -50, 50, 50, Color.GREEN));
 *         ctx.print(0, 0, "Center");
 *     })
 *     .build();
 * }</pre>
 *
 * @see Shape
 * @see Context
 * @see Marker
 */
public final class Canvas implements Widget {

    // Braille character base (Unicode 0x2800)
    private static final int BRAILLE_BASE = 0x2800;

    // Braille dot positions (2x4 grid mapped to bits)
    // Column 0: dots 1,2,3,7 (bits 0,1,2,6)
    // Column 1: dots 4,5,6,8 (bits 3,4,5,7)
    private static final int[][] BRAILLE_DOTS = {
        {0x01, 0x02, 0x04, 0x40},  // Column 0: bits for rows 0-3
        {0x08, 0x10, 0x20, 0x80}   // Column 1: bits for rows 0-3
    };

    private final double[] xBounds;
    private final double[] yBounds;
    private final Marker marker;
    private final Block block;
    private final Color backgroundColor;
    private final Consumer<Context> paintCallback;

    private Canvas(Builder builder) {
        this.xBounds = builder.xBounds;
        this.yBounds = builder.yBounds;
        this.marker = builder.marker;
        this.block = builder.block;
        this.backgroundColor = builder.backgroundColor;
        this.paintCallback = builder.paintCallback;
    }

    /**
     * Creates a new canvas builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        // Render block if present
        Rect canvasArea = area;
        if (block != null) {
            block.render(area, buffer);
            canvasArea = block.inner(area);
        }

        if (canvasArea.isEmpty()) {
            return;
        }

        // Apply background color
        if (backgroundColor != null) {
            buffer.setStyle(canvasArea, Style.EMPTY.bg(backgroundColor));
        }

        // Create context and execute paint callback
        Context ctx = new Context(
            canvasArea.width(),
            canvasArea.height(),
            xBounds,
            yBounds,
            marker
        );

        if (paintCallback != null) {
            paintCallback.accept(ctx);
        }

        // Render the grid based on marker type
        renderGrid(buffer, canvasArea, ctx);

        // Render labels
        renderLabels(buffer, canvasArea, ctx);
    }

    private void renderGrid(Buffer buffer, Rect area, Context ctx) {
        List<Color[][]> layers = ctx.allLayers();

        switch (marker) {
            case BRAILLE:
                renderBraille(buffer, area, layers);
                break;
            case HALF_BLOCK:
                renderHalfBlock(buffer, area, layers);
                break;
            case DOT:
                renderSimple(buffer, area, layers, "•");
                break;
            case BLOCK:
                renderSimple(buffer, area, layers, "█");
                break;
            case BAR:
                renderSimple(buffer, area, layers, "▄");
                break;
            default:
                break;
        }
    }

    private void renderBraille(Buffer buffer, Rect area, List<Color[][]> layers) {
        // Braille: 2x4 dots per cell
        for (int cellY = 0; cellY < area.height(); cellY++) {
            for (int cellX = 0; cellX < area.width(); cellX++) {
                int pattern = 0;
                Color cellColor = null;

                // Check all 8 dots in this cell (2 columns, 4 rows)
                for (int dy = 0; dy < 4; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        int gridX = cellX * 2 + dx;
                        int gridY = cellY * 4 + dy;

                        Color color = getLayerColor(layers, gridX, gridY);
                        if (color != null) {
                            pattern |= BRAILLE_DOTS[dx][dy];
                            if (cellColor == null) {
                                cellColor = color;
                            }
                        }
                    }
                }

                if (pattern != 0) {
                    String brailleChar = String.valueOf((char) (BRAILLE_BASE + pattern));
                    Style style = cellColor != null ? Style.EMPTY.fg(cellColor) : Style.EMPTY;
                    buffer.setString(area.x() + cellX, area.y() + cellY, brailleChar, style);
                }
            }
        }
    }

    private void renderHalfBlock(Buffer buffer, Rect area, List<Color[][]> layers) {
        // Half-block: 2 half-blocks per cell (top and bottom)
        for (int cellY = 0; cellY < area.height(); cellY++) {
            for (int cellX = 0; cellX < area.width(); cellX++) {
                int gridY = cellY * 2;
                Color topColor = getLayerColor(layers, cellX, gridY);
                Color bottomColor = getLayerColor(layers, cellX, gridY + 1);

                if (topColor != null || bottomColor != null) {
                    String symbol;
                    Style style;

                    if (topColor != null && bottomColor != null) {
                        // Both halves filled - use full block with top color
                        symbol = "█";
                        style = Style.EMPTY.fg(topColor);
                    } else if (topColor != null) {
                        // Only top half - use upper half block
                        symbol = "▀";
                        style = Style.EMPTY.fg(topColor);
                    } else {
                        // Only bottom half - use lower half block
                        symbol = "▄";
                        style = Style.EMPTY.fg(bottomColor);
                    }

                    buffer.setString(area.x() + cellX, area.y() + cellY, symbol, style);
                }
            }
        }
    }

    private void renderSimple(Buffer buffer, Rect area, List<Color[][]> layers, String symbol) {
        // Simple markers: 1 point per cell
        for (int cellY = 0; cellY < area.height(); cellY++) {
            for (int cellX = 0; cellX < area.width(); cellX++) {
                Color color = getLayerColor(layers, cellX, cellY);
                if (color != null) {
                    Style style = Style.EMPTY.fg(color);
                    buffer.setString(area.x() + cellX, area.y() + cellY, symbol, style);
                }
            }
        }
    }

    private Color getLayerColor(List<Color[][]> layers, int x, int y) {
        // Check layers from top to bottom
        for (int i = layers.size() - 1; i >= 0; i--) {
            Color[][] layer = layers.get(i);
            if (y < layer.length && x < layer[y].length) {
                Color color = layer[y][x];
                if (color != null) {
                    return color;
                }
            }
        }
        return null;
    }

    private void renderLabels(Buffer buffer, Rect area, Context ctx) {
        for (Context.Label label : ctx.labels()) {
            // Convert canvas coordinates to screen coordinates
            double xRange = xBounds[1] - xBounds[0];
            double yRange = yBounds[1] - yBounds[0];

            if (xRange == 0 || yRange == 0) {
                continue;
            }

            int screenX = (int) Math.round((label.x() - xBounds[0]) / xRange * (area.width() - 1));
            int screenY = (int) Math.round((yBounds[1] - label.y()) / yRange * (area.height() - 1));

            screenX = area.x() + Math.max(0, Math.min(area.width() - 1, screenX));
            screenY = area.y() + Math.max(0, Math.min(area.height() - 1, screenY));

            // Render the line
            Line line = label.line();
            int x = screenX;
            List<Span> spans = line.spans();
            for (int i = 0; i < spans.size(); i++) {
                Span span = spans.get(i);
                buffer.setString(x, screenY, span.content(), span.style());
                x += span.width();
            }
        }
    }

    /**
     * Builder for {@link Canvas}.
     */
    public static final class Builder {
        private double[] xBounds = {0.0, 1.0};
        private double[] yBounds = {0.0, 1.0};
        private Marker marker = Marker.BRAILLE;
        private Block block;
        private Color backgroundColor;
        private Consumer<Context> paintCallback;

        private Builder() {}

        /**
         * Sets the x-axis bounds.
         *
         * @param min the minimum x value
         * @param max the maximum x value
         * @return this builder
         */
        public Builder xBounds(double min, double max) {
            this.xBounds = new double[] {min, max};
            return this;
        }

        /**
         * Sets the y-axis bounds.
         *
         * @param min the minimum y value
         * @param max the maximum y value
         * @return this builder
         */
        public Builder yBounds(double min, double max) {
            this.yBounds = new double[] {min, max};
            return this;
        }

        /**
         * Sets the marker type for rendering points.
         *
         * @param marker the marker type
         * @return this builder
         */
        public Builder marker(Marker marker) {
            this.marker = marker != null ? marker : Marker.BRAILLE;
            return this;
        }

        /**
         * Wraps the canvas in a block.
         *
         * @param block the block to wrap the canvas in
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the background color.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder backgroundColor(Color color) {
            this.backgroundColor = color;
            return this;
        }

        /**
         * Sets the paint callback for drawing shapes.
         * <p>
         * The callback receives a {@link Context} that can be used to
         * draw shapes and print text.
         *
         * @param callback the paint callback
         * @return this builder
         */
        public Builder paint(Consumer<Context> callback) {
            this.paintCallback = callback;
            return this;
        }

        /**
         * Builds the canvas.
         *
         * @return a new canvas instance
         */
        public Canvas build() {
            return new Canvas(this);
        }
    }
}

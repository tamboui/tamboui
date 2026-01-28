/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas;

import dev.tamboui.style.Color;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

import java.util.ArrayList;
import java.util.List;

/**
 * The drawing context for a {@link Canvas}.
 * <p>
 * Maintains the painting state during canvas operations, including
 * the grid of points, text labels, and layer management.
 * <p>
 * Applications typically don't create Context instances directly.
 * Instead, the Canvas widget creates and passes a Context to the
 * paint callback.
 *
 * @see Canvas
 * @see Painter
 */
public class Context {

    private final int width;
    private final int height;
    private final double[] xBounds;
    private final double[] yBounds;
    private final Marker marker;

    // Grid dimensions depend on marker resolution
    private final int gridWidth;
    private final int gridHeight;

    // Grid of colors (null = transparent)
    private Color[][] grid;
    private final List<Color[][]> layers;
    private final List<Label> labels;

    /**
     * Creates a new drawing context.
     *
     * @param width    the terminal width in cells
     * @param height   the terminal height in cells
     * @param xBounds  the x-axis bounds [min, max]
     * @param yBounds  the y-axis bounds [min, max]
     * @param marker   the marker type for rendering
     */
    public Context(int width, int height, double[] xBounds, double[] yBounds, Marker marker) {
        this.width = width;
        this.height = height;
        this.xBounds = xBounds.clone();
        this.yBounds = yBounds.clone();
        this.marker = marker;

        // Calculate grid dimensions based on marker resolution
        this.gridWidth = calculateGridWidth(width, marker);
        this.gridHeight = calculateGridHeight(height, marker);

        this.grid = new Color[gridHeight][gridWidth];
        this.layers = new ArrayList<>();
        this.labels = new ArrayList<>();
    }

    private static int calculateGridWidth(int width, Marker marker) {
        switch (marker) {
            case BRAILLE:
                return width * 2;  // 2 dots horizontally per cell
            default:
                return width;
        }
    }

    private static int calculateGridHeight(int height, Marker marker) {
        switch (marker) {
            case BRAILLE:
                return height * 4;     // 4 dots vertically per cell
            case HALF_BLOCK:
                return height * 2;  // 2 half-blocks per cell
            default:
                return height;
        }
    }

    /**
     * Draws a shape on this context.
     *
     * @param shape the shape to draw
     */
    public void draw(Shape shape) {
        Painter painter = new Painter(this);
        shape.draw(painter);
    }

    /**
     * Prints text at the specified canvas coordinates.
     * <p>
     * Text is rendered on top of shapes and is not affected by layering.
     *
     * @param x    the x coordinate in canvas space
     * @param y    the y coordinate in canvas space
     * @param text the text to print
     */
    public void print(double x, double y, String text) {
        print(x, y, Line.from(text));
    }

    /**
     * Prints styled text at the specified canvas coordinates.
     *
     * @param x    the x coordinate in canvas space
     * @param y    the y coordinate in canvas space
     * @param line the styled line to print
     */
    public void print(double x, double y, Line line) {
        labels.add(new Label(x, y, line));
    }

    /**
     * Prints a styled span at the specified canvas coordinates.
     *
     * @param x    the x coordinate in canvas space
     * @param y    the y coordinate in canvas space
     * @param span the styled span to print
     */
    public void print(double x, double y, Span span) {
        labels.add(new Label(x, y, Line.from(span)));
    }

    /**
     * Saves the current grid as a layer and resets for subsequent drawing.
     * <p>
     * Layers are composited from bottom to top when rendering.
     */
    public void layer() {
        layers.add(grid);
        grid = new Color[gridHeight][gridWidth];
    }

    /**
     * Paints a point at grid coordinates.
     */
    void paint(int x, int y, Color color) {
        if (x >= 0 && x < gridWidth && y >= 0 && y < gridHeight) {
            grid[y][x] = color;
        }
    }

    /**
     * Returns the x-axis bounds.
     */
    double[] xBounds() {
        return xBounds;
    }

    /**
     * Returns the y-axis bounds.
     */
    double[] yBounds() {
        return yBounds;
    }

    /**
     * Returns the grid width (in marker units).
     */
    int gridWidth() {
        return gridWidth;
    }

    /**
     * Returns the grid height (in marker units).
     */
    int gridHeight() {
        return gridHeight;
    }

    /**
     * Returns the terminal width in cells.
     */
    int width() {
        return width;
    }

    /**
     * Returns the terminal height in cells.
     */
    int height() {
        return height;
    }

    /**
     * Returns the marker type.
     */
    Marker marker() {
        return marker;
    }

    /**
     * Returns the current grid plus all saved layers.
     *
     * @return a list containing all saved layers and the current grid
     */
    public List<Color[][]> allLayers() {
        List<Color[][]> all = new ArrayList<>(layers);
        all.add(grid);
        return all;
    }

    /**
     * Returns all text labels.
     */
    List<Label> labels() {
        return labels;
    }

    /**
     * A text label at canvas coordinates.
     */
    static final class Label {
        private final double x;
        private final double y;
        private final Line line;

        Label(double x, double y, Line line) {
            this.x = x;
            this.y = y;
            this.line = line;
        }

        double x() {
            return x;
        }

        double y() {
            return y;
        }

        Line line() {
            return line;
        }
    }
}

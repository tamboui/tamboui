/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.chart;

import dev.tamboui.style.Style;
import dev.tamboui.text.Line;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A dataset to be plotted in a {@link Chart}.
 * <p>
 * Contains data points (x, y coordinates) and rendering configuration.
 *
 * <pre>{@code
 * Dataset dataset = Dataset.builder()
 *     .name("Temperature")
 *     .data(new double[][] {{0, 20}, {1, 22}, {2, 25}, {3, 23}})
 *     .graphType(GraphType.LINE)
 *     .marker(Dataset.Marker.BRAILLE)
 *     .style(Style.EMPTY.fg(Color.RED))
 *     .build();
 * }</pre>
 */
public final class Dataset {

    /**
     * Marker symbols for data points.
     */
    public enum Marker {
        /** Dot marker: • */
        DOT("•"),
        /** Block marker: █ */
        BLOCK("█"),
        /** Bar marker: ▄ */
        BAR("▄"),
        /** Braille patterns for high-resolution plotting */
        BRAILLE("⣿");

        private final String symbol;

        Marker(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Returns the marker symbol.
         *
         * @return the marker symbol string
         */
        public String symbol() {
            return symbol;
        }
    }

    private final Line name;
    private final double[][] data;
    private final Marker marker;
    private final GraphType graphType;
    private final Style style;

    private Dataset(Builder builder) {
        this.name = builder.name;
        this.data = builder.data;
        this.marker = builder.marker;
        this.graphType = builder.graphType;
        this.style = builder.style;
    }

    /**
     * Creates a new dataset builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a dataset with just data points.
     *
     * @param data the data points as [x, y] pairs
     * @return a new dataset
     */
    public static Dataset of(double[][] data) {
        return builder().data(data).build();
    }

    /**
     * Creates a named dataset with data points.
     *
     * @param name the dataset name
     * @param data the data points as [x, y] pairs
     * @return a new dataset
     */
    public static Dataset of(String name, double[][] data) {
        return builder().name(name).data(data).build();
    }

    /**
     * Returns the dataset name.
     *
     * @return the dataset name, or empty if not set
     */
    public Optional<Line> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the data points as [x, y] pairs.
     *
     * @return the data points array
     */
    public double[][] data() {
        return data;
    }

    /**
     * Returns the number of data points.
     *
     * @return the number of data points
     */
    public int size() {
        return data.length;
    }

    /**
     * Returns the marker type.
     *
     * @return the marker type
     */
    public Marker marker() {
        return marker;
    }

    /**
     * Returns the graph type.
     *
     * @return the graph type
     */
    public GraphType graphType() {
        return graphType;
    }

    /**
     * Returns the style.
     *
     * @return the style, or {@link Style#EMPTY} if not set
     */
    public Style style() {
        return style != null ? style : Style.EMPTY;
    }

    /**
     * Returns true if this dataset has a name.
     *
     * @return {@code true} if this dataset has a name
     */
    public boolean hasName() {
        return name != null;
    }

    /**
     * Builder for {@link Dataset}.
     */
    public static final class Builder {
        private Line name;
        private double[][] data = new double[0][];
        private Marker marker = Marker.DOT;
        private GraphType graphType = GraphType.SCATTER;
        private Style style;

        private Builder() {}

        /**
         * Sets the dataset name (for legend).
         *
         * @param name the dataset name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name != null ? Line.from(name) : null;
            return this;
        }

        /**
         * Sets the dataset name.
         *
         * @param name the dataset name as a styled line
         * @return this builder
         */
        public Builder name(Line name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the data points as [x, y] pairs.
         *
         * @param data the data points as [x, y] pairs
         * @return this builder
         */
        public Builder data(double[][] data) {
            if (data == null) {
                this.data = new double[0][];
            } else {
                this.data = new double[data.length][];
                for (int i = 0; i < data.length; i++) {
                    this.data[i] = data[i].clone();
                }
            }
            return this;
        }

        /**
         * Sets the data points from a list of [x, y] pairs.
         *
         * @param data the data points as a list of [x, y] pairs
         * @return this builder
         */
        public Builder data(List<double[]> data) {
            if (data == null || data.isEmpty()) {
                this.data = new double[0][];
            } else {
                this.data = new double[data.size()][];
                for (int i = 0; i < data.size(); i++) {
                    this.data[i] = data.get(i).clone();
                }
            }
            return this;
        }

        /**
         * Adds a single data point.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @return this builder
         */
        public Builder addPoint(double x, double y) {
            double[][] newData = Arrays.copyOf(this.data, this.data.length + 1);
            newData[this.data.length] = new double[] {x, y};
            this.data = newData;
            return this;
        }

        /**
         * Sets the marker type for data points.
         *
         * @param marker the marker type
         * @return this builder
         */
        public Builder marker(Marker marker) {
            this.marker = marker != null ? marker : Marker.DOT;
            return this;
        }

        /**
         * Sets the graph type.
         *
         * @param graphType the graph type
         * @return this builder
         */
        public Builder graphType(GraphType graphType) {
            this.graphType = graphType != null ? graphType : GraphType.SCATTER;
            return this;
        }

        /**
         * Sets the dataset style.
         *
         * @param style the style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Builds the dataset.
         *
         * @return the new dataset
         */
        public Dataset build() {
            return new Dataset(this);
        }
    }
}

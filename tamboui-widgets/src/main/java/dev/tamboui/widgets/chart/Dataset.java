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
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a dataset with just data points.
     */
    public static Dataset of(double[][] data) {
        return builder().data(data).build();
    }

    /**
     * Creates a named dataset with data points.
     */
    public static Dataset of(String name, double[][] data) {
        return builder().name(name).data(data).build();
    }

    /**
     * Returns the dataset name.
     */
    public Optional<Line> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the data points as [x, y] pairs.
     */
    public double[][] data() {
        return data;
    }

    /**
     * Returns the number of data points.
     */
    public int size() {
        return data.length;
    }

    /**
     * Returns the marker type.
     */
    public Marker marker() {
        return marker;
    }

    /**
     * Returns the graph type.
     */
    public GraphType graphType() {
        return graphType;
    }

    /**
     * Returns the style.
     */
    public Style style() {
        return style != null ? style : Style.EMPTY;
    }

    /**
     * Returns true if this dataset has a name.
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
         */
        public Builder name(String name) {
            this.name = name != null ? Line.from(name) : null;
            return this;
        }

        /**
         * Sets the dataset name.
         */
        public Builder name(Line name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the data points as [x, y] pairs.
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
         */
        public Builder addPoint(double x, double y) {
            double[][] newData = Arrays.copyOf(this.data, this.data.length + 1);
            newData[this.data.length] = new double[] {x, y};
            this.data = newData;
            return this;
        }

        /**
         * Sets the marker type for data points.
         */
        public Builder marker(Marker marker) {
            this.marker = marker != null ? marker : Marker.DOT;
            return this;
        }

        /**
         * Sets the graph type.
         */
        public Builder graphType(GraphType graphType) {
            this.graphType = graphType != null ? graphType : GraphType.SCATTER;
            return this;
        }

        /**
         * Sets the dataset style.
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Builds the dataset.
         */
        public Dataset build() {
            return new Dataset(this);
        }
    }
}

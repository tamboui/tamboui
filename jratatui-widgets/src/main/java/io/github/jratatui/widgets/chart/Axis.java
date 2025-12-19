/*
 * Copyright (c) 2025 JRatatui Contributors
 * SPDX-License-Identifier: MIT
 */
package io.github.jratatui.widgets.chart;

import io.github.jratatui.layout.Alignment;
import io.github.jratatui.style.Style;
import io.github.jratatui.text.Line;
import io.github.jratatui.text.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * An axis configuration for a {@link Chart}.
 * <p>
 * Defines the title, bounds, labels, and styling for an axis.
 *
 * <pre>{@code
 * var xAxis = Axis.builder()
 *     .title("Time (s)")
 *     .bounds(0, 100)
 *     .labels("0", "25", "50", "75", "100")
 *     .style(Style.EMPTY.fg(Color.CYAN))
 *     .build();
 * }</pre>
 */
public final class Axis {

    private final Line title;
    private final double[] bounds;
    private final List<Span> labels;
    private final Style style;
    private final Alignment labelsAlignment;

    private Axis(Builder builder) {
        this.title = builder.title;
        this.bounds = builder.bounds;
        this.labels = List.copyOf(builder.labels);
        this.style = builder.style;
        this.labelsAlignment = builder.labelsAlignment;
    }

    /**
     * Creates a new axis builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an axis with default settings.
     */
    public static Axis defaults() {
        return builder().build();
    }

    /**
     * Returns the axis title.
     */
    public Optional<Line> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the axis bounds [min, max].
     */
    public double[] bounds() {
        return bounds.clone();
    }

    /**
     * Returns the minimum bound.
     */
    public double min() {
        return bounds[0];
    }

    /**
     * Returns the maximum bound.
     */
    public double max() {
        return bounds[1];
    }

    /**
     * Returns the axis labels.
     */
    public List<Span> labels() {
        return labels;
    }

    /**
     * Returns the axis style.
     */
    public Style style() {
        return style != null ? style : Style.EMPTY;
    }

    /**
     * Returns the labels alignment.
     */
    public Alignment labelsAlignment() {
        return labelsAlignment;
    }

    /**
     * Returns true if this axis has labels.
     */
    public boolean hasLabels() {
        return !labels.isEmpty();
    }

    /**
     * Returns the range (max - min).
     */
    public double range() {
        return bounds[1] - bounds[0];
    }

    /**
     * Builder for {@link Axis}.
     */
    public static final class Builder {
        private Line title;
        private double[] bounds = {0.0, 0.0};
        private final List<Span> labels = new ArrayList<>();
        private Style style;
        private Alignment labelsAlignment = Alignment.LEFT;

        private Builder() {}

        /**
         * Sets the axis title.
         */
        public Builder title(String title) {
            this.title = title != null ? Line.from(title) : null;
            return this;
        }

        /**
         * Sets the axis title.
         */
        public Builder title(Line title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the axis title with styling.
         */
        public Builder title(Span... spans) {
            this.title = Line.from(spans);
            return this;
        }

        /**
         * Sets the axis bounds [min, max].
         */
        public Builder bounds(double min, double max) {
            this.bounds = new double[] {min, max};
            return this;
        }

        /**
         * Sets the axis bounds.
         */
        public Builder bounds(double[] bounds) {
            if (bounds != null && bounds.length >= 2) {
                this.bounds = new double[] {bounds[0], bounds[1]};
            }
            return this;
        }

        /**
         * Sets the axis labels.
         */
        public Builder labels(String... labels) {
            this.labels.clear();
            if (labels != null) {
                for (String label : labels) {
                    this.labels.add(Span.raw(label));
                }
            }
            return this;
        }

        /**
         * Sets the axis labels.
         */
        public Builder labels(Span... labels) {
            this.labels.clear();
            if (labels != null) {
                this.labels.addAll(List.of(labels));
            }
            return this;
        }

        /**
         * Sets the axis labels.
         */
        public Builder labels(List<Span> labels) {
            this.labels.clear();
            if (labels != null) {
                this.labels.addAll(labels);
            }
            return this;
        }

        /**
         * Adds a label.
         */
        public Builder addLabel(String label) {
            this.labels.add(Span.raw(label));
            return this;
        }

        /**
         * Adds a label.
         */
        public Builder addLabel(Span label) {
            this.labels.add(label);
            return this;
        }

        /**
         * Sets the axis style.
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the labels alignment.
         */
        public Builder labelsAlignment(Alignment alignment) {
            this.labelsAlignment = alignment != null ? alignment : Alignment.LEFT;
            return this;
        }

        /**
         * Builds the axis.
         */
        public Axis build() {
            return new Axis(this);
        }
    }
}

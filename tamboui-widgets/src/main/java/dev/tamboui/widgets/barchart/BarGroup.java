/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.barchart;

import dev.tamboui.text.Line;
import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A group of {@link Bar}s in a {@link BarChart}.
 * <p>
 * Groups allow organizing related bars together with an optional group label.
 *
 * <pre>{@code
 * // Simple group from values
 * BarGroup group = BarGroup.of(10, 20, 30);
 *
 * // Group with label
 * BarGroup group2 = BarGroup.builder()
 *     .label("Q1")
 *     .bars(
 *         Bar.of(100, "Jan"),
 *         Bar.of(150, "Feb"),
 *         Bar.of(120, "Mar")
 *     )
 *     .build();
 * }</pre>
 */
public final class BarGroup {

    private final List<Bar> bars;
    private final Line label;

    private BarGroup(Builder builder) {
        this.bars = listCopyOf(builder.bars);
        this.label = builder.label;
    }

    /**
     * Creates a group from bar values.
     */
    public static BarGroup of(long... values) {
        Builder builder = builder();
        for (long value : values) {
            builder.addBar(Bar.of(value));
        }
        return builder.build();
    }

    /**
     * Creates a group from bars.
     */
    public static BarGroup of(Bar... bars) {
        return builder().bars(bars).build();
    }

    /**
     * Creates a group from bars with a label.
     */
    public static BarGroup of(String label, Bar... bars) {
        return builder().label(label).bars(bars).build();
    }

    /**
     * Creates a new bar group builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the bars in this group.
     */
    public List<Bar> bars() {
        return bars;
    }

    /**
     * Returns the group's label, if set.
     */
    public Optional<Line> label() {
        return Optional.ofNullable(label);
    }

    /**
     * Returns the number of bars in this group.
     */
    public int size() {
        return bars.size();
    }

    /**
     * Returns the maximum value among all bars in this group.
     */
    public long maxValue() {
        return bars.stream()
            .mapToLong(Bar::value)
            .max()
            .orElse(0);
    }

    /**
     * Builder for {@link BarGroup}.
     */
    public static final class Builder {
        private final List<Bar> bars = new ArrayList<>();
        private Line label;

        private Builder() {}

        /**
         * Sets the bars in this group.
         */
        public Builder bars(Bar... bars) {
            this.bars.clear();
            if (bars != null) {
                this.bars.addAll(Arrays.asList(bars));
            }
            return this;
        }

        /**
         * Sets the bars in this group.
         */
        public Builder bars(List<Bar> bars) {
            this.bars.clear();
            if (bars != null) {
                this.bars.addAll(bars);
            }
            return this;
        }

        /**
         * Adds a bar to this group.
         */
        public Builder addBar(Bar bar) {
            if (bar != null) {
                this.bars.add(bar);
            }
            return this;
        }

        /**
         * Adds a bar with the given value.
         */
        public Builder addBar(long value) {
            return addBar(Bar.of(value));
        }

        /**
         * Adds a bar with the given value and label.
         */
        public Builder addBar(long value, String label) {
            return addBar(Bar.of(value, label));
        }

        /**
         * Sets the group's label.
         */
        public Builder label(String label) {
            this.label = label != null ? Line.from(label) : null;
            return this;
        }

        /**
         * Sets the group's label.
         */
        public Builder label(Line label) {
            this.label = label;
            return this;
        }

        /**
         * Builds the bar group.
         */
        public BarGroup build() {
            return new BarGroup(this);
        }
    }
}

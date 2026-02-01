/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.barchart;

import dev.tamboui.style.Style;
import dev.tamboui.text.Line;

import java.util.Optional;

/**
 * A single bar in a {@link BarChart}.
 * <p>
 * Each bar has a value, optional label, and optional styling.
 *
 * <pre>{@code
 * // Simple bar with just a value
 * Bar bar = Bar.of(75);
 *
 * // Bar with label
 * Bar bar2 = Bar.of(75, "Sales");
 *
 * // Fully customized bar
 * Bar bar3 = Bar.builder()
 *     .value(75)
 *     .label("Sales")
 *     .textValue("75%")
 *     .style(Style.EMPTY.fg(Color.GREEN))
 *     .build();
 * }</pre>
 */
public final class Bar {

    private final long value;
    private final Line label;
    private final String textValue;
    private final Style style;
    private final Style valueStyle;

    private Bar(Builder builder) {
        this.value = builder.value;
        this.label = builder.label;
        this.textValue = builder.textValue;
        this.style = builder.style;
        this.valueStyle = builder.valueStyle;
    }

    /**
     * Creates a bar with the given value.
     *
     * @param value the bar value
     * @return a new bar
     */
    public static Bar of(long value) {
        return builder().value(value).build();
    }

    /**
     * Creates a bar with the given value and label.
     *
     * @param value the bar value
     * @param label the bar label
     * @return a new bar
     */
    public static Bar of(long value, String label) {
        return builder().value(value).label(label).build();
    }

    /**
     * Creates a new bar builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the bar's value.
     *
     * @return the bar's value
     */
    public long value() {
        return value;
    }

    /**
     * Returns the bar's label, if set.
     *
     * @return the bar's label
     */
    public Optional<Line> label() {
        return Optional.ofNullable(label);
    }

    /**
     * Returns the custom text value, if set.
     *
     * @return the custom text value
     */
    public Optional<String> textValue() {
        return Optional.ofNullable(textValue);
    }

    /**
     * Returns the bar's style, if set.
     *
     * @return the bar's style
     */
    public Optional<Style> style() {
        return Optional.ofNullable(style);
    }

    /**
     * Returns the value's style, if set.
     *
     * @return the value's style
     */
    public Optional<Style> valueStyle() {
        return Optional.ofNullable(valueStyle);
    }

    /**
     * Returns the display string for this bar's value.
     *
     * @return the display value
     */
    public String displayValue() {
        return textValue != null ? textValue : String.valueOf(value);
    }

    /**
     * Builder for {@link Bar}.
     */
    public static final class Builder {
        private long value;
        private Line label;
        private String textValue;
        private Style style;
        private Style valueStyle;

        private Builder() {}

        /**
         * Sets the bar's value.
         *
         * @param value the value
         * @return this builder
         */
        public Builder value(long value) {
            this.value = Math.max(0, value);
            return this;
        }

        /**
         * Sets the bar's label.
         *
         * @param label the label
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label != null ? Line.from(label) : null;
            return this;
        }

        /**
         * Sets the bar's label.
         *
         * @param label the label
         * @return this builder
         */
        public Builder label(Line label) {
            this.label = label;
            return this;
        }

        /**
         * Sets a custom text value to display instead of the numeric value.
         *
         * @param textValue the text value
         * @return this builder
         */
        public Builder textValue(String textValue) {
            this.textValue = textValue;
            return this;
        }

        /**
         * Sets the bar's style.
         *
         * @param style the style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the value's style.
         *
         * @param valueStyle the value style
         * @return this builder
         */
        public Builder valueStyle(Style valueStyle) {
            this.valueStyle = valueStyle;
            return this;
        }

        /**
         * Builds the bar.
         *
         * @return the built bar
         */
        public Bar build() {
            return new Bar(this);
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.select;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.widget.StatefulWidget;

import java.util.Optional;

/**
 * An inline select widget that displays the current selection with navigation indicators.
 *
 * <p>The widget shows the currently selected option with optional left/right indicators
 * to suggest navigation, e.g., "◄ Option A ►" or "&lt; Option A &gt;".
 *
 * <pre>{@code
 * // Create a select with default style: < Option >
 * Select select = Select.builder().build();
 *
 * // Create a select with custom indicators
 * Select select = Select.builder()
 *     .leftIndicator("◄ ")
 *     .rightIndicator(" ►")
 *     .selectedColor(Color.CYAN)
 *     .build();
 *
 * // Render with state
 * SelectState state = new SelectState("Option A", "Option B", "Option C");
 * select.render(area, buffer, state);
 * }</pre>
 *
 * <h2>CSS Properties</h2>
 * <ul>
 *   <li>{@code select-left-indicator} - Left navigation indicator (default: "&lt; ")</li>
 *   <li>{@code select-right-indicator} - Right navigation indicator (default: " >")</li>
 *   <li>{@code select-selected-color} - Foreground color for selected value</li>
 *   <li>{@code select-indicator-color} - Foreground color for indicators</li>
 * </ul>
 *
 * @see SelectState
 */
public final class Select implements StatefulWidget<SelectState> {

    /** Default left navigation indicator. */
    public static final String DEFAULT_LEFT_INDICATOR = "< ";

    /** Default right navigation indicator. */
    public static final String DEFAULT_RIGHT_INDICATOR = " >";

    /**
     * CSS property for the left navigation indicator.
     */
    public static final PropertyDefinition<String> LEFT_INDICATOR =
            PropertyDefinition.builder("select-left-indicator", Optional::of)
                    .defaultValue(DEFAULT_LEFT_INDICATOR)
                    .build();

    /**
     * CSS property for the right navigation indicator.
     */
    public static final PropertyDefinition<String> RIGHT_INDICATOR =
            PropertyDefinition.builder("select-right-indicator", Optional::of)
                    .defaultValue(DEFAULT_RIGHT_INDICATOR)
                    .build();

    /**
     * CSS property for the selected value foreground color.
     */
    public static final PropertyDefinition<Color> SELECTED_COLOR =
            PropertyDefinition.of("select-selected-color", ColorConverter.INSTANCE);

    /**
     * CSS property for the indicator foreground color.
     */
    public static final PropertyDefinition<Color> INDICATOR_COLOR =
            PropertyDefinition.of("select-indicator-color", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(LEFT_INDICATOR, RIGHT_INDICATOR, SELECTED_COLOR, INDICATOR_COLOR);
    }

    private final String leftIndicator;
    private final String rightIndicator;
    private final Style selectedStyle;
    private final Style indicatorStyle;

    private Select(Builder builder) {
        this.leftIndicator = builder.resolveLeftIndicator();
        this.rightIndicator = builder.resolveRightIndicator();

        Color selectedColor = builder.resolveSelectedColor();
        Color indicatorColor = builder.resolveIndicatorColor();

        this.selectedStyle = selectedColor != null
                ? builder.style.fg(selectedColor)
                : builder.style;
        this.indicatorStyle = indicatorColor != null
                ? builder.style.fg(indicatorColor)
                : builder.style.fg(Color.DARK_GRAY);
    }

    /**
     * Creates a new builder for Select.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer, SelectState state) {
        if (area.isEmpty()) {
            return;
        }

        String value = state.selectedValue();
        int x = area.x();
        int maxX = area.x() + area.width();

        // Render left indicator
        if (x < maxX && !leftIndicator.isEmpty()) {
            String indicator = truncate(leftIndicator, maxX - x);
            buffer.setString(x, area.y(), indicator, indicatorStyle);
            x += indicator.length();
        }

        // Render selected value
        if (x < maxX && !value.isEmpty()) {
            String displayValue = truncate(value, maxX - x - rightIndicator.length());
            if (displayValue.isEmpty() && maxX - x > 0) {
                displayValue = truncate(value, maxX - x);
            }
            buffer.setString(x, area.y(), displayValue, selectedStyle);
            x += displayValue.length();
        }

        // Render right indicator
        if (x < maxX && !rightIndicator.isEmpty()) {
            String indicator = truncate(rightIndicator, maxX - x);
            buffer.setString(x, area.y(), indicator, indicatorStyle);
        }
    }

    private static String truncate(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        return text.length() > maxWidth ? text.substring(0, maxWidth) : text;
    }

    /**
     * Returns the left navigation indicator.
     *
     * @return the left indicator
     */
    public String leftIndicator() {
        return leftIndicator;
    }

    /**
     * Returns the right navigation indicator.
     *
     * @return the right indicator
     */
    public String rightIndicator() {
        return rightIndicator;
    }

    /**
     * Returns the display width for the given state.
     *
     * @param state the select state
     * @return the width needed to render the current selection
     */
    public int width(SelectState state) {
        return leftIndicator.length() + state.selectedValue().length() + rightIndicator.length();
    }

    /**
     * Returns the minimum width (indicators only, no value).
     *
     * @return the minimum width
     */
    public int minWidth() {
        return leftIndicator.length() + rightIndicator.length();
    }

    /**
     * Builder for {@link Select}.
     */
    public static final class Builder {
        private String leftIndicator;
        private String rightIndicator;
        private Color selectedColor;
        private Color indicatorColor;
        private Style style = Style.EMPTY;
        private StylePropertyResolver styleResolver;

        private Builder() {
        }

        /**
         * Sets the left navigation indicator.
         *
         * @param indicator the left indicator (e.g., "&lt; " or "◄ ")
         * @return this builder
         */
        public Builder leftIndicator(String indicator) {
            this.leftIndicator = indicator;
            return this;
        }

        /**
         * Sets the right navigation indicator.
         *
         * @param indicator the right indicator (e.g., " >" or " ►")
         * @return this builder
         */
        public Builder rightIndicator(String indicator) {
            this.rightIndicator = indicator;
            return this;
        }

        /**
         * Sets the foreground color for the selected value.
         *
         * @param color the selected color
         * @return this builder
         */
        public Builder selectedColor(Color color) {
            this.selectedColor = color;
            return this;
        }

        /**
         * Sets the foreground color for indicators.
         *
         * @param color the indicator color
         * @return this builder
         */
        public Builder indicatorColor(Color color) {
            this.indicatorColor = color;
            return this;
        }

        /**
         * Sets the base style for rendering.
         *
         * @param style the base style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style != null ? style : Style.EMPTY;
            return this;
        }

        /**
         * Sets the style resolver for CSS property resolution.
         *
         * @param resolver the style resolver
         * @return this builder
         */
        public Builder styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver;
            return this;
        }

        String resolveLeftIndicator() {
            if (styleResolver != null) {
                return styleResolver.resolve(LEFT_INDICATOR, leftIndicator);
            }
            return leftIndicator != null ? leftIndicator : DEFAULT_LEFT_INDICATOR;
        }

        String resolveRightIndicator() {
            if (styleResolver != null) {
                return styleResolver.resolve(RIGHT_INDICATOR, rightIndicator);
            }
            return rightIndicator != null ? rightIndicator : DEFAULT_RIGHT_INDICATOR;
        }

        Color resolveSelectedColor() {
            if (styleResolver != null) {
                return styleResolver.resolve(SELECTED_COLOR, selectedColor);
            }
            return selectedColor;
        }

        Color resolveIndicatorColor() {
            if (styleResolver != null) {
                return styleResolver.resolve(INDICATOR_COLOR, indicatorColor);
            }
            return indicatorColor;
        }

        /**
         * Builds the Select instance.
         *
         * @return a new Select
         */
        public Select build() {
            return new Select(this);
        }
    }
}

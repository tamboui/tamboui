/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.checkbox;

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
 * A checkbox widget that displays a toggleable checked/unchecked state.
 *
 * <pre>{@code
 * // Create a checkbox with default style [x] / [ ]
 * Checkbox checkbox = Checkbox.builder().build();
 *
 * // Create a checkbox with custom symbols
 * Checkbox checkbox = Checkbox.builder()
 *     .checkedSymbol("[✓]")
 *     .uncheckedSymbol("[ ]")
 *     .checkedColor(Color.GREEN)
 *     .build();
 *
 * // Render with state
 * CheckboxState state = new CheckboxState(true);
 * checkbox.render(area, buffer, state);
 * }</pre>
 *
 * <h2>CSS Properties</h2>
 * <ul>
 *   <li>{@code checkbox-checked-symbol} - Symbol when checked (default: "[x]")</li>
 *   <li>{@code checkbox-unchecked-symbol} - Symbol when unchecked (default: "[ ]")</li>
 *   <li>{@code checkbox-checked-color} - Foreground color when checked</li>
 *   <li>{@code checkbox-unchecked-color} - Foreground color when unchecked</li>
 * </ul>
 *
 * @see CheckboxState
 */
public final class Checkbox implements StatefulWidget<CheckboxState> {

    /** Default symbol for checked state. */
    public static final String DEFAULT_CHECKED_SYMBOL = "[x]";

    /** Default symbol for unchecked state. */
    public static final String DEFAULT_UNCHECKED_SYMBOL = "[ ]";

    /**
     * CSS property for the checked symbol.
     */
    public static final PropertyDefinition<String> CHECKED_SYMBOL =
            PropertyDefinition.builder("checkbox-checked-symbol", Optional::of)
                    .defaultValue(DEFAULT_CHECKED_SYMBOL)
                    .build();

    /**
     * CSS property for the unchecked symbol.
     */
    public static final PropertyDefinition<String> UNCHECKED_SYMBOL =
            PropertyDefinition.builder("checkbox-unchecked-symbol", Optional::of)
                    .defaultValue(DEFAULT_UNCHECKED_SYMBOL)
                    .build();

    /**
     * CSS property for the checked state foreground color.
     */
    public static final PropertyDefinition<Color> CHECKED_COLOR =
            PropertyDefinition.of("checkbox-checked-color", ColorConverter.INSTANCE);

    /**
     * CSS property for the unchecked state foreground color.
     */
    public static final PropertyDefinition<Color> UNCHECKED_COLOR =
            PropertyDefinition.of("checkbox-unchecked-color", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(CHECKED_SYMBOL, UNCHECKED_SYMBOL, CHECKED_COLOR, UNCHECKED_COLOR);
    }

    private final String checkedSymbol;
    private final String uncheckedSymbol;
    private final Style checkedStyle;
    private final Style uncheckedStyle;

    private Checkbox(Builder builder) {
        this.checkedSymbol = builder.resolveCheckedSymbol();
        this.uncheckedSymbol = builder.resolveUncheckedSymbol();

        Color checkedColor = builder.resolveCheckedColor();
        Color uncheckedColor = builder.resolveUncheckedColor();

        this.checkedStyle = checkedColor != null
                ? builder.style.fg(checkedColor)
                : builder.style;
        this.uncheckedStyle = uncheckedColor != null
                ? builder.style.fg(uncheckedColor)
                : builder.style;
    }

    /**
     * Creates a new builder for Checkbox.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer, CheckboxState state) {
        if (area.isEmpty()) {
            return;
        }

        boolean checked = state.isChecked();
        String symbol = checked ? checkedSymbol : uncheckedSymbol;
        Style style = checked ? checkedStyle : uncheckedStyle;

        // Truncate if needed
        if (symbol.length() > area.width()) {
            symbol = symbol.substring(0, area.width());
        }

        buffer.setString(area.x(), area.y(), symbol, style);
    }

    /**
     * Returns the symbol used for checked state.
     *
     * @return the checked symbol
     */
    public String checkedSymbol() {
        return checkedSymbol;
    }

    /**
     * Returns the symbol used for unchecked state.
     *
     * @return the unchecked symbol
     */
    public String uncheckedSymbol() {
        return uncheckedSymbol;
    }

    /**
     * Returns the display width of this checkbox.
     *
     * @return the maximum width of either symbol
     */
    public int width() {
        return Math.max(checkedSymbol.length(), uncheckedSymbol.length());
    }

    /**
     * Builder for {@link Checkbox}.
     */
    public static final class Builder {
        private String checkedSymbol;
        private String uncheckedSymbol;
        private Color checkedColor;
        private Color uncheckedColor;
        private Style style = Style.EMPTY;
        private StylePropertyResolver styleResolver;

        private Builder() {
        }

        /**
         * Sets the symbol to display when checked.
         *
         * @param symbol the checked symbol
         * @return this builder
         */
        public Builder checkedSymbol(String symbol) {
            this.checkedSymbol = symbol;
            return this;
        }

        /**
         * Sets the symbol to display when unchecked.
         *
         * @param symbol the unchecked symbol
         * @return this builder
         */
        public Builder uncheckedSymbol(String symbol) {
            this.uncheckedSymbol = symbol;
            return this;
        }

        /**
         * Sets the foreground color when checked.
         *
         * @param color the checked color
         * @return this builder
         */
        public Builder checkedColor(Color color) {
            this.checkedColor = color;
            return this;
        }

        /**
         * Sets the foreground color when unchecked.
         *
         * @param color the unchecked color
         * @return this builder
         */
        public Builder uncheckedColor(Color color) {
            this.uncheckedColor = color;
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

        String resolveCheckedSymbol() {
            if (styleResolver != null) {
                return styleResolver.resolve(CHECKED_SYMBOL, checkedSymbol);
            }
            return checkedSymbol != null ? checkedSymbol : DEFAULT_CHECKED_SYMBOL;
        }

        String resolveUncheckedSymbol() {
            if (styleResolver != null) {
                return styleResolver.resolve(UNCHECKED_SYMBOL, uncheckedSymbol);
            }
            return uncheckedSymbol != null ? uncheckedSymbol : DEFAULT_UNCHECKED_SYMBOL;
        }

        Color resolveCheckedColor() {
            if (styleResolver != null) {
                return styleResolver.resolve(CHECKED_COLOR, checkedColor);
            }
            return checkedColor;
        }

        Color resolveUncheckedColor() {
            if (styleResolver != null) {
                return styleResolver.resolve(UNCHECKED_COLOR, uncheckedColor);
            }
            return uncheckedColor;
        }

        /**
         * Builds the Checkbox instance.
         *
         * @return a new Checkbox
         */
        public Checkbox build() {
            return new Checkbox(this);
        }
    }
}

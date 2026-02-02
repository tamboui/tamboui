/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toggle;

import java.util.Optional;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.widget.StatefulWidget;

/**
 * A toggle switch widget that displays an on/off state.
 *
 * <p>Supports two display modes:
 * <ul>
 *   <li><b>Single symbol mode</b> (default): Shows one symbol at a time, e.g., "[ON ]" or "[OFF]"</li>
 *   <li><b>Inline choice mode</b>: Shows both options with indicators, e.g., "● Yes / ○ No"</li>
 * </ul>
 *
 * <pre>{@code
 * // Single symbol mode (default)
 * Toggle toggle = Toggle.builder().build();  // [ON ] or [OFF]
 *
 * // Custom single symbols
 * Toggle toggle = Toggle.builder()
 *     .onSymbol("◉ ON ")
 *     .offSymbol("○ OFF")
 *     .onColor(Color.GREEN)
 *     .build();
 *
 * // Inline choice mode: ● Yes / ○ No
 * Toggle toggle = Toggle.builder()
 *     .inlineChoice(true)
 *     .onLabel("Yes")
 *     .offLabel("No")
 *     .selectedIndicator("●")
 *     .unselectedIndicator("○")
 *     .selectedColor(Color.GREEN)
 *     .unselectedColor(Color.DARK_GRAY)
 *     .build();
 * }</pre>
 *
 * <h2>CSS Properties</h2>
 * <ul>
 *   <li>{@code toggle-on-symbol} - Symbol when on (single mode, default: "[ON ]")</li>
 *   <li>{@code toggle-off-symbol} - Symbol when off (single mode, default: "[OFF]")</li>
 *   <li>{@code toggle-on-color} - Foreground color when on</li>
 *   <li>{@code toggle-off-color} - Foreground color when off</li>
 *   <li>{@code toggle-selected-indicator} - Indicator for selected option (inline mode, default: "●")</li>
 *   <li>{@code toggle-unselected-indicator} - Indicator for unselected option (inline mode, default: "○")</li>
 *   <li>{@code toggle-selected-color} - Color for selected option indicator</li>
 *   <li>{@code toggle-unselected-color} - Color for unselected option indicator</li>
 * </ul>
 *
 * @see ToggleState
 */
public final class Toggle implements StatefulWidget<ToggleState> {

    /** Default symbol for on state (single mode). */
    public static final String DEFAULT_ON_SYMBOL = "[ON ]";

    /** Default symbol for off state (single mode). */
    public static final String DEFAULT_OFF_SYMBOL = "[OFF]";

    /** Default indicator for selected option (inline mode). */
    public static final String DEFAULT_SELECTED_INDICATOR = "●";

    /** Default indicator for unselected option (inline mode). */
    public static final String DEFAULT_UNSELECTED_INDICATOR = "○";

    /** Default on label (inline mode). */
    public static final String DEFAULT_ON_LABEL = "Yes";

    /** Default off label (inline mode). */
    public static final String DEFAULT_OFF_LABEL = "No";

    /** Default separator between choices (inline mode). */
    public static final String DEFAULT_SEPARATOR = " / ";

    /**
     * CSS property for the on symbol (single mode).
     */
    public static final PropertyDefinition<String> ON_SYMBOL =
            PropertyDefinition.builder("toggle-on-symbol", Optional::of)
                    .defaultValue(DEFAULT_ON_SYMBOL)
                    .build();

    /**
     * CSS property for the off symbol (single mode).
     */
    public static final PropertyDefinition<String> OFF_SYMBOL =
            PropertyDefinition.builder("toggle-off-symbol", Optional::of)
                    .defaultValue(DEFAULT_OFF_SYMBOL)
                    .build();

    /**
     * CSS property for the on state foreground color.
     */
    public static final PropertyDefinition<Color> ON_COLOR =
            PropertyDefinition.of("toggle-on-color", ColorConverter.INSTANCE);

    /**
     * CSS property for the off state foreground color.
     */
    public static final PropertyDefinition<Color> OFF_COLOR =
            PropertyDefinition.of("toggle-off-color", ColorConverter.INSTANCE);

    /**
     * CSS property for the selected indicator (inline choice mode).
     */
    public static final PropertyDefinition<String> SELECTED_INDICATOR =
            PropertyDefinition.builder("toggle-selected-indicator", Optional::of)
                    .defaultValue(DEFAULT_SELECTED_INDICATOR)
                    .build();

    /**
     * CSS property for the unselected indicator (inline choice mode).
     */
    public static final PropertyDefinition<String> UNSELECTED_INDICATOR =
            PropertyDefinition.builder("toggle-unselected-indicator", Optional::of)
                    .defaultValue(DEFAULT_UNSELECTED_INDICATOR)
                    .build();

    /**
     * CSS property for the selected color (inline choice mode).
     */
    public static final PropertyDefinition<Color> SELECTED_COLOR =
            PropertyDefinition.of("toggle-selected-color", ColorConverter.INSTANCE);

    /**
     * CSS property for the unselected color (inline choice mode).
     */
    public static final PropertyDefinition<Color> UNSELECTED_COLOR =
            PropertyDefinition.of("toggle-unselected-color", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(
                ON_SYMBOL, OFF_SYMBOL, ON_COLOR, OFF_COLOR,
                SELECTED_INDICATOR, UNSELECTED_INDICATOR, SELECTED_COLOR, UNSELECTED_COLOR
        );
    }

    // Single mode fields
    private final String onSymbol;
    private final String offSymbol;
    private final Style onStyle;
    private final Style offStyle;

    // Inline choice mode fields
    private final boolean inlineChoice;
    private final String onLabel;
    private final String offLabel;
    private final String selectedIndicator;
    private final String unselectedIndicator;
    private final String separator;
    private final Style selectedStyle;
    private final Style unselectedStyle;

    private Toggle(Builder builder) {
        this.inlineChoice = builder.inlineChoice;

        // Single mode
        this.onSymbol = builder.resolveOnSymbol();
        this.offSymbol = builder.resolveOffSymbol();

        Color onColor = builder.resolveOnColor();
        Color offColor = builder.resolveOffColor();

        this.onStyle = onColor != null
                ? builder.style.fg(onColor)
                : builder.style.fg(Color.GREEN);
        this.offStyle = offColor != null
                ? builder.style.fg(offColor)
                : builder.style;

        // Inline choice mode
        this.onLabel = builder.onLabel != null ? builder.onLabel : DEFAULT_ON_LABEL;
        this.offLabel = builder.offLabel != null ? builder.offLabel : DEFAULT_OFF_LABEL;
        this.selectedIndicator = builder.resolveSelectedIndicator();
        this.unselectedIndicator = builder.resolveUnselectedIndicator();
        this.separator = builder.separator != null ? builder.separator : DEFAULT_SEPARATOR;

        Color selectedColor = builder.resolveSelectedColor();
        Color unselectedColor = builder.resolveUnselectedColor();

        this.selectedStyle = selectedColor != null
                ? builder.style.fg(selectedColor)
                : builder.style.fg(Color.GREEN);
        this.unselectedStyle = unselectedColor != null
                ? builder.style.fg(unselectedColor)
                : builder.style.fg(Color.DARK_GRAY);
    }

    /**
     * Creates a new builder for Toggle.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer, ToggleState state) {
        if (area.isEmpty()) {
            return;
        }

        if (inlineChoice) {
            renderInlineChoice(area, buffer, state);
        } else {
            renderSingleSymbol(area, buffer, state);
        }
    }

    private void renderSingleSymbol(Rect area, Buffer buffer, ToggleState state) {
        boolean on = state.isOn();
        String symbol = on ? onSymbol : offSymbol;
        Style style = on ? onStyle : offStyle;

        if (symbol.length() > area.width()) {
            symbol = symbol.substring(0, area.width());
        }

        buffer.setString(area.x(), area.y(), symbol, style);
    }

    private void renderInlineChoice(Rect area, Buffer buffer, ToggleState state) {
        boolean on = state.isOn();
        int x = area.x();

        // Render "on" option: indicator + space + label
        String onIndicator = on ? selectedIndicator : unselectedIndicator;
        Style onIndicatorStyle = on ? selectedStyle : unselectedStyle;

        if (x < area.x() + area.width()) {
            buffer.setString(x, area.y(), onIndicator, onIndicatorStyle);
            x += onIndicator.length();
        }

        if (x < area.x() + area.width()) {
            buffer.setString(x, area.y(), " ", onIndicatorStyle);
            x += 1;
        }

        if (x < area.x() + area.width()) {
            String label = truncate(onLabel, area.x() + area.width() - x);
            buffer.setString(x, area.y(), label, onIndicatorStyle);
            x += label.length();
        }

        // Render separator
        if (x < area.x() + area.width()) {
            String sep = truncate(separator, area.x() + area.width() - x);
            buffer.setString(x, area.y(), sep, unselectedStyle);
            x += sep.length();
        }

        // Render "off" option: indicator + space + label
        String offIndicator = on ? unselectedIndicator : selectedIndicator;
        Style offIndicatorStyle = on ? unselectedStyle : selectedStyle;

        if (x < area.x() + area.width()) {
            buffer.setString(x, area.y(), offIndicator, offIndicatorStyle);
            x += offIndicator.length();
        }

        if (x < area.x() + area.width()) {
            buffer.setString(x, area.y(), " ", offIndicatorStyle);
            x += 1;
        }

        if (x < area.x() + area.width()) {
            String label = truncate(offLabel, area.x() + area.width() - x);
            buffer.setString(x, area.y(), label, offIndicatorStyle);
        }
    }

    private static String truncate(String text, int maxWidth) {
        return text.length() > maxWidth ? text.substring(0, maxWidth) : text;
    }

    /**
     * Returns the symbol used for on state (single mode).
     *
     * @return the on symbol
     */
    public String onSymbol() {
        return onSymbol;
    }

    /**
     * Returns the symbol used for off state (single mode).
     *
     * @return the off symbol
     */
    public String offSymbol() {
        return offSymbol;
    }

    /**
     * Returns whether inline choice mode is enabled.
     *
     * @return true if inline choice mode
     */
    public boolean isInlineChoice() {
        return inlineChoice;
    }

    /**
     * Returns the display width of this toggle.
     *
     * @return the width needed to render
     */
    public int width() {
        if (inlineChoice) {
            // "● Yes / ○ No" format
            return selectedIndicator.length() + 1 + onLabel.length()
                    + separator.length()
                    + unselectedIndicator.length() + 1 + offLabel.length();
        }
        return Math.max(onSymbol.length(), offSymbol.length());
    }

    /**
     * Builder for {@link Toggle}.
     */
    public static final class Builder {
        // Single mode
        private String onSymbol;
        private String offSymbol;
        private Color onColor;
        private Color offColor;

        // Inline choice mode
        private boolean inlineChoice;
        private String onLabel;
        private String offLabel;
        private String selectedIndicator;
        private String unselectedIndicator;
        private String separator;
        private Color selectedColor;
        private Color unselectedColor;

        private Style style = Style.EMPTY;
        private StylePropertyResolver styleResolver;

        private Builder() {
        }

        // === Single mode methods ===

        /**
         * Sets the symbol to display when on (single mode).
         *
         * @param symbol the on symbol
         * @return this builder
         */
        public Builder onSymbol(String symbol) {
            this.onSymbol = symbol;
            return this;
        }

        /**
         * Sets the symbol to display when off (single mode).
         *
         * @param symbol the off symbol
         * @return this builder
         */
        public Builder offSymbol(String symbol) {
            this.offSymbol = symbol;
            return this;
        }

        /**
         * Sets the foreground color when on.
         *
         * @param color the on color
         * @return this builder
         */
        public Builder onColor(Color color) {
            this.onColor = color;
            return this;
        }

        /**
         * Sets the foreground color when off.
         *
         * @param color the off color
         * @return this builder
         */
        public Builder offColor(Color color) {
            this.offColor = color;
            return this;
        }

        // === Inline choice mode methods ===

        /**
         * Enables inline choice mode, showing both options like "● Yes / ○ No".
         *
         * @param enabled true to enable inline choice mode
         * @return this builder
         */
        public Builder inlineChoice(boolean enabled) {
            this.inlineChoice = enabled;
            return this;
        }

        /**
         * Sets the label for the "on" option (inline choice mode).
         *
         * @param label the on label
         * @return this builder
         */
        public Builder onLabel(String label) {
            this.onLabel = label;
            return this;
        }

        /**
         * Sets the label for the "off" option (inline choice mode).
         *
         * @param label the off label
         * @return this builder
         */
        public Builder offLabel(String label) {
            this.offLabel = label;
            return this;
        }

        /**
         * Sets the indicator for the selected option (inline choice mode).
         *
         * @param indicator the selected indicator (e.g., "●")
         * @return this builder
         */
        public Builder selectedIndicator(String indicator) {
            this.selectedIndicator = indicator;
            return this;
        }

        /**
         * Sets the indicator for the unselected option (inline choice mode).
         *
         * @param indicator the unselected indicator (e.g., "○")
         * @return this builder
         */
        public Builder unselectedIndicator(String indicator) {
            this.unselectedIndicator = indicator;
            return this;
        }

        /**
         * Sets the separator between choices (inline choice mode).
         *
         * @param separator the separator (default: " / ")
         * @return this builder
         */
        public Builder separator(String separator) {
            this.separator = separator;
            return this;
        }

        /**
         * Sets the color for the selected option indicator (inline choice mode).
         *
         * @param color the selected color
         * @return this builder
         */
        public Builder selectedColor(Color color) {
            this.selectedColor = color;
            return this;
        }

        /**
         * Sets the color for the unselected option indicator (inline choice mode).
         *
         * @param color the unselected color
         * @return this builder
         */
        public Builder unselectedColor(Color color) {
            this.unselectedColor = color;
            return this;
        }

        // === Common methods ===

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

        // === Resolution methods ===

        String resolveOnSymbol() {
            if (styleResolver != null) {
                return styleResolver.resolve(ON_SYMBOL, onSymbol);
            }
            return onSymbol != null ? onSymbol : DEFAULT_ON_SYMBOL;
        }

        String resolveOffSymbol() {
            if (styleResolver != null) {
                return styleResolver.resolve(OFF_SYMBOL, offSymbol);
            }
            return offSymbol != null ? offSymbol : DEFAULT_OFF_SYMBOL;
        }

        Color resolveOnColor() {
            if (styleResolver != null) {
                return styleResolver.resolve(ON_COLOR, onColor);
            }
            return onColor;
        }

        Color resolveOffColor() {
            if (styleResolver != null) {
                return styleResolver.resolve(OFF_COLOR, offColor);
            }
            return offColor;
        }

        String resolveSelectedIndicator() {
            if (styleResolver != null) {
                return styleResolver.resolve(SELECTED_INDICATOR, selectedIndicator);
            }
            return selectedIndicator != null ? selectedIndicator : DEFAULT_SELECTED_INDICATOR;
        }

        String resolveUnselectedIndicator() {
            if (styleResolver != null) {
                return styleResolver.resolve(UNSELECTED_INDICATOR, unselectedIndicator);
            }
            return unselectedIndicator != null ? unselectedIndicator : DEFAULT_UNSELECTED_INDICATOR;
        }

        Color resolveSelectedColor() {
            if (styleResolver != null) {
                return styleResolver.resolve(SELECTED_COLOR, selectedColor);
            }
            return selectedColor;
        }

        Color resolveUnselectedColor() {
            if (styleResolver != null) {
                return styleResolver.resolve(UNSELECTED_COLOR, unselectedColor);
            }
            return unselectedColor;
        }

        /**
         * Builds the Toggle instance.
         *
         * @return a new Toggle
         */
        public Toggle build() {
            return new Toggle(this);
        }
    }
}

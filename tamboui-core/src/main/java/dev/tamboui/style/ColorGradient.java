/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A multi-stop color gradient that maps a {@code [0.0, 1.0]} ratio to an interpolated color.
 * <p>
 * Color stops are evenly distributed across the gradient range. For example, a gradient created
 * with green, yellow, and red will map:
 * <ul>
 *   <li>{@code 0.0} to green</li>
 *   <li>{@code 0.5} to yellow</li>
 *   <li>{@code 1.0} to red</li>
 * </ul>
 * Ratios outside the {@code [0.0, 1.0]} range (or NaN) are automatically clamped.
 *
 * <pre>{@code
 * ColorGradient gradient = ColorGradient.of(Color.GREEN, Color.YELLOW, Color.RED);
 * Color mid = gradient.at(0.5); // YELLOW
 * Color threeQuarters = gradient.at(0.75); // midpoint between YELLOW and RED
 * }</pre>
 */
public final class ColorGradient {

    private final List<Color> stops;

    private ColorGradient(List<Color> stops) {
        this.stops = stops;
    }

    /**
     * Creates a color gradient with the given color stops.
     *
     * @param stops the color stops (at least one required)
     * @return a new ColorGradient
     */
    public static ColorGradient of(Color... stops) {
        Objects.requireNonNull(stops, "stops");
        if (stops.length == 0) {
            throw new IllegalArgumentException("At least one color stop is required");
        }
        for (Color stop : stops) {
            Objects.requireNonNull(stop, "stop color cannot be null");
        }
        return new ColorGradient(Collections.unmodifiableList(new ArrayList<>(Arrays.asList(stops))));
    }

    /**
     * Creates a color gradient with the given color stops.
     *
     * @param stops the color stops (at least one required)
     * @return a new ColorGradient
     */
    public static ColorGradient of(List<Color> stops) {
        Objects.requireNonNull(stops, "stops");
        if (stops.isEmpty()) {
            throw new IllegalArgumentException("At least one color stop is required");
        }
        for (Color stop : stops) {
            Objects.requireNonNull(stop, "stop color cannot be null");
        }
        return new ColorGradient(Collections.unmodifiableList(new ArrayList<>(stops)));
    }

    /**
     * Returns an unmodifiable list of the color stops in this gradient.
     *
     * @return the list of color stops
     */
    public List<Color> stops() {
        return stops;
    }

    /**
     * Evaluates the gradient at the given ratio (clamped to {@code [0.0, 1.0]}).
     *
     * @param ratio the ratio between 0.0 and 1.0
     * @return the interpolated color
     */
    public Color at(double ratio) {
        if (Double.isNaN(ratio) || ratio <= 0.0) {
            return stops.get(0);
        }
        if (ratio >= 1.0) {
            return stops.get(stops.size() - 1);
        }
        int numStops = stops.size();
        if (numStops == 1) {
            return stops.get(0);
        }
        double scaled = ratio * (numStops - 1);
        int index = (int) scaled;
        double t = scaled - index;
        if (index >= numStops - 1) {
            return stops.get(numStops - 1);
        }
        return ColorMath.lerpRgb(stops.get(index), stops.get(index + 1), t);
    }

    /**
     * Evaluates the gradient at the given float ratio.
     *
     * @param ratio the ratio between 0.0f and 1.0f
     * @return the interpolated color
     */
    public Color at(float ratio) {
        return at((double) ratio);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ColorGradient)) {
            return false;
        }
        ColorGradient that = (ColorGradient) o;
        return stops.equals(that.stops);
    }

    @Override
    public int hashCode() {
        return stops.hashCode();
    }

    @Override
    public String toString() {
        return "ColorGradient" + stops;
    }
}

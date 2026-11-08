/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.style.Color;

/**
 * Defines the color space used for color interpolation in effects.
 * <p>
 * Color spaces determine how colors are interpolated between start and end values.
 * Different color spaces produce different visual results, particularly when transitioning
 * between colors with different hues or saturation levels.
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * Color space selection balances performance and perceptual quality. RGB is fastest
 * but can produce perceptually non-uniform transitions (e.g., muddy gray midpoints).
 * HSL and HSV provide better perceptual uniformity at the cost of additional computation.
 * <p>
 * <b>Color Space Characteristics:</b>
 * <ul>
 *   <li><b>RGB:</b> Linear interpolation in RGB space. Fastest but can produce
 *       perceptually incorrect transitions, especially between saturated colors.</li>
 *   <li><b>HSL:</b> Interpolation in Hue-Saturation-Lightness space. Default choice
 *       providing good balance of performance and perceptual quality. Produces smooth
 *       hue transitions and maintains saturation better than RGB.</li>
 *   <li><b>HSV:</b> Interpolation in Hue-Saturation-Value space. Similar to HSL but
 *       with a different perceptual model. Value represents brightness differently
 *       than Lightness in HSL.</li>
 * </ul>
 * <p>
 * <b>When to Use Each:</b>
 * <ul>
 *   <li><b>RGB:</b> Use for performance-critical scenarios or when interpolating
 *       between similar colors where perceptual differences are minimal</li>
 *   <li><b>HSL:</b> Use as default for most fade effects, especially when transitioning
 *       between colors with different hues</li>
 *   <li><b>HSV:</b> Use when you need a different perceptual model than HSL, or
 *       when transitioning between colors where Value is more important than Lightness</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Use HSL for smooth hue transitions (default)
 * Effect fade = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
 *     .withColorSpace(TFxColorSpace.HSL);
 * 
 * // Use RGB for performance
 * Effect fastFade = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.Linear)
 *     .withColorSpace(TFxColorSpace.RGB);
 * }</pre>
 * <p>
 * This enum also provides utility methods for creating colors from HSL and HSV values,
 * which can be useful for creating color palettes or converting between color representations.
 */
public enum TFxColorSpace {
    /**
     * Linear RGB interpolation (fastest but not perceptually uniform)
     */
    RGB,
    
    /**
     * HSL interpolation (default - balance of performance and perceptual quality)
     */
    HSL,
    
    /**
     * HSV interpolation (similar to HSL but different perceptual model)
     */
    HSV;
    
    /**
     * Interpolates between two colors using the specified color space.
     * 
     * @param from The starting color
     * @param to The target color
     * @param alpha The interpolation factor (0.0 to 1.0)
     * @return The interpolated color
     */
    public Color lerp(Color from, Color to, float alpha) {
        switch (this) {
            case RGB:
                return dev.tamboui.style.ColorMath.lerpRgb(from, to, alpha);
            case HSL:
                return dev.tamboui.style.ColorMath.lerpHsl(from, to, alpha);
            case HSV:
                return dev.tamboui.style.ColorMath.lerpHsv(from, to, alpha);
            default:
                return dev.tamboui.style.ColorMath.lerpRgb(from, to, alpha);
        }
    }
    
    /**
     * Converts a color to RGB components [r, g, b].
     */
    int[] toRgbComponents(Color color) {
        return dev.tamboui.style.ColorMath.toRgbComponents(color);
    }
    
    // Color conversion utilities
    
    float[] rgbToHsl(int r, int g, int b) {
        return dev.tamboui.style.ColorMath.rgbToHsl(r, g, b);
    }
    
    int[] hslToRgb(float h, float s, float l) {
        return dev.tamboui.style.ColorMath.hslToRgb(h, s, l);
    }
    
    float[] rgbToHsv(int r, int g, int b) {
        return dev.tamboui.style.ColorMath.rgbToHsv(r, g, b);
    }
    
    int[] hsvToRgb(float h, float s, float v) {
        return dev.tamboui.style.ColorMath.hsvToRgb(h, s, v);
    }
    
    // Public utility methods for creating colors
    
    /**
     * Creates a color from HSL (Hue, Saturation, Lightness) values.
     * 
     * @param h Hue in degrees (0-360)
     * @param s Saturation percentage (0-100)
     * @param l Lightness percentage (0-100)
     * @return An RGB color
     */
    public static Color fromHsl(float h, float s, float l) {
        return dev.tamboui.style.ColorMath.fromHsl(h, s, l);
    }
    
    /**
     * Creates a color from HSV (Hue, Saturation, Value) values.
     * 
     * @param h Hue in degrees (0-360)
     * @param s Saturation percentage (0-100)
     * @param v Value/brightness percentage (0-100)
     * @return An RGB color
     */
    public static Color fromHsv(float h, float s, float v) {
        return dev.tamboui.style.ColorMath.fromHsv(h, s, v);
    }
    
    /**
     * Converts a color to HSL components.
     * 
     * @param color The color to convert
     * @return An array [hue, saturation, lightness] where:
     *         - hue is in degrees (0-360)
     *         - saturation and lightness are percentages (0-100)
     */
    public static float[] toHsl(Color color) {
        return dev.tamboui.style.ColorMath.toHsl(color);
    }
    
    /**
     * Converts a color to HSV components.
     * 
     * @param color The color to convert
     * @return An array [hue, saturation, value] where:
     *         - hue is in degrees (0-360)
     *         - saturation and value are percentages (0-100)
     */
    public static float[] toHsv(Color color) {
        return dev.tamboui.style.ColorMath.toHsv(color);
    }
}

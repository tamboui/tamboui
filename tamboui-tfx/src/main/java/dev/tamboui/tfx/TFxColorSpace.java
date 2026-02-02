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
        alpha = java.lang.Math.max(0.0f, java.lang.Math.min(1.0f, alpha));
        
        if (alpha == 0.0f) {
            return from;
        } else if (alpha == 1.0f) {
            return to;
        }
        
        switch (this) {
            case RGB:
                return lerpRgb(from, to, alpha);
            case HSL:
                return lerpHsl(from, to, alpha);
            case HSV:
                return lerpHsv(from, to, alpha);
            default:
                return lerpRgb(from, to, alpha);
        }
    }
    
    private Color lerpRgb(Color from, Color to, float alpha) {
        // Convert both colors to RGB components
        int[] fromRgb = toRgbComponents(from);
        int[] toRgb = toRgbComponents(to);
        
        // Linear interpolation in RGB space
        int r = lerpComponent(fromRgb[0], toRgb[0], alpha);
        int g = lerpComponent(fromRgb[1], toRgb[1], alpha);
        int b = lerpComponent(fromRgb[2], toRgb[2], alpha);
        
        return Color.rgb(r, g, b);
    }
    
    private Color lerpHsl(Color from, Color to, float alpha) {
        float[] fromHsl = toHslInternal(from);
        float[] toHsl = toHslInternal(to);
        
        // Interpolate hue taking shortest path around color wheel
        float hDiff = toHsl[0] - fromHsl[0];
        if (hDiff > 180.0f) {
            hDiff -= 360.0f;
        } else if (hDiff < -180.0f) {
            hDiff += 360.0f;
        }
        
        float h = fromHsl[0] + hDiff * alpha;
        if (h < 0.0f) {
            h += 360.0f;
        } else if (h >= 360.0f) {
            h -= 360.0f;
        }
        
        float s = fromHsl[1] + (toHsl[1] - fromHsl[1]) * alpha;
        float l = fromHsl[2] + (toHsl[2] - fromHsl[2]) * alpha;
        
        return fromHslInternal(h, s, l);
    }
    
    private Color lerpHsv(Color from, Color to, float alpha) {
        float[] fromHsv = toHsvInternal(from);
        float[] toHsv = toHsvInternal(to);
        
        // Interpolate hue taking shortest path around color wheel
        float hDiff = toHsv[0] - fromHsv[0];
        if (hDiff > 180.0f) {
            hDiff -= 360.0f;
        } else if (hDiff < -180.0f) {
            hDiff += 360.0f;
        }
        
        float h = fromHsv[0] + hDiff * alpha;
        if (h < 0.0f) {
            h += 360.0f;
        } else if (h >= 360.0f) {
            h -= 360.0f;
        }
        
        float s = fromHsv[1] + (toHsv[1] - fromHsv[1]) * alpha;
        float v = fromHsv[2] + (toHsv[2] - fromHsv[2]) * alpha;
        
        return fromHsvInternal(h, s, v);
    }
    
    private int lerpComponent(int from, int to, float alpha) {
        return java.lang.Math.round(from + (to - from) * alpha);
    }
    
    /**
     * Converts a color to RGB components [r, g, b].
     */
    int[] toRgbComponents(Color color) {
        if (color instanceof Color.Reset) {
            // Reset defaults to black for effects
            return new int[]{0, 0, 0};
        }
        Color.Rgb rgb = color.toRgb();
        return new int[]{rgb.r(), rgb.g(), rgb.b()};
    }
    
    /**
     * Internal helper: Converts a color to HSL components [h, s, l] where:
     * h is in degrees (0-360)
     * s and l are percentages (0-100)
     */
    private float[] toHslInternal(Color color) {
        int[] rgb = toRgbComponents(color);
        return rgbToHsl(rgb[0], rgb[1], rgb[2]);
    }
    
    /**
     * Internal helper: Converts a color to HSV components [h, s, v] where:
     * h is in degrees (0-360)
     * s and v are percentages (0-100)
     */
    private float[] toHsvInternal(Color color) {
        int[] rgb = toRgbComponents(color);
        return rgbToHsv(rgb[0], rgb[1], rgb[2]);
    }
    
    /**
     * Internal helper: Creates a color from HSL values.
     */
    private Color fromHslInternal(float h, float s, float l) {
        int[] rgb = hslToRgb(h, s, l);
        return Color.rgb(rgb[0], rgb[1], rgb[2]);
    }
    
    /**
     * Internal helper: Creates a color from HSV values.
     */
    private Color fromHsvInternal(float h, float s, float v) {
        int[] rgb = hsvToRgb(h, s, v);
        return Color.rgb(rgb[0], rgb[1], rgb[2]);
    }
    
    // Color conversion utilities
    
    float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;
        
        float max = java.lang.Math.max(rf, java.lang.Math.max(gf, bf));
        float min = java.lang.Math.min(rf, java.lang.Math.min(gf, bf));
        float delta = max - min;
        
        // Lightness
        float l = (max + min) / 2.0f;
        
        // Saturation
        float s;
        if (delta == 0.0f) {
            s = 0.0f;
        } else {
            s = delta / (1.0f - java.lang.Math.abs(2.0f * l - 1.0f));
        }
        
        // Hue
        float h;
        if (delta == 0.0f) {
            h = 0.0f;
        } else if (max == rf) {
            h = 60.0f * (((gf - bf) / delta) % 6.0f);
        } else if (max == gf) {
            h = 60.0f * ((bf - rf) / delta + 2.0f);
        } else {
            h = 60.0f * ((rf - gf) / delta + 4.0f);
        }
        
        if (h < 0.0f) {
            h += 360.0f;
        }
        
        return new float[]{h, s * 100.0f, l * 100.0f};
    }
    
    int[] hslToRgb(float h, float s, float l) {
        h = h % 360.0f;
        if (h < 0.0f) {
            h += 360.0f;
        }
        s = s / 100.0f;
        l = l / 100.0f;
        
        float c = (1.0f - java.lang.Math.abs(2.0f * l - 1.0f)) * s;
        float x = c * (1.0f - java.lang.Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = l - c / 2.0f;
        
        float r, g, b;
        if (h < 60.0f) {
            r = c;
            g = x;
            b = 0.0f;
        } else if (h < 120.0f) {
            r = x;
            g = c;
            b = 0.0f;
        } else if (h < 180.0f) {
            r = 0.0f;
            g = c;
            b = x;
        } else if (h < 240.0f) {
            r = 0.0f;
            g = x;
            b = c;
        } else if (h < 300.0f) {
            r = x;
            g = 0.0f;
            b = c;
        } else {
            r = c;
            g = 0.0f;
            b = x;
        }
        
        return new int[]{
            java.lang.Math.round((r + m) * 255.0f),
            java.lang.Math.round((g + m) * 255.0f),
            java.lang.Math.round((b + m) * 255.0f)
        };
    }
    
    float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;
        
        float max = java.lang.Math.max(rf, java.lang.Math.max(gf, bf));
        float min = java.lang.Math.min(rf, java.lang.Math.min(gf, bf));
        float delta = max - min;
        
        // Hue
        float h;
        if (delta == 0.0f) {
            h = 0.0f;
        } else if (max == rf) {
            h = 60.0f * (((gf - bf) / delta) % 6.0f);
        } else if (max == gf) {
            h = 60.0f * ((bf - rf) / delta + 2.0f);
        } else {
            h = 60.0f * ((rf - gf) / delta + 4.0f);
        }
        
        if (h < 0.0f) {
            h += 360.0f;
        }
        
        // Saturation
        float s = max == 0.0f ? 0.0f : delta / max;
        
        // Value
        float v = max;
        
        return new float[]{h, s * 100.0f, v * 100.0f};
    }
    
    int[] hsvToRgb(float h, float s, float v) {
        h = h % 360.0f;
        if (h < 0.0f) {
            h += 360.0f;
        }
        s = s / 100.0f;
        v = v / 100.0f;
        
        float c = v * s;
        float x = c * (1.0f - java.lang.Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - c;
        
        float r, g, b;
        if (h < 60.0f) {
            r = c;
            g = x;
            b = 0.0f;
        } else if (h < 120.0f) {
            r = x;
            g = c;
            b = 0.0f;
        } else if (h < 180.0f) {
            r = 0.0f;
            g = c;
            b = x;
        } else if (h < 240.0f) {
            r = 0.0f;
            g = x;
            b = c;
        } else if (h < 300.0f) {
            r = x;
            g = 0.0f;
            b = c;
        } else {
            r = c;
            g = 0.0f;
            b = x;
        }
        
        return new int[]{
            java.lang.Math.round((r + m) * 255.0f),
            java.lang.Math.round((g + m) * 255.0f),
            java.lang.Math.round((b + m) * 255.0f)
        };
    }
    
    /**
     * Converts ANSI color to approximate RGB values.
     */
    private int[] ansiToRgb(dev.tamboui.style.AnsiColor ansiColor) {
        // Standard ANSI color palette RGB values
        switch (ansiColor) {
            case BLACK:
                return new int[]{0, 0, 0};
            case RED:
                return new int[]{205, 0, 0};
            case GREEN:
                return new int[]{0, 205, 0};
            case YELLOW:
                return new int[]{205, 205, 0};
            case BLUE:
                return new int[]{0, 0, 238};
            case MAGENTA:
                return new int[]{205, 0, 205};
            case CYAN:
                return new int[]{0, 205, 205};
            case WHITE:
                return new int[]{229, 229, 229};
            case BRIGHT_BLACK:
                return new int[]{127, 127, 127};
            case BRIGHT_RED:
                return new int[]{255, 0, 0};
            case BRIGHT_GREEN:
                return new int[]{0, 255, 0};
            case BRIGHT_YELLOW:
                return new int[]{255, 255, 0};
            case BRIGHT_BLUE:
                return new int[]{92, 92, 255};
            case BRIGHT_MAGENTA:
                return new int[]{255, 0, 255};
            case BRIGHT_CYAN:
                return new int[]{0, 255, 255};
            case BRIGHT_WHITE:
                return new int[]{255, 255, 255};
            default:
                return new int[]{0, 0, 0};
        }
    }
    
    /**
     * Converts indexed color (0-255) to approximate RGB values.
     */
    private int[] indexedToRgb(int index) {
        if (index < 16) {
            // First 16 are standard ANSI colors
            return ansiToRgb(dev.tamboui.style.AnsiColor.values()[index]);
        } else if (index < 232) {
            // 216-color cube (6x6x6)
            int cubeIndex = index - 16;
            int r = cubeIndex / 36;
            int g = (cubeIndex / 6) % 6;
            int b = cubeIndex % 6;
            return new int[]{
                r == 0 ? 0 : 55 + r * 40,
                g == 0 ? 0 : 55 + g * 40,
                b == 0 ? 0 : 55 + b * 40
            };
        } else {
            // Grayscale ramp (232-255)
            int gray = 8 + (index - 232) * 10;
            return new int[]{gray, gray, gray};
        }
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
        TFxColorSpace instance = HSL; // Use any instance to access private methods
        int[] rgb = instance.hslToRgb(h, s, l);
        return Color.rgb(rgb[0], rgb[1], rgb[2]);
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
        TFxColorSpace instance = HSV; // Use any instance to access private methods
        int[] rgb = instance.hsvToRgb(h, s, v);
        return Color.rgb(rgb[0], rgb[1], rgb[2]);
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
        TFxColorSpace instance = HSL; // Use any instance to access private methods
        int[] rgb = instance.toRgbComponents(color);
        return instance.rgbToHsl(rgb[0], rgb[1], rgb[2]);
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
        TFxColorSpace instance = HSL; // Use any instance to access private methods
        int[] rgb = instance.toRgbComponents(color);
        return instance.rgbToHsv(rgb[0], rgb[1], rgb[2]);
    }
}

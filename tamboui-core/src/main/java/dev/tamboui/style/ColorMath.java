/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Objects;

/**
 * Utility methods for color space conversions and interpolation.
 * <p>
 * Supports linear interpolation in RGB, HSL (Hue, Saturation, Lightness), and HSV
 * (Hue, Saturation, Value) color spaces, as well as conversions between color representations.
 */
public final class ColorMath {

    private ColorMath() {
        // Utility class
    }

    /**
     * Converts a color to RGB components {@code [r, g, b]} in the range 0-255.
     *
     * @param color the color
     * @return an array with three elements: red, green, blue
     */
    public static int[] toRgbComponents(Color color) {
        Objects.requireNonNull(color, "color");
        if (color instanceof Color.Reset) {
            return new int[] {0, 0, 0};
        }
        Color.Rgb rgb = color.toRgb();
        return new int[] {rgb.r(), rgb.g(), rgb.b()};
    }

    /**
     * Linearly interpolates between two integer color components.
     *
     * @param from the starting component value
     * @param to the target component value
     * @param alpha the interpolation factor (0.0 to 1.0)
     * @return the interpolated component value rounded to nearest integer
     */
    private static int lerpComponent(int from, int to, double alpha) {
        return (int) Math.round(from + (to - from) * alpha);
    }

    /**
     * Linearly interpolates between two colors in RGB space.
     *
     * @param from the starting color
     * @param to the target color
     * @param alpha the interpolation factor (0.0 to 1.0, clamped)
     * @return the interpolated color
     */
    public static Color lerpRgb(Color from, Color to, double alpha) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        alpha = Math.max(0.0, Math.min(1.0, alpha));

        if (alpha == 0.0) {
            return from.toRgb();
        } else if (alpha == 1.0) {
            return to.toRgb();
        }

        int[] fromRgb = toRgbComponents(from);
        int[] toRgb = toRgbComponents(to);

        int r = lerpComponent(fromRgb[0], toRgb[0], alpha);
        int g = lerpComponent(fromRgb[1], toRgb[1], alpha);
        int b = lerpComponent(fromRgb[2], toRgb[2], alpha);

        return Color.rgb(r, g, b);
    }

    /**
     * Linearly interpolates between two colors in RGB space using a float factor.
     *
     * @param from the starting color
     * @param to the target color
     * @param alpha the interpolation factor (0.0f to 1.0f, clamped)
     * @return the interpolated color
     */
    public static Color lerpRgb(Color from, Color to, float alpha) {
        return lerpRgb(from, to, (double) alpha);
    }

    /**
     * Default interpolation between two colors (RGB space).
     *
     * @param from the starting color
     * @param to the target color
     * @param alpha the interpolation factor (0.0 to 1.0, clamped)
     * @return the interpolated color
     */
    public static Color lerp(Color from, Color to, double alpha) {
        return lerpRgb(from, to, alpha);
    }

    /**
     * Interpolates between two colors in HSL space, taking the shortest path around the hue circle.
     *
     * @param from the starting color
     * @param to the target color
     * @param alpha the interpolation factor (0.0f to 1.0f, clamped)
     * @return the interpolated color
     */
    public static Color lerpHsl(Color from, Color to, float alpha) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));

        if (alpha == 0.0f) {
            return from.toRgb();
        } else if (alpha == 1.0f) {
            return to.toRgb();
        }

        float[] fromHsl = toHsl(from);
        float[] toHsl = toHsl(to);

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

        return fromHsl(h, s, l);
    }

    /**
     * Interpolates between two colors in HSV space, taking the shortest path around the hue circle.
     *
     * @param from the starting color
     * @param to the target color
     * @param alpha the interpolation factor (0.0f to 1.0f, clamped)
     * @return the interpolated color
     */
    public static Color lerpHsv(Color from, Color to, float alpha) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));

        if (alpha == 0.0f) {
            return from.toRgb();
        } else if (alpha == 1.0f) {
            return to.toRgb();
        }

        float[] fromHsv = toHsv(from);
        float[] toHsv = toHsv(to);

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

        return fromHsv(h, s, v);
    }

    /**
     * Converts RGB color components (0-255) to HSL components.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return an array {@code [hue, saturation, lightness]} where:
     *         <ul>
     *           <li>hue is in degrees (0-360)</li>
     *           <li>saturation is percentage (0-100)</li>
     *           <li>lightness is percentage (0-100)</li>
     *         </ul>
     */
    public static float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;

        // Lightness
        float l = (max + min) / 2.0f;

        // Saturation
        float s;
        if (delta == 0.0f) {
            s = 0.0f;
        } else {
            s = delta / (1.0f - Math.abs(2.0f * l - 1.0f));
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

        return new float[] {h, s * 100.0f, l * 100.0f};
    }

    /**
     * Converts HSL components to RGB components (0-255).
     *
     * @param h hue in degrees (0-360)
     * @param s saturation percentage (0-100)
     * @param l lightness percentage (0-100)
     * @return an array {@code [r, g, b]} in the range 0-255
     */
    public static int[] hslToRgb(float h, float s, float l) {
        h = h % 360.0f;
        if (h < 0.0f) {
            h += 360.0f;
        }
        s = s / 100.0f;
        l = l / 100.0f;

        float c = (1.0f - Math.abs(2.0f * l - 1.0f)) * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = l - c / 2.0f;

        float r;
        float g;
        float b;
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

        return new int[] {
            Math.round((r + m) * 255.0f),
            Math.round((g + m) * 255.0f),
            Math.round((b + m) * 255.0f)
        };
    }

    /**
     * Converts RGB color components (0-255) to HSV components.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return an array {@code [hue, saturation, value]} where:
     *         <ul>
     *           <li>hue is in degrees (0-360)</li>
     *           <li>saturation is percentage (0-100)</li>
     *           <li>value is percentage (0-100)</li>
     *         </ul>
     */
    public static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255.0f;
        float gf = g / 255.0f;
        float bf = b / 255.0f;

        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
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

        return new float[] {h, s * 100.0f, v * 100.0f};
    }

    /**
     * Converts HSV components to RGB components (0-255).
     *
     * @param h hue in degrees (0-360)
     * @param s saturation percentage (0-100)
     * @param v value/brightness percentage (0-100)
     * @return an array {@code [r, g, b]} in the range 0-255
     */
    public static int[] hsvToRgb(float h, float s, float v) {
        h = h % 360.0f;
        if (h < 0.0f) {
            h += 360.0f;
        }
        s = s / 100.0f;
        v = v / 100.0f;

        float c = v * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - c;

        float r;
        float g;
        float b;
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

        return new int[] {
            Math.round((r + m) * 255.0f),
            Math.round((g + m) * 255.0f),
            Math.round((b + m) * 255.0f)
        };
    }

    /**
     * Creates a color from HSL (Hue, Saturation, Lightness) values.
     *
     * @param h hue in degrees (0-360)
     * @param s saturation percentage (0-100)
     * @param l lightness percentage (0-100)
     * @return an RGB color
     */
    public static Color fromHsl(float h, float s, float l) {
        int[] rgb = hslToRgb(h, s, l);
        return Color.rgb(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Creates a color from HSV (Hue, Saturation, Value) values.
     *
     * @param h hue in degrees (0-360)
     * @param s saturation percentage (0-100)
     * @param v value/brightness percentage (0-100)
     * @return an RGB color
     */
    public static Color fromHsv(float h, float s, float v) {
        int[] rgb = hsvToRgb(h, s, v);
        return Color.rgb(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Converts a color to HSL components.
     *
     * @param color the color to convert
     * @return an array {@code [hue, saturation, lightness]} where:
     *         <ul>
     *           <li>hue is in degrees (0-360)</li>
     *           <li>saturation and lightness are percentages (0-100)</li>
     *         </ul>
     */
    public static float[] toHsl(Color color) {
        int[] rgb = toRgbComponents(color);
        return rgbToHsl(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Converts a color to HSV components.
     *
     * @param color the color to convert
     * @return an array {@code [hue, saturation, value]} where:
     *         <ul>
     *           <li>hue is in degrees (0-360)</li>
     *           <li>saturation and value are percentages (0-100)</li>
     *         </ul>
     */
    public static float[] toHsv(Color color) {
        int[] rgb = toRgbComponents(color);
        return rgbToHsv(rgb[0], rgb[1], rgb[2]);
    }
}

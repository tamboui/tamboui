/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.color;

import dev.tamboui.style.Color;

/**
 * Color manipulation operations and color space conversions.
 * <p>
 * Provides both low-level color space conversions (RGB/HSL/HSV) and
 * high-level semantic operations (lighten, darken, blend, contrast).
 */
public final class ColorManipulation {

    private ColorManipulation() {
        // Utility class
    }

    // ===== Low-Level Color Space Conversions =====

    /**
     * Converts RGB to HSL color space.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return array [hue (0-360), saturation (0-100), lightness (0-100)]
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

        return new float[]{h, s * 100.0f, l * 100.0f};
    }

    /**
     * Converts HSL to RGB color space.
     *
     * @param h hue in degrees (0-360)
     * @param s saturation percentage (0-100)
     * @param l lightness percentage (0-100)
     * @return array [red (0-255), green (0-255), blue (0-255)]
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

        float r, g, b;
        if (h < 60.0f) {
            r = c; g = x; b = 0.0f;
        } else if (h < 120.0f) {
            r = x; g = c; b = 0.0f;
        } else if (h < 180.0f) {
            r = 0.0f; g = c; b = x;
        } else if (h < 240.0f) {
            r = 0.0f; g = x; b = c;
        } else if (h < 300.0f) {
            r = x; g = 0.0f; b = c;
        } else {
            r = c; g = 0.0f; b = x;
        }

        return new int[]{
            Math.round((r + m) * 255.0f),
            Math.round((g + m) * 255.0f),
            Math.round((b + m) * 255.0f)
        };
    }

    /**
     * Converts RGB to HSV color space.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return array [hue (0-360), saturation (0-100), value (0-100)]
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

        return new float[]{h, s * 100.0f, v * 100.0f};
    }

    /**
     * Converts HSV to RGB color space.
     *
     * @param h hue in degrees (0-360)
     * @param s saturation percentage (0-100)
     * @param v value/brightness percentage (0-100)
     * @return array [red (0-255), green (0-255), blue (0-255)]
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

        float r, g, b;
        if (h < 60.0f) {
            r = c; g = x; b = 0.0f;
        } else if (h < 120.0f) {
            r = x; g = c; b = 0.0f;
        } else if (h < 180.0f) {
            r = 0.0f; g = c; b = x;
        } else if (h < 240.0f) {
            r = 0.0f; g = x; b = c;
        } else if (h < 300.0f) {
            r = x; g = 0.0f; b = c;
        } else {
            r = c; g = 0.0f; b = x;
        }

        return new int[]{
            Math.round((r + m) * 255.0f),
            Math.round((g + m) * 255.0f),
            Math.round((b + m) * 255.0f)
        };
    }

    /**
     * Linear interpolation between two colors in RGB space.
     *
     * @param from starting color
     * @param to target color
     * @param alpha interpolation factor (0.0 to 1.0)
     * @return interpolated color
     */
    public static Color lerp(Color from, Color to, float alpha) {
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));

        if (alpha == 0.0f) return from;
        if (alpha == 1.0f) return to;

        Color.Rgb fromRgb = from.toRgb();
        Color.Rgb toRgb = to.toRgb();

        int r = Math.round(fromRgb.r() + (toRgb.r() - fromRgb.r()) * alpha);
        int g = Math.round(fromRgb.g() + (toRgb.g() - fromRgb.g()) * alpha);
        int b = Math.round(fromRgb.b() + (toRgb.b() - fromRgb.b()) * alpha);

        return Color.rgb(r, g, b);
    }

    // ===== High-Level Color Manipulation =====

    /**
     * Lightens a color by increasing its lightness in HSL space.
     *
     * @param color the color to lighten
     * @param amount amount to increase lightness (0.0 to 1.0)
     * @return lightened color
     * @throws IllegalArgumentException if color is null or amount is out of range
     */
    public static Color lighten(Color color, float amount) {
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null");
        }
        if (amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be between 0.0 and 1.0, got: " + amount);
        }

        Color.Rgb rgb = color.toRgb();
        float[] hsl = rgbToHsl(rgb.r(), rgb.g(), rgb.b());

        float newL = Math.min(100.0f, hsl[2] + amount * 100.0f);
        int[] newRgb = hslToRgb(hsl[0], hsl[1], newL);

        return Color.rgb(newRgb[0], newRgb[1], newRgb[2]);
    }

    /**
     * Darkens a color by decreasing its lightness in HSL space.
     *
     * @param color the color to darken
     * @param amount amount to decrease lightness (0.0 to 1.0)
     * @return darkened color
     * @throws IllegalArgumentException if color is null or amount is out of range
     */
    public static Color darken(Color color, float amount) {
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null");
        }
        if (amount < 0.0f || amount > 1.0f) {
            throw new IllegalArgumentException("amount must be between 0.0 and 1.0, got: " + amount);
        }

        Color.Rgb rgb = color.toRgb();
        float[] hsl = rgbToHsl(rgb.r(), rgb.g(), rgb.b());

        float newL = Math.max(0.0f, hsl[2] - amount * 100.0f);
        int[] newRgb = hslToRgb(hsl[0], hsl[1], newL);

        return Color.rgb(newRgb[0], newRgb[1], newRgb[2]);
    }

    /**
     * Blends two colors using linear RGB interpolation.
     *
     * @param base the base color
     * @param other the color to blend with
     * @param ratio blend ratio (0.0 = all base, 1.0 = all other)
     * @return blended color
     * @throws IllegalArgumentException if base or other is null
     */
    public static Color blend(Color base, Color other, float ratio) {
        if (base == null) {
            throw new IllegalArgumentException("base cannot be null");
        }
        if (other == null) {
            throw new IllegalArgumentException("other cannot be null");
        }

        ratio = Math.max(0.0f, Math.min(1.0f, ratio));

        Color.Rgb baseRgb = base.toRgb();
        Color.Rgb otherRgb = other.toRgb();

        int r = Math.round(baseRgb.r() + (otherRgb.r() - baseRgb.r()) * ratio);
        int g = Math.round(baseRgb.g() + (otherRgb.g() - baseRgb.g()) * ratio);
        int b = Math.round(baseRgb.b() + (otherRgb.b() - baseRgb.b()) * ratio);

        return Color.rgb(r, g, b);
    }

    /**
     * Returns a contrasting text color (black or white) based on background luminance.
     * Uses WCAG relative luminance calculation.
     *
     * @param background the background color
     * @return white for dark backgrounds, black for light backgrounds
     * @throws IllegalArgumentException if background is null
     */
    public static Color getContrastText(Color background) {
        if (background == null) {
            throw new IllegalArgumentException("background cannot be null");
        }

        Color.Rgb rgb = background.toRgb();

        // Calculate relative luminance (WCAG formula)
        double r = rgb.r() / 255.0;
        double g = rgb.g() / 255.0;
        double b = rgb.b() / 255.0;

        // Apply gamma correction
        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;

        // Return white for dark backgrounds, black for light backgrounds
        return luminance > 0.5 ? Color.rgb(0, 0, 0) : Color.rgb(255, 255, 255);
    }

    /**
     * Inverts a color by flipping RGB components.
     *
     * @param color the color to invert
     * @return inverted color
     * @throws IllegalArgumentException if color is null
     */
    public static Color inverse(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null");
        }

        Color.Rgb rgb = color.toRgb();
        return Color.rgb(255 - rgb.r(), 255 - rgb.g(), 255 - rgb.b());
    }

    /**
     * Applies alpha/transparency to a color.
     * Note: Current Color interface doesn't support alpha, so this is a no-op.
     * Reserved for future RGBA support.
     *
     * @param color the base color
     * @param alpha alpha value (0.0 to 1.0)
     * @return the color (unchanged until RGBA support added)
     * @throws IllegalArgumentException if color is null
     */
    public static Color withAlpha(Color color, float alpha) {
        if (color == null) {
            throw new IllegalArgumentException("color cannot be null");
        }

        // TODO: Implement when Color supports RGBA
        return color;
    }
}

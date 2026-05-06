/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.color;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;

import static org.junit.jupiter.api.Assertions.*;

class ColorManipulationTest {

    @Test
    void testLighten() {
        Color base = Color.rgb(100, 100, 100);
        Color lighter = ColorManipulation.lighten(base, 0.2f);

        Color.Rgb rgb = lighter.toRgb();
        assertTrue(rgb.r() > 100);
        assertTrue(rgb.g() > 100);
        assertTrue(rgb.b() > 100);
    }

    @Test
    void testDarken() {
        Color base = Color.rgb(200, 200, 200);
        Color darker = ColorManipulation.darken(base, 0.2f);

        Color.Rgb rgb = darker.toRgb();
        assertTrue(rgb.r() < 200);
        assertTrue(rgb.g() < 200);
        assertTrue(rgb.b() < 200);
    }

    @Test
    void testBlend() {
        Color red = Color.rgb(255, 0, 0);
        Color blue = Color.rgb(0, 0, 255);
        Color blended = ColorManipulation.blend(red, blue, 0.5f);

        Color.Rgb rgb = blended.toRgb();
        assertEquals(127, rgb.r(), 2);
        assertEquals(0, rgb.g(), 2);
        assertEquals(127, rgb.b(), 2);
    }

    @Test
    void testGetContrastText_Dark() {
        Color dark = Color.rgb(0, 0, 0);
        Color contrast = ColorManipulation.getContrastText(dark);

        // Should return white or very light color
        Color.Rgb rgb = contrast.toRgb();
        assertTrue(rgb.r() > 200);
    }

    @Test
    void testGetContrastText_Light() {
        Color light = Color.rgb(255, 255, 255);
        Color contrast = ColorManipulation.getContrastText(light);

        // Should return black or very dark color
        Color.Rgb rgb = contrast.toRgb();
        assertTrue(rgb.r() < 50);
    }

    @Test
    void testInverse() {
        Color color = Color.rgb(100, 150, 200);
        Color inverted = ColorManipulation.inverse(color);
        Color.Rgb rgb = inverted.toRgb();
        assertEquals(155, rgb.r());
        assertEquals(105, rgb.g());
        assertEquals(55, rgb.b());
    }

    @Test
    void testWithAlpha() {
        Color color = Color.rgb(100, 100, 100);
        Color result = ColorManipulation.withAlpha(color, 0.5f);
        // Currently a no-op, should return same color
        assertEquals(color, result);
    }

    // Low-level color space conversion tests

    @Test
    void testRgbToHsl_Red() {
        float[] hsl = ColorManipulation.rgbToHsl(255, 0, 0);
        assertEquals(0.0f, hsl[0], 0.1f);      // Hue
        assertEquals(100.0f, hsl[1], 0.1f);    // Saturation
        assertEquals(50.0f, hsl[2], 0.1f);     // Lightness
    }

    @Test
    void testRgbToHsl_Gray() {
        float[] hsl = ColorManipulation.rgbToHsl(128, 128, 128);
        assertEquals(0.0f, hsl[1], 0.1f);      // No saturation
        assertEquals(50.2f, hsl[2], 0.5f);     // Mid lightness
    }

    @Test
    void testHslToRgb_Red() {
        int[] rgb = ColorManipulation.hslToRgb(0.0f, 100.0f, 50.0f);
        assertEquals(255, rgb[0]);
        assertEquals(0, rgb[1]);
        assertEquals(0, rgb[2]);
    }
}

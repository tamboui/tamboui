/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorConvenienceTest {

    @Test
    void testLightenConvenience() {
        Color base = Color.rgb(100, 100, 100);
        Color lighter = base.lighten(0.2f);

        Color.Rgb rgb = lighter.toRgb();
        assertTrue(rgb.r() > 100);
    }

    @Test
    void testDarkenConvenience() {
        Color base = Color.rgb(200, 200, 200);
        Color darker = base.darken(0.2f);

        Color.Rgb rgb = darker.toRgb();
        assertTrue(rgb.r() < 200);
    }

    @Test
    void testBlendConvenience() {
        Color red = Color.rgb(255, 0, 0);
        Color blue = Color.rgb(0, 0, 255);
        Color blended = red.blend(blue, 0.5f);

        assertNotNull(blended);
    }

    @Test
    void testGetContrastTextConvenience() {
        Color dark = Color.rgb(0, 0, 0);
        Color contrast = dark.getContrastText();

        Color.Rgb rgb = contrast.toRgb();
        assertTrue(rgb.r() > 200);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ColorMathTest {

    @Test
    @DisplayName("toRgbComponents converts colors accurately")
    void toRgbComponents() {
        Color.Rgb rgb = new Color.Rgb(10, 20, 30);
        assertThat(ColorMath.toRgbComponents(rgb)).containsExactly(10, 20, 30);

        Color reset = Color.RESET;
        assertThat(ColorMath.toRgbComponents(reset)).containsExactly(0, 0, 0);

        assertThatNullPointerException()
            .isThrownBy(() -> ColorMath.toRgbComponents(null));
    }

    @Test
    @DisplayName("lerpRgb linearly interpolates in RGB color space")
    void lerpRgb() {
        Color black = Color.rgb(0, 0, 0);
        Color white = Color.rgb(200, 100, 50);

        assertThat(ColorMath.lerpRgb(black, white, 0.0).toRgb())
            .isEqualTo(Color.rgb(0, 0, 0));
        assertThat(ColorMath.lerpRgb(black, white, 1.0).toRgb())
            .isEqualTo(Color.rgb(200, 100, 50));
        assertThat(ColorMath.lerpRgb(black, white, 0.5).toRgb())
            .isEqualTo(Color.rgb(100, 50, 25));

        // Clamping
        assertThat(ColorMath.lerpRgb(black, white, -0.5).toRgb())
            .isEqualTo(Color.rgb(0, 0, 0));
        assertThat(ColorMath.lerpRgb(black, white, 1.5).toRgb())
            .isEqualTo(Color.rgb(200, 100, 50));

        // Float overload and lerp alias
        assertThat(ColorMath.lerpRgb(black, white, 0.5f).toRgb())
            .isEqualTo(Color.rgb(100, 50, 25));
        assertThat(ColorMath.lerp(black, white, 0.5).toRgb())
            .isEqualTo(Color.rgb(100, 50, 25));
    }

    @Test
    @DisplayName("lerpHsl interpolates taking shortest path around hue circle")
    void lerpHsl() {
        Color red = Color.rgb(255, 0, 0); // Hue = 0
        Color green = Color.rgb(0, 255, 0); // Hue = 120

        assertThat(ColorMath.lerpHsl(red, green, 0.0f).toRgb())
            .isEqualTo(red.toRgb());
        assertThat(ColorMath.lerpHsl(red, green, 1.0f).toRgb())
            .isEqualTo(green.toRgb());

        // Midpoint should be Yellow (Hue ~60)
        Color mid = ColorMath.lerpHsl(red, green, 0.5f);
        float[] hsl = ColorMath.toHsl(mid);
        assertThat(hsl[0]).isBetween(58.0f, 62.0f);
    }

    @Test
    @DisplayName("lerpHsv interpolates taking shortest path around hue circle")
    void lerpHsv() {
        Color red = Color.rgb(255, 0, 0);
        Color blue = Color.rgb(0, 0, 255); // Hue = 240

        assertThat(ColorMath.lerpHsv(red, blue, 0.0f).toRgb())
            .isEqualTo(red.toRgb());
        assertThat(ColorMath.lerpHsv(red, blue, 1.0f).toRgb())
            .isEqualTo(blue.toRgb());

        // Red (0) to Blue (240): shortest path goes through Magenta (300/-60), not Green (120)
        Color mid = ColorMath.lerpHsv(red, blue, 0.5f);
        float[] hsv = ColorMath.toHsv(mid);
        assertThat(hsv[0]).isBetween(295.0f, 305.0f);
    }

    @Test
    @DisplayName("RGB, HSL, and HSV conversion utilities round-trip correctly")
    void conversionsRoundTrip() {
        Color original = Color.rgb(120, 80, 200);

        float[] hsl = ColorMath.toHsl(original);
        Color fromHsl = ColorMath.fromHsl(hsl[0], hsl[1], hsl[2]);
        Color.Rgb rgbHsl = fromHsl.toRgb();
        assertThat(rgbHsl.r()).isBetween(119, 121);
        assertThat(rgbHsl.g()).isBetween(79, 81);
        assertThat(rgbHsl.b()).isBetween(199, 201);

        float[] hsv = ColorMath.toHsv(original);
        Color fromHsv = ColorMath.fromHsv(hsv[0], hsv[1], hsv[2]);
        Color.Rgb rgbHsv = fromHsv.toRgb();
        assertThat(rgbHsv.r()).isBetween(119, 121);
        assertThat(rgbHsv.g()).isBetween(79, 81);
        assertThat(rgbHsv.b()).isBetween(199, 201);
    }
}

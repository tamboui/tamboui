/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class ColorTest {

    @Test
    @DisplayName("Color constants are Ansi colors")
    void colorConstants() {
        assertThat(Color.RED).isInstanceOf(Color.Ansi.class);
        assertThat(Color.GREEN).isInstanceOf(Color.Ansi.class);
        assertThat(Color.BLUE).isInstanceOf(Color.Ansi.class);
        assertThat(Color.BLACK).isInstanceOf(Color.Ansi.class);
        assertThat(Color.WHITE).isInstanceOf(Color.Ansi.class);
    }

    @Test
    @DisplayName("Color.Ansi wraps AnsiColor")
    void ansiColor() {
        Color.Ansi color = new Color.Ansi(AnsiColor.CYAN);
        assertThat(color.color()).isEqualTo(AnsiColor.CYAN);
    }

    @Test
    @DisplayName("Color.Rgb holds RGB values")
    void rgbColor() {
        Color.Rgb color = new Color.Rgb(255, 128, 64);
        assertThat(color.r()).isEqualTo(255);
        assertThat(color.g()).isEqualTo(128);
        assertThat(color.b()).isEqualTo(64);
    }

    @Test
    @DisplayName("Color.Indexed holds palette index")
    void indexedColor() {
        Color.Indexed color = new Color.Indexed(42);
        assertThat(color.index()).isEqualTo(42);
    }

    @Test
    @DisplayName("Color.Reset is a singleton-like type")
    void resetColor() {
        Color.Reset reset1 = new Color.Reset();
        Color.Reset reset2 = new Color.Reset();
        assertThat(reset1).isEqualTo(reset2);
    }

    @Test
    @DisplayName("Color.rgb factory method")
    void rgbFactory() {
        Color color = Color.rgb(100, 150, 200);
        assertThat(color).isInstanceOf(Color.Rgb.class);
        assertThat(((Color.Rgb) color).r()).isEqualTo(100);
    }

    @Test
    @DisplayName("Color.indexed factory method")
    void indexedFactory() {
        Color color = Color.indexed(128);
        assertThat(color).isInstanceOf(Color.Indexed.class);
        assertThat(((Color.Indexed) color).index()).isEqualTo(128);
    }
}


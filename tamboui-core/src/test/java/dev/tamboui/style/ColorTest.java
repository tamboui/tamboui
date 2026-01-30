/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ColorTest {

    @Test
    @DisplayName("Color constants are Named colors with Ansi defaults")
    void colorConstants() {
        assertThat(Color.RED).isInstanceOf(Color.Named.class);
        assertThat(Color.GREEN).isInstanceOf(Color.Named.class);
        assertThat(Color.BLUE).isInstanceOf(Color.Named.class);
        assertThat(Color.BLACK).isInstanceOf(Color.Named.class);
        assertThat(Color.WHITE).isInstanceOf(Color.Named.class);

        // Verify named colors have correct CSS class names
        assertThat(((Color.Named) Color.RED).name()).isEqualTo("red");
        assertThat(((Color.Named) Color.GREEN).name()).isEqualTo("green");
        assertThat(((Color.Named) Color.BLUE).name()).isEqualTo("blue");

        // Verify named colors have Ansi defaults
        assertThat(((Color.Named) Color.RED).defaultValue()).isInstanceOf(Color.Ansi.class);
        assertThat(((Color.Named) Color.GREEN).defaultValue()).isInstanceOf(Color.Ansi.class);
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

    @Test
    @DisplayName("Named color converts to RGB via its default value")
    void namedColorToRgb() {
        Color.Named red = (Color.Named) Color.RED;
        Color.Rgb rgb = red.toRgb();
        // RED's default is Ansi RED which converts to (170, 0, 0)
        assertThat(rgb.r()).isEqualTo(170);
        assertThat(rgb.g()).isEqualTo(0);
        assertThat(rgb.b()).isEqualTo(0);
    }

    // toAnsiForeground tests

    @Test
    @DisplayName("Reset.toAnsiForeground returns 39")
    void resetToAnsiForeground() {
        assertThat(Color.RESET.toAnsiForeground()).isEqualTo("39");
    }

    @Test
    @DisplayName("Ansi.toAnsiForeground returns foreground SGR code")
    void ansiToAnsiForeground() {
        Color color = new Color.Ansi(AnsiColor.RED);
        assertThat(color.toAnsiForeground()).isEqualTo("31");

        Color bright = new Color.Ansi(AnsiColor.BRIGHT_CYAN);
        assertThat(bright.toAnsiForeground()).isEqualTo("96");
    }

    @Test
    @DisplayName("Indexed.toAnsiForeground returns 38;5;index")
    void indexedToAnsiForeground() {
        Color color = new Color.Indexed(42);
        assertThat(color.toAnsiForeground()).isEqualTo("38;5;42");
    }

    @Test
    @DisplayName("Rgb.toAnsiForeground returns 38;2;r;g;b")
    void rgbToAnsiForeground() {
        Color color = new Color.Rgb(100, 150, 200);
        assertThat(color.toAnsiForeground()).isEqualTo("38;2;100;150;200");
    }

    @Test
    @DisplayName("Named.toAnsiForeground delegates to default value")
    void namedToAnsiForeground() {
        // Color.RED is Named with Ansi(RED) default → fgCode 31
        assertThat(Color.RED.toAnsiForeground()).isEqualTo("31");
    }

    // toAnsiBackground tests

    @Test
    @DisplayName("Reset.toAnsiBackground returns 49")
    void resetToAnsiBackground() {
        assertThat(Color.RESET.toAnsiBackground()).isEqualTo("49");
    }

    @Test
    @DisplayName("Ansi.toAnsiBackground returns background SGR code")
    void ansiToAnsiBackground() {
        Color color = new Color.Ansi(AnsiColor.RED);
        assertThat(color.toAnsiBackground()).isEqualTo("41");

        Color bright = new Color.Ansi(AnsiColor.BRIGHT_CYAN);
        assertThat(bright.toAnsiBackground()).isEqualTo("106");
    }

    @Test
    @DisplayName("Indexed.toAnsiBackground returns 48;5;index")
    void indexedToAnsiBackground() {
        Color color = new Color.Indexed(42);
        assertThat(color.toAnsiBackground()).isEqualTo("48;5;42");
    }

    @Test
    @DisplayName("Rgb.toAnsiBackground returns 48;2;r;g;b")
    void rgbToAnsiBackground() {
        Color color = new Color.Rgb(100, 150, 200);
        assertThat(color.toAnsiBackground()).isEqualTo("48;2;100;150;200");
    }

    @Test
    @DisplayName("Named.toAnsiBackground delegates to default value")
    void namedToAnsiBackground() {
        assertThat(Color.RED.toAnsiBackground()).isEqualTo("41");
    }

    // toAnsiUnderline tests

    @Test
    @DisplayName("Reset.toAnsiUnderline returns empty string")
    void resetToAnsiUnderline() {
        assertThat(Color.RESET.toAnsiUnderline()).isEmpty();
    }

    @Test
    @DisplayName("Ansi.toAnsiUnderline returns empty string")
    void ansiToAnsiUnderline() {
        Color color = new Color.Ansi(AnsiColor.RED);
        assertThat(color.toAnsiUnderline()).isEmpty();
    }

    @Test
    @DisplayName("Indexed.toAnsiUnderline returns 58;5;index")
    void indexedToAnsiUnderline() {
        Color color = new Color.Indexed(42);
        assertThat(color.toAnsiUnderline()).isEqualTo("58;5;42");
    }

    @Test
    @DisplayName("Rgb.toAnsiUnderline returns 58;2;r;g;b")
    void rgbToAnsiUnderline() {
        Color color = new Color.Rgb(100, 150, 200);
        assertThat(color.toAnsiUnderline()).isEqualTo("58;2;100;150;200");
    }

    @Test
    @DisplayName("Named.toAnsiUnderline delegates to default value")
    void namedToAnsiUnderline() {
        // Ansi colors don't support underline color, so Named wrapping Ansi returns
        // empty
        assertThat(Color.RED.toAnsiUnderline()).isEmpty();

        // Named wrapping Rgb should delegate
        Color named = new Color.Named("custom", new Color.Rgb(10, 20, 30));
        assertThat(named.toAnsiUnderline()).isEqualTo("58;2;10;20;30");
    }

    // toRgb polymorphic tests

    @Test
    @DisplayName("Rgb.toRgb returns this")
    void rgbToRgbReturnsSelf() {
        Color.Rgb rgb = new Color.Rgb(10, 20, 30);
        assertThat(rgb.toRgb()).isSameAs(rgb);
    }

    @Test
    @DisplayName("Reset.toRgb returns white")
    void resetToRgb() {
        Color.Rgb rgb = Color.RESET.toRgb();
        assertThat(rgb.r()).isEqualTo(255);
        assertThat(rgb.g()).isEqualTo(255);
        assertThat(rgb.b()).isEqualTo(255);
    }

    @Test
    @DisplayName("Indexed.toRgb converts via palette")
    void indexedToRgb() {
        // Index 0 = ANSI BLACK = (0, 0, 0)
        Color.Rgb rgb = new Color.Indexed(0).toRgb();
        assertThat(rgb.r()).isEqualTo(0);
        assertThat(rgb.g()).isEqualTo(0);
        assertThat(rgb.b()).isEqualTo(0);
    }
}

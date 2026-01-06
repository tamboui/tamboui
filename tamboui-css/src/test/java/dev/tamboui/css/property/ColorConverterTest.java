/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the core ColorConverter.
 */
class ColorConverterTest {

    private final ColorConverter converter = ColorConverter.INSTANCE;

    @Test
    void convertsNamedColors() {
        assertThat(converter.convert("red")).hasValue(Color.RED);
        assertThat(converter.convert("blue")).hasValue(Color.BLUE);
        assertThat(converter.convert("green")).hasValue(Color.GREEN);
        assertThat(converter.convert("black")).hasValue(Color.BLACK);
        assertThat(converter.convert("white")).hasValue(Color.WHITE);
    }

    @Test
    void convertsNamedColorsCaseInsensitive() {
        assertThat(converter.convert("RED")).hasValue(Color.RED);
        assertThat(converter.convert("Red")).hasValue(Color.RED);
    }

    @Test
    void convertsHexColor6Digits() {
        Optional<Color> color = converter.convert("#ff0000");

        assertThat(color).isPresent();
        assertThat(color.get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb rgb = (Color.Rgb) color.get();
        assertThat(rgb.r()).isEqualTo(255);
        assertThat(rgb.g()).isEqualTo(0);
        assertThat(rgb.b()).isEqualTo(0);
    }

    @Test
    void convertsHexColor3Digits() {
        Optional<Color> color = converter.convert("#f00");

        assertThat(color).isPresent();
        assertThat(color.get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb rgb = (Color.Rgb) color.get();
        assertThat(rgb.r()).isEqualTo(255);
        assertThat(rgb.g()).isEqualTo(0);
        assertThat(rgb.b()).isEqualTo(0);
    }

    @Test
    void convertsRgbFunction() {
        Optional<Color> color = converter.convert("rgb(100, 150, 200)");

        assertThat(color).isPresent();
        assertThat(color.get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb rgb = (Color.Rgb) color.get();
        assertThat(rgb.r()).isEqualTo(100);
        assertThat(rgb.g()).isEqualTo(150);
        assertThat(rgb.b()).isEqualTo(200);
    }

    @Test
    void convertsIndexedColor() {
        Optional<Color> color = converter.convert("indexed(42)");

        assertThat(color).isPresent();
        assertThat(color.get()).isInstanceOf(Color.Indexed.class);
        assertThat(((Color.Indexed) color.get()).index()).isEqualTo(42);
    }

    @Test
    void variableResolutionHappensAtCssLevel() {
        // The core ColorConverter doesn't handle variables - that happens in the CSS layer
        // When a variable is pre-resolved, the value is passed directly
        assertThat(converter.convert("blue")).hasValue(Color.BLUE);
    }

    @Test
    void returnsEmptyForInvalidColor() {
        assertThat(converter.convert("invalid")).isEmpty();
        assertThat(converter.convert("")).isEmpty();
        assertThat(converter.convert(null)).isEmpty();
    }

    @Test
    void convertsBrightColors() {
        assertThat(converter.convert("light-red")).hasValue(Color.LIGHT_RED);
        assertThat(converter.convert("bright-blue")).hasValue(Color.LIGHT_BLUE);
    }
}

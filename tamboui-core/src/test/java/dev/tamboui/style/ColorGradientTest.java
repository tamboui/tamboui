/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ColorGradientTest {

    @Test
    @DisplayName("Validation rejects null, empty, or null-containing stops")
    void validation() {
        assertThatNullPointerException()
            .isThrownBy(() -> ColorGradient.of((Color[]) null));
        assertThatIllegalArgumentException()
            .isThrownBy(ColorGradient::of);
        assertThatNullPointerException()
            .isThrownBy(() -> ColorGradient.of(Color.RED, null));

        assertThatNullPointerException()
            .isThrownBy(() -> ColorGradient.of(Collections.singletonList(null)));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ColorGradient.of(Collections.emptyList()));
    }

    @Test
    @DisplayName("Single stop gradient returns that stop for all ratios")
    void singleStop() {
        ColorGradient gradient = ColorGradient.of(Color.CYAN);

        assertThat(gradient.stops()).containsExactly(Color.CYAN);
        assertThat(gradient.at(0.0)).isEqualTo(Color.CYAN);
        assertThat(gradient.at(0.5)).isEqualTo(Color.CYAN);
        assertThat(gradient.at(1.0)).isEqualTo(Color.CYAN);
        assertThat(gradient.at(-1.0)).isEqualTo(Color.CYAN);
        assertThat(gradient.at(2.0)).isEqualTo(Color.CYAN);
        assertThat(gradient.at(Double.NaN)).isEqualTo(Color.CYAN);
    }

    @Test
    @DisplayName("Two-stop gradient interpolates between endpoints")
    void twoStops() {
        Color black = Color.rgb(0, 0, 0);
        Color white = Color.rgb(200, 100, 50);
        ColorGradient gradient = ColorGradient.of(black, white);

        assertThat(gradient.at(0.0).toRgb()).isEqualTo(Color.rgb(0, 0, 0));
        assertThat(gradient.at(1.0).toRgb()).isEqualTo(Color.rgb(200, 100, 50));
        assertThat(gradient.at(0.5).toRgb()).isEqualTo(Color.rgb(100, 50, 25));

        // Clamping
        assertThat(gradient.at(-0.2).toRgb()).isEqualTo(Color.rgb(0, 0, 0));
        assertThat(gradient.at(1.5).toRgb()).isEqualTo(Color.rgb(200, 100, 50));
        assertThat(gradient.at(Double.NaN).toRgb()).isEqualTo(Color.rgb(0, 0, 0));

        // Float overload
        assertThat(gradient.at(0.5f).toRgb()).isEqualTo(Color.rgb(100, 50, 25));
    }

    @Test
    @DisplayName("Three-stop gradient distributes stops evenly across [0.0, 1.0]")
    void threeStops() {
        Color green = Color.rgb(0, 200, 0);
        Color yellow = Color.rgb(200, 200, 0);
        Color red = Color.rgb(200, 0, 0);
        ColorGradient gradient = ColorGradient.of(green, yellow, red);

        assertThat(gradient.at(0.0).toRgb()).isEqualTo(green.toRgb());
        assertThat(gradient.at(0.5).toRgb()).isEqualTo(yellow.toRgb());
        assertThat(gradient.at(1.0).toRgb()).isEqualTo(red.toRgb());

        // 0.25 is midpoint between green and yellow
        assertThat(gradient.at(0.25).toRgb()).isEqualTo(Color.rgb(100, 200, 0));
        // 0.75 is midpoint between yellow and red
        assertThat(gradient.at(0.75).toRgb()).isEqualTo(Color.rgb(200, 100, 0));
    }

    @Test
    @DisplayName("equals, hashCode, and toString")
    void equalityAndString() {
        ColorGradient g1 = ColorGradient.of(Color.RED, Color.BLUE);
        ColorGradient g2 = ColorGradient.of(Arrays.asList(Color.RED, Color.BLUE));
        ColorGradient g3 = ColorGradient.of(Color.RED, Color.GREEN);

        assertThat(g1).isEqualTo(g2);
        assertThat(g1.hashCode()).isEqualTo(g2.hashCode());
        assertThat(g1).isNotEqualTo(g3);
        assertThat(g1.toString()).contains("ColorGradient");
    }
}

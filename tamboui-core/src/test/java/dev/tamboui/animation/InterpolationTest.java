/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.animation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class InterpolationTest {

    @ParameterizedTest
    @EnumSource(Interpolation.class)
    @DisplayName("flipped() is self-inverting (f.flipped().flipped() == f)")
    void flippedIsSelfInverting(Interpolation interpolation) {
        assertThat(interpolation.flipped().flipped()).isEqualTo(interpolation);
    }

    @ParameterizedTest
    @EnumSource(Interpolation.class)
    @DisplayName("alpha(double) is consistent with alpha(float)")
    void alphaDoubleMatchesFloat(Interpolation interpolation) {
        for (double t = 0.0; t <= 1.0; t += 0.1) {
            double doubleResult = interpolation.alpha(t);
            float floatResult = interpolation.alpha((float) t);
            assertThat(doubleResult).isCloseTo(floatResult, within(1e-5));
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = Interpolation.class,
            names = {"Reverse"},
            mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("standard easing functions start at 0 and end at 1")
    void standardEasingEndpoints(Interpolation interpolation) {
        assertThat(interpolation.alpha(0.0f)).isCloseTo(0.0f, within(1e-4f));
        assertThat(interpolation.alpha(1.0f)).isCloseTo(1.0f, within(1e-4f));
    }

    @Test
    @DisplayName("Reverse easing starts at 1 and ends at 0")
    void reverseEasingEndpoints() {
        assertThat(Interpolation.Reverse.alpha(0.0f)).isEqualTo(1.0f);
        assertThat(Interpolation.Reverse.alpha(1.0f)).isEqualTo(0.0f);
        assertThat(Interpolation.Reverse.alpha(0.3f)).isCloseTo(0.7f, within(1e-5f));
    }

    @Test
    @DisplayName("Linear interpolation is identity")
    void linearIsIdentity() {
        assertThat(Interpolation.Linear.alpha(0.0f)).isEqualTo(0.0f);
        assertThat(Interpolation.Linear.alpha(0.42f)).isEqualTo(0.42f);
        assertThat(Interpolation.Linear.alpha(1.0f)).isEqualTo(1.0f);
    }

    @Test
    @DisplayName("Flipped pairs match expected inverses")
    void flippedPairs() {
        assertThat(Interpolation.QuadIn.flipped()).isEqualTo(Interpolation.QuadOut);
        assertThat(Interpolation.QuadOut.flipped()).isEqualTo(Interpolation.QuadIn);
        assertThat(Interpolation.QuadInOut.flipped()).isEqualTo(Interpolation.QuadInOut);

        assertThat(Interpolation.CubicIn.flipped()).isEqualTo(Interpolation.CubicOut);
        assertThat(Interpolation.QuartIn.flipped()).isEqualTo(Interpolation.QuartOut);
        assertThat(Interpolation.QuintIn.flipped()).isEqualTo(Interpolation.QuintOut);
        assertThat(Interpolation.SineIn.flipped()).isEqualTo(Interpolation.SineOut);
        assertThat(Interpolation.ExpoIn.flipped()).isEqualTo(Interpolation.ExpoOut);
        assertThat(Interpolation.CircIn.flipped()).isEqualTo(Interpolation.CircOut);
        assertThat(Interpolation.ElasticIn.flipped()).isEqualTo(Interpolation.ElasticOut);
        assertThat(Interpolation.BackIn.flipped()).isEqualTo(Interpolation.BackOut);
        assertThat(Interpolation.BounceIn.flipped()).isEqualTo(Interpolation.BounceOut);
    }

    @Test
    @DisplayName("Polynomial easing behaves monotonically")
    void monotonicCurves() {
        Interpolation[] monotonic = {
            Interpolation.Linear,
            Interpolation.QuadIn,
            Interpolation.QuadOut,
            Interpolation.QuadInOut,
            Interpolation.CubicIn,
            Interpolation.CubicOut,
            Interpolation.CubicInOut,
            Interpolation.SineIn,
            Interpolation.SineOut,
            Interpolation.SineInOut,
            Interpolation.ExpoIn,
            Interpolation.ExpoOut,
            Interpolation.ExpoInOut,
            Interpolation.CircIn,
            Interpolation.CircOut,
            Interpolation.CircInOut
        };

        for (Interpolation interp : monotonic) {
            float prev = interp.alpha(0.0f);
            for (float t = 0.05f; t <= 1.0f; t += 0.05f) {
                float curr = interp.alpha(t);
                assertThat(curr)
                        .as("%s should be monotonic at t=%f", interp, t)
                        .isGreaterThanOrEqualTo(prev - 1e-6f);
                prev = curr;
            }
        }
    }

    @Test
    @DisplayName("Back easing exhibits expected overshoot behavior")
    void backEasingOvershoot() {
        // BackIn pulls back below 0 before rising
        assertThat(Interpolation.BackIn.alpha(0.2f)).isLessThan(0.0f);

        // BackOut overshoots above 1 before settling
        assertThat(Interpolation.BackOut.alpha(0.8f)).isGreaterThan(1.0f);
    }

    @Test
    @DisplayName("Bounce easing produces values in [0, 1]")
    void bounceEasingBounds() {
        for (float t = 0.0f; t <= 1.0f; t += 0.02f) {
            assertThat(Interpolation.BounceOut.alpha(t)).isBetween(0.0f, 1.0f);
            assertThat(Interpolation.BounceIn.alpha(t)).isBetween(0.0f, 1.0f);
            assertThat(Interpolation.BounceInOut.alpha(t)).isBetween(0.0f, 1.0f);
        }
    }
}

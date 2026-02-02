/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.spinner;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpinnerStyleConverterTest {

    @Test
    void convertsLowercaseStyle() {
        assertThat(SpinnerStyleConverter.INSTANCE.convert("dots"))
                .isEqualTo(Optional.of(SpinnerStyle.DOTS));
        assertThat(SpinnerStyleConverter.INSTANCE.convert("line"))
                .isEqualTo(Optional.of(SpinnerStyle.LINE));
        assertThat(SpinnerStyleConverter.INSTANCE.convert("gauge"))
                .isEqualTo(Optional.of(SpinnerStyle.GAUGE));
    }

    @Test
    void convertsHyphenatedStyle() {
        assertThat(SpinnerStyleConverter.INSTANCE.convert("bouncing-bar"))
                .isEqualTo(Optional.of(SpinnerStyle.BOUNCING_BAR));
        assertThat(SpinnerStyleConverter.INSTANCE.convert("vertical-gauge"))
                .isEqualTo(Optional.of(SpinnerStyle.VERTICAL_GAUGE));
        assertThat(SpinnerStyleConverter.INSTANCE.convert("growing-dots"))
                .isEqualTo(Optional.of(SpinnerStyle.GROWING_DOTS));
    }

    @Test
    void convertsUnderscoreStyle() {
        assertThat(SpinnerStyleConverter.INSTANCE.convert("bouncing_bar"))
                .isEqualTo(Optional.of(SpinnerStyle.BOUNCING_BAR));
        assertThat(SpinnerStyleConverter.INSTANCE.convert("vertical_gauge"))
                .isEqualTo(Optional.of(SpinnerStyle.VERTICAL_GAUGE));
    }

    @Test
    void isCaseInsensitive() {
        assertThat(SpinnerStyleConverter.INSTANCE.convert("DOTS"))
                .isEqualTo(Optional.of(SpinnerStyle.DOTS));
        assertThat(SpinnerStyleConverter.INSTANCE.convert("Bouncing-Bar"))
                .isEqualTo(Optional.of(SpinnerStyle.BOUNCING_BAR));
        assertThat(SpinnerStyleConverter.INSTANCE.convert("LINE"))
                .isEqualTo(Optional.of(SpinnerStyle.LINE));
    }

    @Test
    void trimsWhitespace() {
        assertThat(SpinnerStyleConverter.INSTANCE.convert("  dots  "))
                .isEqualTo(Optional.of(SpinnerStyle.DOTS));
        assertThat(SpinnerStyleConverter.INSTANCE.convert("\tline\n"))
                .isEqualTo(Optional.of(SpinnerStyle.LINE));
    }

    @Test
    void returnsEmptyForInvalidStyle() {
        assertThat(SpinnerStyleConverter.INSTANCE.convert("invalid"))
                .isEmpty();
        assertThat(SpinnerStyleConverter.INSTANCE.convert("not-a-style"))
                .isEmpty();
    }

    @Test
    void returnsEmptyForNullOrEmpty() {
        assertThat(SpinnerStyleConverter.INSTANCE.convert(null))
                .isEmpty();
        assertThat(SpinnerStyleConverter.INSTANCE.convert(""))
                .isEmpty();
        assertThat(SpinnerStyleConverter.INSTANCE.convert("   "))
                .isEmpty();
    }

    @Test
    void convertsAllStyles() {
        for (SpinnerStyle style : SpinnerStyle.values()) {
            String hyphenated = style.name().toLowerCase().replace('_', '-');
            assertThat(SpinnerStyleConverter.INSTANCE.convert(hyphenated))
                    .describedAs("Style: " + style.name())
                    .isEqualTo(Optional.of(style));
        }
    }
}

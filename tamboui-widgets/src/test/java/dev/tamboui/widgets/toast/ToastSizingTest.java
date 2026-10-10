/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.text.CharWidth;

import static org.assertj.core.api.Assertions.*;

class ToastSizingTest {

    @Test
    @DisplayName("Wraps long messages by display width")
    void wrapsByDisplayWidth() {
        String message = "Hello 世界 this is a long toast message for testing";
        List<String> lines = ToastSizing.wrapMessage(message, 12);

        assertThat(lines).hasSizeGreaterThan(1);
        for (String line : lines) {
            assertThat(CharWidth.of(line)).isLessThanOrEqualTo(12);
        }
    }

    @Test
    @DisplayName("Measures height including title and progress rows")
    void measuresHeight() {
        Toast toast = ToastBuilder.info("Line one line two")
                .title("Title")
                .duration(Duration.ofSeconds(5))
                .build();

        ToastSizing.Dimension dim = ToastSizing.measure(toast, 20);

        assertThat(dim.width()).isGreaterThan(0);
        assertThat(dim.height()).isGreaterThanOrEqualTo(4); // border + title + message + progress
    }
}

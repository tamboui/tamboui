/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ToastBuilderTest {

    @Test
    @DisplayName("Builder creates timed toast with defaults")
    void timedToastDefaults() {
        Toast toast = ToastBuilder.info("Saved").duration(Duration.ofSeconds(3)).build();

        assertThat(toast.type()).isEqualTo(ToastType.INFO);
        assertThat(toast.message()).isEqualTo("Saved");
        assertThat(toast.sticky()).isFalse();
        assertThat(toast.lifetime()).isEqualTo(Duration.ofSeconds(3));
        assertThat(toast.progressStyle()).isEqualTo(ProgressStyle.FULL_BLOCK);
        assertThat(toast.deduplicationKey()).isNull();
    }

    @Test
    @DisplayName("Timed toast without an explicit duration uses the default lifetime")
    void timedToastDefaultsDuration() {
        Toast toast = ToastBuilder.info("Saved").build();

        assertThat(toast.sticky()).isFalse();
        assertThat(toast.lifetime()).isEqualTo(ToastBuilder.DEFAULT_DURATION);
    }

    @Test
    @DisplayName("Sticky toast has no lifetime")
    void stickyToast() {
        Toast toast = ToastBuilder.warning("Check logs").sticky(true).build();

        assertThat(toast.sticky()).isTrue();
        assertThat(toast.lifetime()).isNull();
    }

    @Test
    @DisplayName("Deduplication key is opt-in and overridable")
    void customDeduplicationKey() {
        Toast toast = ToastBuilder.error("Fail")
                .duration(Duration.ofSeconds(5))
                .deduplicationKey("network-error")
                .build();
        assertThat(toast.deduplicationKey()).isEqualTo("network-error");
    }

    @Test
    @DisplayName("Sticky toast rejects an explicit duration")
    void stickyRejectsDuration() {
        assertThatThrownBy(() -> ToastBuilder.warning("Check logs")
                .sticky(true)
                .duration(Duration.ofSeconds(5))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sticky toast cannot have a lifetime");
    }
}

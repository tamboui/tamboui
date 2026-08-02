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
        assertThat(toast.dedupKey()).isNotBlank();
    }

    @Test
    @DisplayName("Sticky toast has no lifetime")
    void stickyToast() {
        Toast toast = ToastBuilder.warning("Check logs").sticky(true).build();

        assertThat(toast.sticky()).isTrue();
        assertThat(toast.lifetime()).isNull();
    }

    @Test
    @DisplayName("Dedup key is overridable")
    void customDedupKey() {
        Toast toast = ToastBuilder.error("Fail")
                .duration(Duration.ofSeconds(5))
                .dedupKey("network-error")
                .build();
        assertThat(toast.dedupKey()).isEqualTo("network-error");
    }

    @Test
    @DisplayName("Default dedup key hashes type+title+message")
    void defaultDedupKey() {
        Toast a = ToastBuilder.success("Done").title("OK").duration(Duration.ofSeconds(5)).build();
        Toast b = ToastBuilder.success("Done").title("OK").duration(Duration.ofSeconds(5)).build();
        Toast c = ToastBuilder.success("Done").title("Nope").duration(Duration.ofSeconds(5)).build();

        assertThat(a.dedupKey()).isEqualTo(b.dedupKey());
        assertThat(a.dedupKey()).isNotEqualTo(c.dedupKey());
    }

    @Test
    @DisplayName("Timed toast requires explicit duration")
    void timedToastRequiresDuration() {
        assertThatThrownBy(() -> ToastBuilder.info("Saved").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timed toast requires a positive duration");
    }
}

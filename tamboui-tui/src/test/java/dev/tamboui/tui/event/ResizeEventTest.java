/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ResizeEventTest {

    @Test
    @DisplayName("of creates resize event with dimensions")
    void ofCreatesResizeEvent() {
        ResizeEvent event = ResizeEvent.of(80, 24);
        assertThat(event.width()).isEqualTo(80);
        assertThat(event.height()).isEqualTo(24);
    }

    @Test
    @DisplayName("resize event is an Event")
    void resizeEventIsEvent() {
        Event event = ResizeEvent.of(100, 50);
        assertThat(event).isInstanceOf(ResizeEvent.class);
    }

    @Test
    @DisplayName("resize event with various dimensions")
    void variousDimensions() {
        assertThat(ResizeEvent.of(120, 40).width()).isEqualTo(120);
        assertThat(ResizeEvent.of(120, 40).height()).isEqualTo(40);
        assertThat(ResizeEvent.of(1, 1).width()).isEqualTo(1);
        assertThat(ResizeEvent.of(1, 1).height()).isEqualTo(1);
    }
}

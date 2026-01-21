/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import dev.tamboui.layout.Padding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class PaddingTest {

    @Test
    @DisplayName("Padding.NONE has all zeros")
    void none() {
        assertThat(Padding.NONE.top()).isEqualTo(0);
        assertThat(Padding.NONE.right()).isEqualTo(0);
        assertThat(Padding.NONE.bottom()).isEqualTo(0);
        assertThat(Padding.NONE.left()).isEqualTo(0);
    }

    @Test
    @DisplayName("Padding.uniform creates equal padding")
    void uniform() {
        Padding padding = Padding.uniform(5);
        assertThat(padding.top()).isEqualTo(5);
        assertThat(padding.right()).isEqualTo(5);
        assertThat(padding.bottom()).isEqualTo(5);
        assertThat(padding.left()).isEqualTo(5);
    }

    @Test
    @DisplayName("Padding.symmetric creates vertical/horizontal padding")
    void symmetric() {
        Padding padding = Padding.symmetric(2, 4);
        assertThat(padding.top()).isEqualTo(2);
        assertThat(padding.bottom()).isEqualTo(2);
        assertThat(padding.left()).isEqualTo(4);
        assertThat(padding.right()).isEqualTo(4);
    }

    @Test
    @DisplayName("Padding.horizontal creates left/right padding")
    void horizontal() {
        Padding padding = Padding.horizontal(3);
        assertThat(padding.top()).isEqualTo(0);
        assertThat(padding.bottom()).isEqualTo(0);
        assertThat(padding.left()).isEqualTo(3);
        assertThat(padding.right()).isEqualTo(3);
    }

    @Test
    @DisplayName("Padding.vertical creates top/bottom padding")
    void vertical() {
        Padding padding = Padding.vertical(3);
        assertThat(padding.top()).isEqualTo(3);
        assertThat(padding.bottom()).isEqualTo(3);
        assertThat(padding.left()).isEqualTo(0);
        assertThat(padding.right()).isEqualTo(0);
    }
}

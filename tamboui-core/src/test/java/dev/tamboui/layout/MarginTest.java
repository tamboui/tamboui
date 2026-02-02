/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MarginTest {

    @Test
    @DisplayName("Margin uniform creates equal margins")
    void uniform() {
        Margin margin = Margin.uniform(5);
        assertThat(margin.top()).isEqualTo(5);
        assertThat(margin.right()).isEqualTo(5);
        assertThat(margin.bottom()).isEqualTo(5);
        assertThat(margin.left()).isEqualTo(5);
    }

    @Test
    @DisplayName("Margin symmetric creates vertical/horizontal margins")
    void symmetric() {
        Margin margin = Margin.symmetric(2, 4);
        assertThat(margin.top()).isEqualTo(2);
        assertThat(margin.bottom()).isEqualTo(2);
        assertThat(margin.left()).isEqualTo(4);
        assertThat(margin.right()).isEqualTo(4);
    }

    @Test
    @DisplayName("Margin horizontal creates left/right margins")
    void horizontal() {
        Margin margin = Margin.horizontal(3);
        assertThat(margin.top()).isEqualTo(0);
        assertThat(margin.bottom()).isEqualTo(0);
        assertThat(margin.left()).isEqualTo(3);
        assertThat(margin.right()).isEqualTo(3);
    }

    @Test
    @DisplayName("Margin vertical creates top/bottom margins")
    void vertical() {
        Margin margin = Margin.vertical(3);
        assertThat(margin.top()).isEqualTo(3);
        assertThat(margin.bottom()).isEqualTo(3);
        assertThat(margin.left()).isEqualTo(0);
        assertThat(margin.right()).isEqualTo(0);
    }

    @Test
    @DisplayName("Margin total calculations")
    void totals() {
        Margin margin = new Margin(1, 2, 3, 4);
        assertThat(margin.horizontalTotal()).isEqualTo(6); // 2 + 4
        assertThat(margin.verticalTotal()).isEqualTo(4);   // 1 + 3
    }

    @Test
    @DisplayName("Margin NONE is all zeros")
    void none() {
        assertThat(Margin.NONE.horizontalTotal()).isEqualTo(0);
        assertThat(Margin.NONE.verticalTotal()).isEqualTo(0);
    }
}

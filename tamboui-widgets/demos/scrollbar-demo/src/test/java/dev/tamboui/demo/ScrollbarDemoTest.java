/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScrollbarDemoTest {

    @Test
    @DisplayName("h and l keys update horizontal scrollbar state and list horizontal offset")
    void horizontalScrollKeys() throws Exception {
        ScrollbarDemo demo = new ScrollbarDemo();

        assertThat(demo.horizontalScrollState().position()).isEqualTo(0);
        assertThat(demo.listState().horizontalOffset()).isEqualTo(0);

        // 'l' scrolls right
        demo.handleKey('l');
        assertThat(demo.horizontalScrollState().position()).isEqualTo(1);
        assertThat(demo.listState().horizontalOffset()).isEqualTo(1);

        demo.handleKey('l');
        demo.handleKey('l');
        assertThat(demo.horizontalScrollState().position()).isEqualTo(3);
        assertThat(demo.listState().horizontalOffset()).isEqualTo(3);

        // 'h' scrolls left
        demo.handleKey('h');
        assertThat(demo.horizontalScrollState().position()).isEqualTo(2);
        assertThat(demo.listState().horizontalOffset()).isEqualTo(2);
    }

    @Test
    @DisplayName("Arrow keys update horizontal scrollbar state and list horizontal offset")
    void horizontalArrowKeys() {
        ScrollbarDemo demo = new ScrollbarDemo();

        // 'C' is Right arrow
        demo.handleArrowKey('C');
        assertThat(demo.horizontalScrollState().position()).isEqualTo(1);
        assertThat(demo.listState().horizontalOffset()).isEqualTo(1);

        // 'D' is Left arrow
        demo.handleArrowKey('D');
        assertThat(demo.horizontalScrollState().position()).isEqualTo(0);
        assertThat(demo.listState().horizontalOffset()).isEqualTo(0);
    }

    @Test
    @DisplayName("Vertical selection keys sync vertical scroll state")
    void verticalSelectionKeys() throws Exception {
        ScrollbarDemo demo = new ScrollbarDemo();

        assertThat(demo.listState().selected()).isEqualTo(0);
        assertThat(demo.verticalScrollState().position()).isEqualTo(0);

        demo.handleKey('j');
        assertThat(demo.listState().selected()).isEqualTo(1);
        assertThat(demo.verticalScrollState().position()).isEqualTo(1);

        demo.handleKey('k');
        assertThat(demo.listState().selected()).isEqualTo(0);
        assertThat(demo.verticalScrollState().position()).isEqualTo(0);
    }
}

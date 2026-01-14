/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlexLayoutTest {

    @Test
    @DisplayName("Flex.START packs elements at the beginning")
    void flexStart() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
                .constraints(
                        Constraint.length(20),
                        Constraint.length(20)
                )
                .flex(Flex.START);

        List<Rect> rects = layout.split(area);

        // Elements should be at positions 0 and 20
        assertThat(rects.get(0).x()).isEqualTo(0);
        assertThat(rects.get(1).x()).isEqualTo(20);
    }

    @Test
    @DisplayName("Flex.END packs elements at the end")
    void flexEnd() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
                .constraints(
                        Constraint.length(20),
                        Constraint.length(20)
                )
                .flex(Flex.END);

        List<Rect> rects = layout.split(area);

        // 40 used, 60 remaining space at start
        assertThat(rects.get(0).x()).isEqualTo(60);
        assertThat(rects.get(1).x()).isEqualTo(80);
    }

    @Test
    @DisplayName("Flex.CENTER centers elements")
    void flexCenter() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
                .constraints(
                        Constraint.length(20),
                        Constraint.length(20)
                )
                .flex(Flex.CENTER);

        List<Rect> rects = layout.split(area);

        // 40 used, 60 remaining, 30 on each side
        assertThat(rects.get(0).x()).isEqualTo(30);
        assertThat(rects.get(1).x()).isEqualTo(50);
    }

    @Test
    @DisplayName("Flex.SPACE_BETWEEN distributes space between elements")
    void flexSpaceBetween() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
                .constraints(
                        Constraint.length(20),
                        Constraint.length(20),
                        Constraint.length(20)
                )
                .flex(Flex.SPACE_BETWEEN);

        List<Rect> rects = layout.split(area);

        // 60 used, 40 remaining, distributed between 3 elements = 20 each gap
        assertThat(rects.get(0).x()).isEqualTo(0);
        assertThat(rects.get(1).x()).isEqualTo(40);
        assertThat(rects.get(2).x()).isEqualTo(80);
    }

    @Test
    @DisplayName("Flex.SPACE_AROUND distributes space around elements")
    void flexSpaceAround() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
                .constraints(
                        Constraint.length(20),
                        Constraint.length(20)
                )
                .flex(Flex.SPACE_AROUND);

        List<Rect> rects = layout.split(area);

        // 40 used, 60 remaining
        // SPACE_AROUND: 4 units (2 elements * 2), 60/4 = 15 per unit
        // Edge gaps = 15, between gap = 30
        // First element starts at 15
        assertThat(rects.get(0).x()).isEqualTo(15);
        // Second element starts at 15 + 20 + 30 = 65
        assertThat(rects.get(1).x()).isEqualTo(65);
    }

    @Test
    @DisplayName("Flex mode works with vertical layout")
    void flexVertical() {
        Rect area = new Rect(0, 0, 50, 100);
        Layout layout = Layout.vertical()
                .constraints(
                        Constraint.length(20),
                        Constraint.length(20)
                )
                .flex(Flex.END);

        List<Rect> rects = layout.split(area);

        // 40 used, 60 remaining at start
        assertThat(rects.get(0).y()).isEqualTo(60);
        assertThat(rects.get(1).y()).isEqualTo(80);
    }

    @Test
    @DisplayName("Flex mode works with spacing")
    void flexWithSpacing() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
                .constraints(
                        Constraint.length(20),
                        Constraint.length(20)
                )
                .spacing(10)
                .flex(Flex.CENTER);

        List<Rect> rects = layout.split(area);

        // 20 + 10 + 20 = 50 used, 50 remaining, 25 on each side
        assertThat(rects.get(0).x()).isEqualTo(25);
        assertThat(rects.get(1).x()).isEqualTo(55); // 25 + 20 + 10
    }

    @Test
    @DisplayName("Flex mode with margin")
    void flexWithMargin() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
                .constraints(
                        Constraint.length(20),
                        Constraint.length(20)
                )
                .margin(10)
                .flex(Flex.CENTER);

        List<Rect> rects = layout.split(area);

        // Inner area: 80x30, 40 used, 40 remaining, 20 on each side
        // Plus margin offset of 10
        assertThat(rects.get(0).x()).isEqualTo(30); // 10 (margin) + 20 (flex gap)
    }

    @Test
    @DisplayName("Flex with single element behaves correctly")
    void flexSingleElement() {
        Rect area = new Rect(0, 0, 100, 50);

        // CENTER with single element
        Layout center = Layout.horizontal()
                .constraints(Constraint.length(20))
                .flex(Flex.CENTER);
        List<Rect> centerRects = center.split(area);
        assertThat(centerRects.get(0).x()).isEqualTo(40); // (100-20)/2

        // END with single element
        Layout end = Layout.horizontal()
                .constraints(Constraint.length(20))
                .flex(Flex.END);
        List<Rect> endRects = end.split(area);
        assertThat(endRects.get(0).x()).isEqualTo(80); // 100-20

        // SPACE_BETWEEN with single element behaves like CENTER
        Layout between = Layout.horizontal()
                .constraints(Constraint.length(20))
                .flex(Flex.SPACE_BETWEEN);
        List<Rect> betweenRects = between.split(area);
        assertThat(betweenRects.get(0).x()).isEqualTo(40);
    }

    @Test
    @DisplayName("Flex has no effect when all space is used")
    void flexNoExtraSpace() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
                .constraints(
                        Constraint.length(50),
                        Constraint.length(50)
                )
                .flex(Flex.CENTER);

        List<Rect> rects = layout.split(area);

        // No remaining space, so no flex effect
        assertThat(rects.get(0).x()).isEqualTo(0);
        assertThat(rects.get(1).x()).isEqualTo(50);
    }
}

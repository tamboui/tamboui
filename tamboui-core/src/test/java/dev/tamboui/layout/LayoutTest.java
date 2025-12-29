/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class LayoutTest {

    @Test
    @DisplayName("Vertical layout with fixed lengths")
    void verticalFixedLengths() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .constraints(
                Constraint.length(20),
                Constraint.length(30),
                Constraint.length(50)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(3);
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 0, 100, 20));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 20, 100, 30));
        assertThat(rects.get(2)).isEqualTo(new Rect(0, 50, 100, 50));
    }

    @Test
    @DisplayName("Horizontal layout with fixed lengths")
    void horizontalFixedLengths() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
            .constraints(
                Constraint.length(30),
                Constraint.length(70)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 0, 30, 50));
        assertThat(rects.get(1)).isEqualTo(new Rect(30, 0, 70, 50));
    }

    @Test
    @DisplayName("Layout with percentages")
    void percentages() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .constraints(
                Constraint.percentage(25),
                Constraint.percentage(75)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        assertThat(rects.get(0).height()).isEqualTo(25);
        assertThat(rects.get(1).height()).isEqualTo(75);
    }

    @Test
    @DisplayName("Layout with fill constraints")
    void fillConstraints() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .constraints(
                Constraint.length(20),
                Constraint.fill()
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        assertThat(rects.get(0).height()).isEqualTo(20);
        assertThat(rects.get(1).height()).isEqualTo(80);
    }

    @Test
    @DisplayName("Layout with margin")
    void withMargin() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .margin(Margin.uniform(10))
            .constraints(Constraint.fill());

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(1);
        assertThat(rects.get(0)).isEqualTo(new Rect(10, 10, 80, 80));
    }

    @Test
    @DisplayName("Layout with spacing")
    void withSpacing() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .spacing(10)
            .constraints(
                Constraint.length(40),
                Constraint.length(40)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 0, 100, 40));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 50, 100, 40));
    }

    @Test
    @DisplayName("Layout with min constraint")
    void minConstraint() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .constraints(
                Constraint.min(30),
                Constraint.fill()
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        assertThat(rects.get(0).height()).isGreaterThanOrEqualTo(30);
    }

    @Test
    @DisplayName("Layout with ratio constraint")
    void ratioConstraint() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .constraints(
                Constraint.ratio(1, 3),
                Constraint.ratio(2, 3)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        assertThat(rects.get(0).height()).isEqualTo(33);
        assertThat(rects.get(1).height()).isEqualTo(66);
    }

    @Test
    @DisplayName("Flex START packs elements at the start")
    void flexStart() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .flex(Flex.START)
            .constraints(
                Constraint.length(20),
                Constraint.length(30)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 0, 100, 20));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 20, 100, 30));
    }

    @Test
    @DisplayName("Flex END packs elements at the end")
    void flexEnd() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .flex(Flex.END)
            .constraints(
                Constraint.length(20),
                Constraint.length(30)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        // 50 units remaining, so offset by 50
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 50, 100, 20));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 70, 100, 30));
    }

    @Test
    @DisplayName("Flex CENTER centers elements")
    void flexCenter() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .flex(Flex.CENTER)
            .constraints(
                Constraint.length(20),
                Constraint.length(30)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        // 50 units remaining, so offset by 25
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 25, 100, 20));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 45, 100, 30));
    }

    @Test
    @DisplayName("Flex SPACE_BETWEEN distributes space between elements")
    void flexSpaceBetween() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .flex(Flex.SPACE_BETWEEN)
            .constraints(
                Constraint.length(20),
                Constraint.length(20),
                Constraint.length(20)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(3);
        // 40 units remaining, distributed between 2 gaps = 20 each
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 0, 100, 20));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 40, 100, 20));
        assertThat(rects.get(2)).isEqualTo(new Rect(0, 80, 100, 20));
    }

    @Test
    @DisplayName("Flex SPACE_AROUND distributes space around elements")
    void flexSpaceAround() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .flex(Flex.SPACE_AROUND)
            .constraints(
                Constraint.length(20),
                Constraint.length(20)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        // 60 units remaining, 60/2=30 per element, start offset = 15
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 15, 100, 20));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 65, 100, 20));
    }

    @Test
    @DisplayName("Flex SPACE_EVENLY distributes space uniformly")
    void flexSpaceEvenly() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .flex(Flex.SPACE_EVENLY)
            .constraints(
                Constraint.length(20),
                Constraint.length(20)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        // 60 units remaining, 60/3=20 per gap (3 gaps for 2 elements)
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 20, 100, 20));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 60, 100, 20));
    }

    @Test
    @DisplayName("Flex LEGACY adds remaining space to last element")
    void flexLegacy() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .flex(Flex.LEGACY)
            .constraints(
                Constraint.length(20),
                Constraint.length(30)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        // 50 units remaining, added to last element
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 0, 100, 20));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 20, 100, 80));
    }

    @Test
    @DisplayName("Flex works with horizontal layout")
    void flexHorizontal() {
        Rect area = new Rect(0, 0, 100, 50);
        Layout layout = Layout.horizontal()
            .flex(Flex.CENTER)
            .constraints(
                Constraint.length(20),
                Constraint.length(20)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        // 60 units remaining, offset by 30
        assertThat(rects.get(0)).isEqualTo(new Rect(30, 0, 20, 50));
        assertThat(rects.get(1)).isEqualTo(new Rect(50, 0, 20, 50));
    }

    @Test
    @DisplayName("Flex with no remaining space behaves like START")
    void flexNoRemainingSpace() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .flex(Flex.CENTER)
            .constraints(
                Constraint.length(50),
                Constraint.length(50)
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(2);
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 0, 100, 50));
        assertThat(rects.get(1)).isEqualTo(new Rect(0, 50, 100, 50));
    }

    @Test
    @DisplayName("Flex SPACE_BETWEEN with single element behaves like START")
    void flexSpaceBetweenSingleElement() {
        Rect area = new Rect(0, 0, 100, 100);
        Layout layout = Layout.vertical()
            .flex(Flex.SPACE_BETWEEN)
            .constraints(Constraint.length(20));

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(1);
        assertThat(rects.get(0)).isEqualTo(new Rect(0, 0, 100, 20));
    }
}

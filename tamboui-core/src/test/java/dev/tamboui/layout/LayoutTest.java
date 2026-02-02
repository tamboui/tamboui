/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        // With exact Fraction arithmetic, 2/3 of 100 = 66.66... rounds to 67
        // using largest remainder method to preserve the total of 100
        assertThat(rects.get(1).height()).isEqualTo(67);
    }

    @Test
    @DisplayName("Horizontal layout with 4 fill constraints does not exceed bounds")
    void fourFillConstraintsDoNotExceedBounds() {
        // Simulates Row in Panel: inner area (1, 1, 18, 1), 4 children with fill()
        Rect area = new Rect(1, 1, 18, 1);
        Layout layout = Layout.horizontal()
            .constraints(
                Constraint.fill(),
                Constraint.fill(),
                Constraint.fill(),
                Constraint.fill()
            );

        List<Rect> rects = layout.split(area);

        assertThat(rects).hasSize(4);

        // Total width should equal input width (18)
        int totalWidth = rects.stream().mapToInt(Rect::width).sum();
        assertThat(totalWidth).as("total width").isEqualTo(area.width());

        // All rects should be within the input area bounds
        for (int i = 0; i < rects.size(); i++) {
            Rect rect = rects.get(i);
            assertThat(rect.left()).as("rect[" + i + "] left").isGreaterThanOrEqualTo(area.left());
            assertThat(rect.right()).as("rect[" + i + "] right").isLessThanOrEqualTo(area.right());
        }
    }
}

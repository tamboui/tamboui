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
    @DisplayName("Spacing is taken out once: two fills with spacing 2 in width 10 are 4 wide each")
    void spacingBetweenFills() {
        List<Rect> rects = Layout.horizontal()
            .spacing(2)
            .constraints(Constraint.fill(), Constraint.fill())
            .split(new Rect(0, 0, 10, 1));

        assertThat(rects).containsExactly(new Rect(0, 0, 4, 1), new Rect(6, 0, 4, 1));
    }

    @Test
    @DisplayName("Spacing is taken out once: a fill between a header and a footer reaches the footer")
    void spacingAroundFill() {
        List<Rect> rects = Layout.vertical()
            .spacing(1)
            .constraints(Constraint.length(3), Constraint.fill(), Constraint.length(1))
            .split(new Rect(0, 0, 80, 24));

        // 24 rows: header 3, gap, fill 18, gap, footer 1 on the last row
        assertThat(rects).containsExactly(
            new Rect(0, 0, 80, 3), new Rect(0, 4, 80, 18), new Rect(0, 23, 80, 1));
    }

    @Test
    @DisplayName("Fill(0) next to a weighted fill takes no space and does not break the solver")
    void fillZeroNextToWeightedFill() {
        Rect area = new Rect(0, 0, 20, 1);

        assertThat(Layout.horizontal().constraints(Constraint.length(5), Constraint.fill(0), Constraint.fill(1))
            .split(area))
            .containsExactly(new Rect(0, 0, 5, 1), new Rect(5, 0, 0, 1), new Rect(5, 0, 15, 1));
        assertThat(Layout.horizontal().constraints(Constraint.fill(0), Constraint.fill(1), Constraint.fill(0))
            .split(area))
            .containsExactly(new Rect(0, 0, 0, 1), new Rect(0, 0, 20, 1), new Rect(20, 0, 0, 1));

        List<Rect> withMin = Layout.horizontal()
            .constraints(Constraint.fill(1), Constraint.fill(0), Constraint.min(5))
            .split(area);
        assertThat(withMin.get(1).width()).isZero();
        assertThat(withMin.stream().mapToInt(Rect::width).sum()).isEqualTo(20);
    }

    @Test
    @DisplayName("Fills that are all Fill(0) share the space equally")
    void allFillZeroShareEqually() {
        List<Rect> rects = Layout.horizontal()
            .constraints(Constraint.fill(0), Constraint.fill(0))
            .split(new Rect(0, 0, 10, 1));

        assertThat(rects).containsExactly(new Rect(0, 0, 5, 1), new Rect(5, 0, 5, 1));
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

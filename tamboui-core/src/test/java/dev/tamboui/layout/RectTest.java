/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class RectTest {

    @Test
    @DisplayName("Rect area calculation")
    void areaCalculation() {
        Rect rect = new Rect(0, 0, 10, 5);
        assertThat(rect.area()).isEqualTo(50);
    }

    @Test
    @DisplayName("Rect with zero dimensions has zero area")
    void zeroArea() {
        assertThat(new Rect(0, 0, 0, 5).area()).isEqualTo(0);
        assertThat(new Rect(0, 0, 5, 0).area()).isEqualTo(0);
        assertThat(Rect.ZERO.area()).isEqualTo(0);
    }

    @Test
    @DisplayName("Rect boundaries")
    void boundaries() {
        Rect rect = new Rect(5, 10, 20, 30);
        assertThat(rect.left()).isEqualTo(5);
        assertThat(rect.right()).isEqualTo(25);
        assertThat(rect.top()).isEqualTo(10);
        assertThat(rect.bottom()).isEqualTo(40);
    }

    @Test
    @DisplayName("Rect isEmpty")
    void isEmpty() {
        assertThat(new Rect(0, 0, 0, 5).isEmpty()).isTrue();
        assertThat(new Rect(0, 0, 5, 0).isEmpty()).isTrue();
        assertThat(new Rect(0, 0, 5, 5).isEmpty()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"0, 0, true", "5, 5, true", "9, 9, true", "10, 10, false", "-1, 0, false",
            "0, -1, false"})
    @DisplayName("Rect contains position")
    void containsPosition(int x, int y, boolean expected) {
        Rect rect = new Rect(0, 0, 10, 10);
        assertThat(rect.contains(new Position(x, y))).isEqualTo(expected);
    }

    @Test
    @DisplayName("Rect intersection")
    void intersection() {
        Rect a = new Rect(0, 0, 10, 10);
        Rect b = new Rect(5, 5, 10, 10);
        Rect intersection = a.intersection(b);

        assertThat(intersection).isEqualTo(new Rect(5, 5, 5, 5));
    }

    @Test
    @DisplayName("Rect intersection with no overlap returns ZERO")
    void intersectionNoOverlap() {
        Rect a = new Rect(0, 0, 5, 5);
        Rect b = new Rect(10, 10, 5, 5);

        assertThat(a.intersection(b)).isEqualTo(Rect.ZERO);
    }

    @Test
    @DisplayName("Rect union")
    void union() {
        Rect a = new Rect(0, 0, 5, 5);
        Rect b = new Rect(3, 3, 5, 5);
        Rect union = a.union(b);

        assertThat(union).isEqualTo(new Rect(0, 0, 8, 8));
    }

    @Test
    @DisplayName("Rect inner with margin")
    void innerWithMargin() {
        Rect rect = new Rect(0, 0, 20, 20);
        Margin margin = new Margin(2, 3, 4, 5);
        Rect inner = rect.inner(margin);

        assertThat(inner.x()).isEqualTo(5);
        assertThat(inner.y()).isEqualTo(2);
        assertThat(inner.width()).isEqualTo(12); // 20 - 5 - 3
        assertThat(inner.height()).isEqualTo(14); // 20 - 2 - 4
    }

    @Test
    @DisplayName("Rect inner with margin larger than rect returns zero-size rect")
    void innerWithLargeMargin() {
        Rect rect = new Rect(0, 0, 10, 10);
        Margin margin = Margin.uniform(10);
        Rect inner = rect.inner(margin);

        assertThat(inner.width()).isEqualTo(0);
        assertThat(inner.height()).isEqualTo(0);
    }

    @Test
    @DisplayName("Rect.of factory methods")
    void factoryMethods() {
        assertThat(Rect.of(10, 20)).isEqualTo(new Rect(0, 0, 10, 20));
        assertThat(Rect.of(new Position(5, 10), new Size(15, 25)))
                .isEqualTo(new Rect(5, 10, 15, 25));
    }

    @Test
    @DisplayName("Rect position and size")
    void positionAndSize() {
        Rect rect = new Rect(5, 10, 15, 20);
        assertThat(rect.position()).isEqualTo(new Position(5, 10));
        assertThat(rect.size()).isEqualTo(new Size(15, 20));
    }
}

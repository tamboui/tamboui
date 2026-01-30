/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas.shapes;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Context;
import dev.tamboui.widgets.canvas.Marker;
import dev.tamboui.widgets.canvas.Painter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointsTest {

    @Test
    void constructor_creates_points() {
        double[][] coords = {{1, 2}, {3, 4}, {5, 6}};
        Points points = new Points(coords, Color.RED);

        assertThat(points.coords()).isEqualTo(coords);
        assertThat(points.color()).isEqualTo(Color.RED);
    }

    @Test
    void of_coords_creates_points() {
        double[][] coords = {{10, 20}, {30, 40}};
        Points points = Points.of(coords, Color.BLUE);

        assertThat(points.coords()).isEqualTo(coords);
        assertThat(points.color()).isEqualTo(Color.BLUE);
    }

    @Test
    void of_xy_arrays_creates_points() {
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {10, 20, 30, 40, 50};
        Points points = Points.of(x, y, Color.GREEN);

        assertThat(points.coords().length).isEqualTo(5);
        assertThat(points.coords()[0]).containsExactly(1.0, 10.0);
        assertThat(points.coords()[4]).containsExactly(5.0, 50.0);
        assertThat(points.color()).isEqualTo(Color.GREEN);
    }

    @Test
    void of_xy_arrays_different_lengths_throws() {
        double[] x = {1, 2, 3};
        double[] y = {10, 20};

        assertThatThrownBy(() -> Points.of(x, y, Color.RED))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("same length");
    }

    @Test
    void draw_points() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Points points = new Points(new double[][]{{0, 0}, {5, 5}, {10, 10}}, Color.YELLOW);

        points.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_single_point() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Points points = new Points(new double[][]{{5, 5}}, Color.CYAN);

        points.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_empty_points() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Points points = new Points(new double[0][], Color.MAGENTA);

        points.draw(new Painter(ctx));
        // Should not throw
    }

    @Test
    void draw_null_coords() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Points points = new Points(null, Color.WHITE);

        points.draw(new Painter(ctx));
        // Should not throw
    }

    @Test
    void draw_points_outside_bounds() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Points points = new Points(new double[][]{{-5, -5}, {15, 15}}, Color.RED);

        points.draw(new Painter(ctx));
        // Should clip and not throw
    }

    @Test
    void draw_points_partial_null() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        double[][] coords = new double[3][];
        coords[0] = new double[]{5, 5};
        coords[1] = null;
        coords[2] = new double[]{3, 3};
        Points points = new Points(coords, Color.GREEN);

        points.draw(new Painter(ctx));
        // Should skip null entries and not throw
    }
}

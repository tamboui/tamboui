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

class CircleTest {

    @Test
    void constructor_creates_circle() {
        Circle circle = new Circle(10, 20, 5, Color.RED);

        assertThat(circle.x()).isEqualTo(10);
        assertThat(circle.y()).isEqualTo(20);
        assertThat(circle.radius()).isEqualTo(5);
        assertThat(circle.color()).isEqualTo(Color.RED);
    }

    @Test
    void of_creates_circle() {
        Circle circle = Circle.of(15, 25, 10, Color.BLUE);

        assertThat(circle.x()).isEqualTo(15);
        assertThat(circle.y()).isEqualTo(25);
        assertThat(circle.radius()).isEqualTo(10);
        assertThat(circle.color()).isEqualTo(Color.BLUE);
    }

    @Test
    void draw_circle() {
        Context ctx = new Context(20, 20, new double[]{0, 20}, new double[]{0, 20}, Marker.DOT);
        Circle circle = new Circle(10, 10, 5, Color.GREEN);

        circle.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_small_circle() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Circle circle = new Circle(5, 5, 1, Color.YELLOW);

        circle.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_large_circle() {
        Context ctx = new Context(50, 50, new double[]{0, 50}, new double[]{0, 50}, Marker.DOT);
        Circle circle = new Circle(25, 25, 20, Color.CYAN);

        circle.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_circle_at_origin() {
        Context ctx = new Context(20, 20, new double[]{-10, 10}, new double[]{-10, 10}, Marker.DOT);
        Circle circle = new Circle(0, 0, 5, Color.MAGENTA);

        circle.draw(new Painter(ctx));
        // Should not throw
    }

    @Test
    void draw_circle_partial_visible() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Circle circle = new Circle(0, 0, 5, Color.WHITE);

        circle.draw(new Painter(ctx));
        // Should clip and not throw
    }

    @Test
    void draw_zero_radius_circle() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Circle circle = new Circle(5, 5, 0, Color.RED);

        circle.draw(new Painter(ctx));
        // Should not throw, should do nothing
    }

    @Test
    void draw_negative_radius_circle() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Circle circle = new Circle(5, 5, -5, Color.RED);

        circle.draw(new Painter(ctx));
        // Should not throw, should do nothing
    }
}

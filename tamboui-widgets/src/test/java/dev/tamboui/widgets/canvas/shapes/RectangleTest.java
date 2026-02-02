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

class RectangleTest {

    @Test
    void constructor_creates_rectangle() {
        Rectangle rect = new Rectangle(10, 20, 30, 40, Color.RED);

        assertThat(rect.x()).isEqualTo(10);
        assertThat(rect.y()).isEqualTo(20);
        assertThat(rect.width()).isEqualTo(30);
        assertThat(rect.height()).isEqualTo(40);
        assertThat(rect.color()).isEqualTo(Color.RED);
    }

    @Test
    void of_creates_rectangle() {
        Rectangle rect = Rectangle.of(5, 10, 15, 20, Color.BLUE);

        assertThat(rect.x()).isEqualTo(5);
        assertThat(rect.y()).isEqualTo(10);
        assertThat(rect.width()).isEqualTo(15);
        assertThat(rect.height()).isEqualTo(20);
        assertThat(rect.color()).isEqualTo(Color.BLUE);
    }

    @Test
    void draw_rectangle() {
        Context ctx = new Context(20, 20, new double[] {0, 20}, new double[] {0, 20}, Marker.DOT);
        Rectangle rect = new Rectangle(5, 5, 10, 10, Color.GREEN);

        rect.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_small_rectangle() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Rectangle rect = new Rectangle(2, 2, 2, 2, Color.YELLOW);

        rect.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_rectangle_at_origin() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Rectangle rect = new Rectangle(0, 0, 5, 5, Color.CYAN);

        rect.draw(new Painter(ctx));
        // Should not throw
    }

    @Test
    void draw_rectangle_partial_visible() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Rectangle rect = new Rectangle(-5, -5, 10, 10, Color.MAGENTA);

        rect.draw(new Painter(ctx));
        // Should clip and not throw
    }
}

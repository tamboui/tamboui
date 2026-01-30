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

class LineTest {

    @Test
    void constructor_creates_line() {
        Line line = new Line(0, 0, 10, 10, Color.RED);

        assertThat(line.x1()).isEqualTo(0);
        assertThat(line.y1()).isEqualTo(0);
        assertThat(line.x2()).isEqualTo(10);
        assertThat(line.y2()).isEqualTo(10);
        assertThat(line.color()).isEqualTo(Color.RED);
    }

    @Test
    void of_creates_line() {
        Line line = Line.of(5, 10, 15, 20, Color.BLUE);

        assertThat(line.x1()).isEqualTo(5);
        assertThat(line.y1()).isEqualTo(10);
        assertThat(line.x2()).isEqualTo(15);
        assertThat(line.y2()).isEqualTo(20);
        assertThat(line.color()).isEqualTo(Color.BLUE);
    }

    @Test
    void draw_horizontal_line() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Line line = new Line(0, 5, 10, 5, Color.GREEN);

        line.draw(new Painter(ctx));

        // Line should have painted points
        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_vertical_line() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Line line = new Line(5, 0, 5, 10, Color.YELLOW);

        line.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_diagonal_line() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Line line = new Line(0, 0, 10, 10, Color.CYAN);

        line.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_point_line() {
        // Line with same start and end points
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Line line = new Line(5, 5, 5, 5, Color.MAGENTA);

        line.draw(new Painter(ctx));
        // Should not throw
    }

    @Test
    void draw_line_outside_bounds() {
        Context ctx = new Context(10, 10, new double[]{0, 10}, new double[]{0, 10}, Marker.DOT);
        Line line = new Line(-10, -10, 20, 20, Color.WHITE);

        line.draw(new Painter(ctx));
        // Should clip and not throw
    }
}

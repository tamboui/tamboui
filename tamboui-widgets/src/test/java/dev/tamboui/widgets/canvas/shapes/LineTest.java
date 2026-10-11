/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas.shapes;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Context;
import dev.tamboui.widgets.canvas.Marker;
import dev.tamboui.widgets.canvas.Painter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

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
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Line line = new Line(0, 5, 10, 5, Color.GREEN);

        line.draw(new Painter(ctx));

        // Line should have painted points
        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_vertical_line() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Line line = new Line(5, 0, 5, 10, Color.YELLOW);

        line.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_diagonal_line() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Line line = new Line(0, 0, 10, 10, Color.CYAN);

        line.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_point_line() {
        // Line with same start and end points
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Line line = new Line(5, 5, 5, 5, Color.MAGENTA);

        line.draw(new Painter(ctx));
        // Should not throw
    }

    @Test
    void draw_line_outside_bounds() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Line line = new Line(-10, -10, 20, 20, Color.WHITE);

        line.draw(new Painter(ctx));
        // Should clip and not throw
    }

    @Test
    void line_partly_outside_the_bounds_is_drawn_up_to_the_edge() {
        // Default bounds [0, 1]
        Buffer buffer = render(Canvas.builder(), new Line(0.1, 0.5, 1.5, 0.5, Color.RED));

        BufferAssertions.assertThat(buffer).hasContent(
            "                    ",
            "                    ",
            "  ⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤",
            "                    ",
            "                    ");
    }

    @Test
    void line_to_a_point_far_outside_large_bounds_is_drawn_without_stepping_along_it() {
        Canvas.Builder canvas = Canvas.builder().xBounds(-2e7, 2e7).yBounds(-2e7, 2e7);

        Buffer buffer = assertTimeoutPreemptively(Duration.ofSeconds(2),
            () -> render(canvas, new Line(-1e7, 0, 2e9, 0, Color.RED)));

        BufferAssertions.assertThat(buffer).hasContent(
            "                    ",
            "                    ",
            "     ⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤⠤",
            "                    ",
            "                    ");
    }

    private static Buffer render(Canvas.Builder canvas, Line line) {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        canvas.paint(ctx -> ctx.draw(line)).build().render(area, buffer);
        return buffer;
    }
}

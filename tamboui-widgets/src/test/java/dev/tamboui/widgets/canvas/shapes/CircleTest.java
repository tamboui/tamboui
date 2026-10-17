/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas.shapes;

import java.util.ArrayList;
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
        Context ctx = new Context(20, 20, new double[] {0, 20}, new double[] {0, 20}, Marker.DOT);
        Circle circle = new Circle(10, 10, 5, Color.GREEN);

        circle.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_small_circle() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Circle circle = new Circle(5, 5, 1, Color.YELLOW);

        circle.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_large_circle() {
        Context ctx = new Context(50, 50, new double[] {0, 50}, new double[] {0, 50}, Marker.DOT);
        Circle circle = new Circle(25, 25, 20, Color.CYAN);

        circle.draw(new Painter(ctx));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).isNotEmpty();
    }

    @Test
    void draw_circle_at_origin() {
        Context ctx = new Context(20, 20, new double[] {-10, 10}, new double[] {-10, 10}, Marker.DOT);
        Circle circle = new Circle(0, 0, 5, Color.MAGENTA);

        circle.draw(new Painter(ctx));
        // Should not throw
    }

    @Test
    void draw_circle_partial_visible() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Circle circle = new Circle(0, 0, 5, Color.WHITE);

        circle.draw(new Painter(ctx));
        // Should clip and not throw
    }

    @Test
    void draw_zero_radius_circle() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Circle circle = new Circle(5, 5, 0, Color.RED);

        circle.draw(new Painter(ctx));
        // Should not throw, should do nothing
    }

    @Test
    void draw_negative_radius_circle() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);
        Circle circle = new Circle(5, 5, -5, Color.RED);

        circle.draw(new Painter(ctx));
        // Should not throw, should do nothing
    }

    @Test
    void small_circle_is_drawn_as_a_ring() {
        Buffer buffer = render(Canvas.builder().xBounds(0, 0.1).yBounds(0, 0.1), new Circle(0.05, 0.05, 0.02, Color.RED));

        BufferAssertions.assertThat(buffer).hasContent(
            "                    ",
            "      ⢀⣠⠤⠤⠤⠤⣄⡀      ",
            "      ⣏      ⣹      ",
            "      ⠈⠙⠒⠒⠒⠒⠋⠁      ",
            "                    ");
    }

    @Test
    void circle_has_no_gaps_with_several_grid_points_per_canvas_unit() {
        // 40 x 40 braille points for 10 x 10 canvas units
        Context ctx = new Context(20, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.BRAILLE);

        new Circle(5, 5, 4, Color.GREEN).draw(new Painter(ctx));

        // A closed curve has no ends: each point touches the points before and after it
        Color[][] grid = ctx.allLayers().get(0);
        int painted = 0;
        List<String> curveEnds = new ArrayList<>();
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                if (grid[y][x] != null) {
                    painted++;
                    if (paintedNeighbours(grid, x, y) < 2) {
                        curveEnds.add(x + "," + y);
                    }
                }
            }
        }
        assertThat(painted).isPositive();
        assertThat(curveEnds).isEmpty();
    }

    @Test
    void circle_much_larger_than_the_canvas_draws_its_arc_across_it() {
        // The top of the circle, at y = 0.6, crosses the default [0, 1] bounds
        Buffer buffer = render(Canvas.builder(), new Circle(0.5, -1e6, 1e6 + 0.6, Color.RED));

        BufferAssertions.assertThat(buffer).hasContent(
            "                    ",
            "                    ",
            "⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉⠉",
            "                    ",
            "                    ");
    }

    private static Buffer render(Canvas.Builder canvas, Circle circle) {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        canvas.paint(ctx -> ctx.draw(circle)).build().render(area, buffer);
        return buffer;
    }

    private static int paintedNeighbours(Color[][] grid, int x, int y) {
        int count = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if ((dx != 0 || dy != 0) && ny >= 0 && ny < grid.length && nx >= 0 && nx < grid[ny].length
                        && grid[ny][nx] != null) {
                    count++;
                }
            }
        }
        return count;
    }
}

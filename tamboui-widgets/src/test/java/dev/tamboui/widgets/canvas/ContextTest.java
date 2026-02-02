/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.canvas.shapes.Line;

import static org.assertj.core.api.Assertions.assertThat;

class ContextTest {

    @Test
    void constructor_creates_context() {
        Context ctx = new Context(20, 10, new double[] {0, 100}, new double[] {0, 50}, Marker.DOT);

        assertThat(ctx.width()).isEqualTo(20);
        assertThat(ctx.height()).isEqualTo(10);
        assertThat(ctx.xBounds()).containsExactly(0.0, 100.0);
        assertThat(ctx.yBounds()).containsExactly(0.0, 50.0);
        assertThat(ctx.marker()).isEqualTo(Marker.DOT);
    }

    @Test
    void grid_dimensions_for_dot_marker() {
        Context ctx = new Context(20, 10, new double[] {0, 100}, new double[] {0, 50}, Marker.DOT);

        assertThat(ctx.gridWidth()).isEqualTo(20);
        assertThat(ctx.gridHeight()).isEqualTo(10);
    }

    @Test
    void grid_dimensions_for_braille_marker() {
        Context ctx = new Context(20, 10, new double[] {0, 100}, new double[] {0, 50}, Marker.BRAILLE);

        assertThat(ctx.gridWidth()).isEqualTo(40);  // 2x horizontal
        assertThat(ctx.gridHeight()).isEqualTo(40); // 4x vertical
    }

    @Test
    void grid_dimensions_for_half_block_marker() {
        Context ctx = new Context(20, 10, new double[] {0, 100}, new double[] {0, 50}, Marker.HALF_BLOCK);

        assertThat(ctx.gridWidth()).isEqualTo(20);
        assertThat(ctx.gridHeight()).isEqualTo(20); // 2x vertical
    }

    @Test
    void draw_adds_to_grid() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);

        ctx.draw(new Line(0, 0, 10, 10, Color.RED));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).hasSize(1);
    }

    @Test
    void print_string_adds_label() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);

        ctx.print(5, 5, "Hello");

        assertThat(ctx.labels()).hasSize(1);
        assertThat(ctx.labels().get(0).x()).isEqualTo(5.0);
        assertThat(ctx.labels().get(0).y()).isEqualTo(5.0);
        assertThat(ctx.labels().get(0).line().rawContent()).isEqualTo("Hello");
    }

    @Test
    void print_line_adds_label() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);

        ctx.print(3, 7, dev.tamboui.text.Line.from("World"));

        assertThat(ctx.labels()).hasSize(1);
        assertThat(ctx.labels().get(0).x()).isEqualTo(3.0);
        assertThat(ctx.labels().get(0).y()).isEqualTo(7.0);
    }

    @Test
    void print_span_adds_label() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);

        ctx.print(1, 2, Span.raw("Test"));

        assertThat(ctx.labels()).hasSize(1);
    }

    @Test
    void layer_saves_current_grid() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);

        ctx.draw(new Line(0, 0, 5, 5, Color.RED));
        ctx.layer();
        ctx.draw(new Line(5, 5, 10, 10, Color.BLUE));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).hasSize(2);
    }

    @Test
    void multiple_layers() {
        Context ctx = new Context(10, 10, new double[] {0, 10}, new double[] {0, 10}, Marker.DOT);

        ctx.draw(new Line(0, 0, 5, 5, Color.RED));
        ctx.layer();
        ctx.draw(new Line(5, 0, 5, 10, Color.BLUE));
        ctx.layer();
        ctx.draw(new Line(0, 5, 10, 5, Color.GREEN));

        List<Color[][]> layers = ctx.allLayers();
        assertThat(layers).hasSize(3);
    }
}

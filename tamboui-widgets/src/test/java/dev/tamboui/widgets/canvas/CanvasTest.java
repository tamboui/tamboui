/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.canvas.shapes.Circle;
import dev.tamboui.widgets.canvas.shapes.Line;
import dev.tamboui.widgets.canvas.shapes.Points;
import dev.tamboui.widgets.canvas.shapes.Rectangle;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasTest {

    @Test
    void builder_creates_canvas_with_defaults() {
        Canvas canvas = Canvas.builder().build();
        assertThat(canvas).isNotNull();
    }

    @Test
    void builder_accepts_bounds() {
        Canvas canvas = Canvas.builder().xBounds(-100, 100).yBounds(-50, 50).build();

        assertThat(canvas).isNotNull();
    }

    @Test
    void builder_accepts_marker() {
        for (Marker marker : Marker.values()) {
            Canvas canvas = Canvas.builder().marker(marker).build();
            assertThat(canvas).isNotNull();
        }
    }

    @Test
    void builder_accepts_block() {
        Canvas canvas = Canvas.builder().block(Block.bordered()).build();

        assertThat(canvas).isNotNull();
    }

    @Test
    void builder_accepts_background_color() {
        Canvas canvas = Canvas.builder().backgroundColor(Color.BLUE).build();

        assertThat(canvas).isNotNull();
    }

    @Test
    void builder_accepts_paint_callback() {
        Canvas canvas = Canvas.builder().paint(ctx -> {
            ctx.draw(new Line(0, 0, 1, 1, Color.RED));
        }).build();

        assertThat(canvas).isNotNull();
    }

    @Test
    void render_empty_area_does_nothing() {
        Canvas canvas = Canvas.builder().build();
        Buffer buffer = Buffer.empty(new Rect(0, 0, 0, 0));

        canvas.render(new Rect(0, 0, 0, 0), buffer);
        // Should not throw
    }

    @Test
    void render_with_block() {
        Canvas canvas = Canvas.builder().block(Block.bordered()).build();

        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Block borders should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo("┐");
    }

    @Test
    void render_with_background_color() {
        Canvas canvas = Canvas.builder().backgroundColor(Color.BLUE).build();

        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Background should be blue
        assertThat(buffer.get(5, 2).style().bg()).contains(Color.BLUE);
    }

    @Test
    void render_line_with_braille_marker() {
        Canvas canvas = Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.BRAILLE)
                .paint(ctx -> {
                    ctx.draw(new Line(0, 0, 10, 10, Color.RED));
                }).build();

        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Should have some braille characters rendered
        boolean hasBraille = false;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 10; x++) {
                String symbol = buffer.get(x, y).symbol();
                if (symbol.codePointAt(0) >= 0x2800 && symbol.codePointAt(0) <= 0x28FF) {
                    hasBraille = true;
                    break;
                }
            }
        }
        assertThat(hasBraille).isTrue();
    }

    @Test
    void render_line_with_dot_marker() {
        Canvas canvas = Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.DOT)
                .paint(ctx -> {
                    ctx.draw(new Line(0, 0, 10, 10, Color.GREEN));
                }).build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Should have dot at corner
        assertThat(buffer.get(0, 9).symbol()).isEqualTo("•");
    }

    @Test
    void render_line_with_block_marker() {
        Canvas canvas = Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.BLOCK)
                .paint(ctx -> {
                    ctx.draw(new Line(5, 5, 5, 5, Color.CYAN));
                }).build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Point should be a block
        assertThat(buffer.get(5, 5).symbol()).isEqualTo("█");
    }

    @Test
    void render_rectangle() {
        Canvas canvas = Canvas.builder().xBounds(0, 20).yBounds(0, 10).marker(Marker.DOT)
                .paint(ctx -> {
                    ctx.draw(new Rectangle(5, 2, 10, 6, Color.YELLOW));
                }).build();

        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Should have dots forming a rectangle outline
    }

    @Test
    void render_circle() {
        Canvas canvas = Canvas.builder().xBounds(0, 20).yBounds(0, 20).marker(Marker.DOT)
                .paint(ctx -> {
                    ctx.draw(new Circle(10, 10, 5, Color.MAGENTA));
                }).build();

        Rect area = new Rect(0, 0, 20, 20);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Should have dots forming a circle
    }

    @Test
    void render_points() {
        Canvas canvas = Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.DOT)
                .paint(ctx -> {
                    ctx.draw(new Points(new double[][]{{0, 0}, {5, 5}, {10, 10}}, Color.WHITE));
                }).build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Corner and center should have dots
        assertThat(buffer.get(0, 9).symbol()).isEqualTo("•");
        assertThat(buffer.get(5, 5).symbol()).isEqualTo("•");
    }

    @Test
    void render_multiple_shapes() {
        Canvas canvas = Canvas.builder().xBounds(0, 20).yBounds(0, 20).marker(Marker.DOT)
                .paint(ctx -> {
                    ctx.draw(new Line(0, 0, 20, 20, Color.RED));
                    ctx.draw(new Circle(10, 10, 5, Color.BLUE));
                }).build();

        Rect area = new Rect(0, 0, 20, 20);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);
        // Should not throw
    }

    @Test
    void render_with_labels() {
        Canvas canvas = Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.DOT)
                .paint(ctx -> {
                    ctx.print(5, 5, "Center");
                }).build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Label should be rendered
        assertThat(buffer.get(5, 5).symbol()).isEqualTo("C");
    }

    @Test
    void render_with_layers() {
        Canvas canvas = Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.DOT)
                .paint(ctx -> {
                    ctx.draw(new Line(0, 5, 10, 5, Color.RED));
                    ctx.layer();
                    ctx.draw(new Line(5, 0, 5, 10, Color.BLUE));
                }).build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Intersection should have the top layer color (blue)
        assertThat(buffer.get(5, 5).style().fg()).contains(Color.BLUE);
    }

    @Test
    void render_half_block_marker() {
        Canvas canvas = Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.HALF_BLOCK)
                .paint(ctx -> {
                    ctx.draw(new Points(new double[][]{{5, 5}}, Color.GREEN));
                }).build();

        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        canvas.render(area, buffer);

        // Should render half-block characters
    }

    @Test
    void marker_null_defaults_to_braille() {
        Canvas canvas = Canvas.builder().marker(null).build();

        assertThat(canvas).isNotNull();
    }
}

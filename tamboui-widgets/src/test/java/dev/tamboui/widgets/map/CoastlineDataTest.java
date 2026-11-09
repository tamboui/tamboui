/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Marker;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoastlineDataTest {

    @Test
    void ofCopiesAndValidates() {
        double[][] lines = {{0, 0, 10, 10}};
        CoastlineData data = CoastlineData.of(lines);
        lines[0][0] = 99;
        assertEquals(0, data.lines()[0][0], "of copies the input");
        assertThrows(IllegalArgumentException.class, () -> CoastlineData.of(new double[][] {{0, 0, 10}}));
        assertThrows(IllegalArgumentException.class, () -> CoastlineData.of(new double[][] {{200, 0}}));
        assertThrows(NullPointerException.class, () -> CoastlineData.of(null));
    }

    @Test
    void loadRoundTripsTheBinaryFormat() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(1);            // one line
            out.writeInt(2);            // two points
            out.writeShort(1000);       // 10.00 E
            out.writeShort(5500);       // 55.00 N
            out.writeShort(1200);       // 12.00 E
            out.writeShort(5600);       // 56.00 N
        }
        CoastlineData data = CoastlineData.load(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(1, data.lines().length);
        assertArrayEquals(new double[] {10.0, 55.0, 12.0, 56.0}, data.lines()[0], 1e-9);
    }

    @Test
    void customDataRendersThroughCoastlineShape() {
        Rect area = new Rect(0, 0, 40, 12);
        // A custom coastline diagonal across the viewport should paint cells.
        CoastlineData data = CoastlineData.of(new double[][] {{-10, 40, 10, 60}});
        Buffer buffer = Buffer.empty(area);
        Canvas.builder().xBounds(-10, 10).yBounds(40, 60).marker(Marker.BRAILLE)
                .paint(ctx -> ctx.draw(new CoastlineShape(data, Color.CYAN))).build().render(area, buffer);
        int painted = 0;
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                if (!buffer.get(x, y).symbol().equals(" ")) {
                    painted++;
                }
            }
        }
        assertTrue(painted > 3, "custom coastline data should render");
    }
}

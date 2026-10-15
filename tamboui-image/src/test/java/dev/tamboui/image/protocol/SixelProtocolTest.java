/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.capability.TerminalImageProtocol;
import dev.tamboui.layout.Rect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SixelProtocolTest {

    @Test
    void requiresRawOutput_returns_true() {
        SixelProtocol protocol = new SixelProtocol();
        assertThat(protocol.requiresRawOutput()).isTrue();
    }

    @Test
    void handlesOwnScaling_returns_false() {
        SixelProtocol protocol = new SixelProtocol();
        assertThat(protocol.handlesOwnScaling()).isFalse();
    }

    @Test
    void name_returns_sixel() {
        SixelProtocol protocol = new SixelProtocol();
        assertThat(protocol.name()).isEqualTo("Sixel");
    }

    @Test
    void protocolType_returns_sixel() {
        SixelProtocol protocol = new SixelProtocol();
        assertThat(protocol.protocolType()).isEqualTo(TerminalImageProtocol.SIXEL);
    }

    @Test
    void resolution_returns_8x16() {
        SixelProtocol protocol = new SixelProtocol();
        ImageProtocol.Resolution res = protocol.resolution();
        assertThat(res.widthMultiplier()).isEqualTo(8);
        assertThat(res.heightMultiplier()).isEqualTo(16);
    }

    @Test
    void render_throws_when_rawOutput_null() {
        SixelProtocol protocol = new SixelProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        assertThatThrownBy(() -> protocol.render(image, area, buffer, null))
            .isInstanceOf(RuntimeIOException.class)
            .hasMessageContaining("Sixel protocol requires raw output stream");
    }

    @Test
    void render_empty_area_does_nothing() throws IOException {
        SixelProtocol protocol = new SixelProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(area);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        protocol.render(image, area, buffer, out);
        assertThat(out.size()).isEqualTo(0);
    }

    @Test
    void render_marks_covered_cells_in_buffer_and_emits_sixel() throws IOException {
        SixelProtocol protocol = new SixelProtocol();
        // 16x32 pixels = 2 columns x 2 rows in 8x16 cell resolution
        ImageData image = createSolidImage(16, 32, 0xFFFF0000);
        Rect area = new Rect(1, 1, 5, 5);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 8, 8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        protocol.render(image, area, buffer, out);

        String output = new String(out.toByteArray(), StandardCharsets.US_ASCII);
        // Verify cursor move and Sixel DCS sequence
        assertThat(output).contains("\033[2;2H");
        assertThat(output).contains("\033P");
        assertThat(output).contains("\033\\");

        // Covered cells (cols 1..2, rows 1..2) must be CONTINUATION
        for (int y = 1; y <= 2; y++) {
            for (int x = 1; x <= 2; x++) {
                assertThat(buffer.get(x, y)).isEqualTo(Cell.CONTINUATION);
            }
        }

        // Uncovered cells in the remaining area (e.g. col 3, row 3) must remain EMPTY
        assertThat(buffer.get(3, 1)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(1, 3)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(0, 0)).isEqualTo(Cell.EMPTY);
    }

    private ImageData createSolidImage(int width, int height, int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return ImageData.fromBufferedImage(image);
    }
}

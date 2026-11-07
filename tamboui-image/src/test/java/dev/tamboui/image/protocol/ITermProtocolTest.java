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

class ITermProtocolTest {

    @Test
    void requiresRawOutput_returns_true() {
        ITermProtocol protocol = new ITermProtocol();
        assertThat(protocol.requiresRawOutput()).isTrue();
    }

    @Test
    void handlesOwnScaling_returns_true() {
        ITermProtocol protocol = new ITermProtocol();
        assertThat(protocol.handlesOwnScaling()).isTrue();
    }

    @Test
    void name_returns_iterm2() {
        ITermProtocol protocol = new ITermProtocol();
        assertThat(protocol.name()).isEqualTo("iTerm2");
    }

    @Test
    void protocolType_returns_iterm2() {
        ITermProtocol protocol = new ITermProtocol();
        assertThat(protocol.protocolType()).isEqualTo(TerminalImageProtocol.ITERM2);
    }

    @Test
    void resolution_returns_8x16() {
        ITermProtocol protocol = new ITermProtocol();
        ImageProtocol.Resolution res = protocol.resolution();
        assertThat(res.widthMultiplier()).isEqualTo(8);
        assertThat(res.heightMultiplier()).isEqualTo(16);
    }

    @Test
    void render_throws_when_rawOutput_null() {
        ITermProtocol protocol = new ITermProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        assertThatThrownBy(() -> protocol.render(image, area, buffer, null))
            .isInstanceOf(RuntimeIOException.class)
            .hasMessageContaining("iTerm2 protocol requires raw output stream");
    }

    @Test
    void render_empty_area_does_nothing() throws IOException {
        ITermProtocol protocol = new ITermProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(area);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        protocol.render(image, area, buffer, out);
        assertThat(out.size()).isEqualTo(0);
    }

    @Test
    void render_marks_covered_cells_in_buffer_and_emits_osc() throws IOException {
        ITermProtocol protocol = new ITermProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(2, 1, 6, 4);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        protocol.render(image, area, buffer, out);

        String output = new String(out.toByteArray(), StandardCharsets.US_ASCII);
        // Verify cursor positioning and iTerm2 OSC header
        assertThat(output).contains("\033[2;3H");
        assertThat(output).contains("\033]1337;File=inline=1;width=6;height=4;preserveAspectRatio=0:");

        // Verify covered cells in buffer are marked as CONTINUATION
        for (int y = 1; y < 5; y++) {
            for (int x = 2; x < 8; x++) {
                assertThat(buffer.get(x, y)).isEqualTo(Cell.CONTINUATION);
            }
        }
        // Verify uncovered cells remain EMPTY
        assertThat(buffer.get(0, 0)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(1, 1)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(8, 1)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(2, 5)).isEqualTo(Cell.EMPTY);
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

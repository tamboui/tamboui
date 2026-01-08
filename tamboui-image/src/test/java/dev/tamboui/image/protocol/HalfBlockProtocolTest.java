/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.image.ImageData;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class HalfBlockProtocolTest {

    @Test
    void requiresRawOutput_returns_false() {
        HalfBlockProtocol protocol = new HalfBlockProtocol();
        assertThat(protocol.requiresRawOutput()).isFalse();
    }

    @Test
    void resolution_returns_1x2() {
        HalfBlockProtocol protocol = new HalfBlockProtocol();
        ImageProtocol.Resolution res = protocol.resolution();

        assertThat(res.widthMultiplier()).isEqualTo(1);
        assertThat(res.heightMultiplier()).isEqualTo(2);
    }

    @Test
    void name_returns_half_block() {
        HalfBlockProtocol protocol = new HalfBlockProtocol();
        assertThat(protocol.name()).isEqualTo("Half-Block");
    }

    @Test
    void render_empty_area_does_nothing() throws IOException {
        HalfBlockProtocol protocol = new HalfBlockProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);
        // Should not throw
    }

    @Test
    void render_solid_color_fills_with_upper_half_blocks() throws IOException {
        HalfBlockProtocol protocol = new HalfBlockProtocol();
        // Image with height = 4 (2 rows after 2x vertical resolution)
        ImageData image = createSolidImage(5, 4, 0xFFFF0000);
        Rect area = new Rect(0, 0, 5, 2);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // All cells should have upper half block with red FG and red BG
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 5; x++) {
                assertThat(buffer.get(x, y).symbol()).isEqualTo("▀");
            }
        }
    }

    @Test
    void render_two_colors_uses_fg_and_bg() throws IOException {
        HalfBlockProtocol protocol = new HalfBlockProtocol();

        // Create image with red top row and green bottom row (each row = 1 virtual pixel)
        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFFFF0000); // Red top-left
        source.setRGB(1, 0, 0xFFFF0000); // Red top-right
        source.setRGB(0, 1, 0xFF00FF00); // Green bottom-left
        source.setRGB(1, 1, 0xFF00FF00); // Green bottom-right

        ImageData image = ImageData.fromBufferedImage(source);
        Rect area = new Rect(0, 0, 2, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // Each cell should have red FG (top) and green BG (bottom)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("▀");
        assertThat(buffer.get(0, 0).style().fg()).isPresent();
        assertThat(buffer.get(0, 0).style().bg()).isPresent();
    }

    @Test
    void render_top_only_uses_upper_half_block() throws IOException {
        HalfBlockProtocol protocol = new HalfBlockProtocol();

        // Create image with visible top row and transparent bottom row
        BufferedImage source = new BufferedImage(1, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFFFF0000); // Red, opaque
        source.setRGB(0, 1, 0x00000000); // Transparent

        ImageData image = ImageData.fromBufferedImage(source);
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("▀");
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.rgb(255, 0, 0));
        assertThat(buffer.get(0, 0).style().bg()).isEmpty();
    }

    @Test
    void render_bottom_only_uses_lower_half_block() throws IOException {
        HalfBlockProtocol protocol = new HalfBlockProtocol();

        // Create image with transparent top row and visible bottom row
        BufferedImage source = new BufferedImage(1, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0x00000000); // Transparent
        source.setRGB(0, 1, 0xFF00FF00); // Green, opaque

        ImageData image = ImageData.fromBufferedImage(source);
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("▄");
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.rgb(0, 255, 0));
    }

    @Test
    void render_transparent_pixel_is_skipped() throws IOException {
        HalfBlockProtocol protocol = new HalfBlockProtocol();

        BufferedImage source = new BufferedImage(1, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0x00000000); // Transparent
        source.setRGB(0, 1, 0x00000000); // Transparent

        ImageData image = ImageData.fromBufferedImage(source);
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // Cell should remain unchanged (empty)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
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

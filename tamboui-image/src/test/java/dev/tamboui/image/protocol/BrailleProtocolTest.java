/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.image.ImageData;
import dev.tamboui.layout.Rect;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class BrailleProtocolTest {

    private static final int BRAILLE_BASE = 0x2800;

    @Test
    void requiresRawOutput_returns_false() {
        BrailleProtocol protocol = new BrailleProtocol();
        assertThat(protocol.requiresRawOutput()).isFalse();
    }

    @Test
    void resolution_returns_2x4() {
        BrailleProtocol protocol = new BrailleProtocol();
        ImageProtocol.Resolution res = protocol.resolution();

        assertThat(res.widthMultiplier()).isEqualTo(2);
        assertThat(res.heightMultiplier()).isEqualTo(4);
    }

    @Test
    void name_returns_braille() {
        BrailleProtocol protocol = new BrailleProtocol();
        assertThat(protocol.name()).isEqualTo("Braille");
    }

    @Test
    void render_empty_area_does_nothing() throws IOException {
        BrailleProtocol protocol = new BrailleProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFFFFFF);
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);
        // Should not throw
    }

    @Test
    void render_all_dots_on_creates_full_braille() throws IOException {
        BrailleProtocol protocol = new BrailleProtocol();
        // Create white image (all dots on with default threshold)
        ImageData image = createSolidImage(2, 4, 0xFFFFFFFF);
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // All 8 dots should be on: 0x01+0x02+0x04+0x08+0x10+0x20+0x40+0x80 = 0xFF
        String symbol = buffer.get(0, 0).symbol();
        assertThat(symbol.codePointAt(0)).isEqualTo(BRAILLE_BASE + 0xFF);
    }

    @Test
    void render_single_dot_creates_correct_pattern() throws IOException {
        BrailleProtocol protocol = new BrailleProtocol();

        // Create image with only top-left dot visible (white)
        BufferedImage source = new BufferedImage(2, 4, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFFFFFFFF); // White (on)
        // Rest are black (off with default threshold)

        ImageData image = ImageData.fromBufferedImage(source);
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // Only dot 1 (top-left) = bit 0x01
        String symbol = buffer.get(0, 0).symbol();
        assertThat(symbol.codePointAt(0)).isEqualTo(BRAILLE_BASE + 0x01);
    }

    @Test
    void render_transparent_pixels_are_off() throws IOException {
        BrailleProtocol protocol = new BrailleProtocol();

        BufferedImage source = new BufferedImage(2, 4, BufferedImage.TYPE_INT_ARGB);
        // All transparent
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 2; x++) {
                source.setRGB(x, y, 0x00000000);
            }
        }

        ImageData image = ImageData.fromBufferedImage(source);
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // Cell should remain unchanged (empty)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    void render_solid_color_shows_all_dots() throws IOException {
        BrailleProtocol protocol = new BrailleProtocol();

        // With percentile-based thresholding, a solid color image (all same luminance)
        // will show all dots since all pixels are >= the percentile threshold
        ImageData image = createSolidImage(2, 4, 0xFF000000); // Black
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // All dots should be on (percentile threshold = 0 for solid black)
        String symbol = buffer.get(0, 0).symbol();
        assertThat(symbol.codePointAt(0)).isEqualTo(BRAILLE_BASE + 0xFF);
    }

    @Test
    void render_with_high_percentile_shows_fewer_dots() throws IOException {
        // With 90th percentile, only the brightest ~10% of pixels are shown
        BrailleProtocol protocol = new BrailleProtocol(90);

        // Create image with gradient
        BufferedImage source = new BufferedImage(2, 4, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFFFFFFFF); // White (brightest) - luminance 255
        source.setRGB(1, 0, 0xFF808080); // Gray - luminance 128
        source.setRGB(0, 1, 0xFF404040); // Dark gray - luminance 64
        source.setRGB(1, 1, 0xFF202020); // Darker gray - luminance 32
        source.setRGB(0, 2, 0xFF101010); // Very dark - luminance 16
        source.setRGB(1, 2, 0xFF080808); // Almost black - luminance 8
        source.setRGB(0, 3, 0xFF040404); // Nearly black - luminance 4
        source.setRGB(1, 3, 0xFF000000); // Black - luminance 0

        ImageData image = ImageData.fromBufferedImage(source);
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // With 90th percentile threshold, pixels >= 128 luminance are on
        // That includes white (255) at (0,0) and gray (128) at (1,0)
        String symbol = buffer.get(0, 0).symbol();
        assertThat(symbol.codePointAt(0)).isEqualTo(BRAILLE_BASE + 0x09); // Dots 1 and 4 (top row)
    }

    @Test
    void render_averages_colors() throws IOException {
        BrailleProtocol protocol = new BrailleProtocol();

        // Create image with different colors for each dot
        BufferedImage source = new BufferedImage(2, 4, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFFFF0000); // Red
        source.setRGB(1, 0, 0xFF00FF00); // Green
        source.setRGB(0, 1, 0xFF0000FF); // Blue
        source.setRGB(1, 1, 0xFFFFFFFF); // White
        source.setRGB(0, 2, 0xFFFFFF00); // Yellow
        source.setRGB(1, 2, 0xFF00FFFF); // Cyan
        source.setRGB(0, 3, 0xFFFF00FF); // Magenta
        source.setRGB(1, 3, 0xFFFFFFFF); // White

        ImageData image = ImageData.fromBufferedImage(source);
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // Should render with averaged color
        assertThat(buffer.get(0, 0).style().fg()).isPresent();
    }

    @Test
    void render_multiple_cells() throws IOException {
        BrailleProtocol protocol = new BrailleProtocol();
        // Create 4x8 image (2x2 cells)
        ImageData image = createSolidImage(4, 8, 0xFFFFFFFF);
        Rect area = new Rect(0, 0, 2, 2);
        Buffer buffer = Buffer.empty(area);

        protocol.render(image, area, buffer, null);

        // All 4 cells should have braille patterns
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                String symbol = buffer.get(x, y).symbol();
                assertThat(symbol.codePointAt(0)).isBetween(BRAILLE_BASE, BRAILLE_BASE + 0xFF);
            }
        }
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

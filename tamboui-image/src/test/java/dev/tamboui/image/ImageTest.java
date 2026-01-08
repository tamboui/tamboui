/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.image.protocol.BrailleProtocol;
import dev.tamboui.image.protocol.HalfBlockProtocol;
import dev.tamboui.layout.Rect;
import dev.tamboui.widgets.block.Block;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTest {

    @Test
    void builder_creates_image() {
        ImageData data = createTestImage(10, 10, 0xFFFF0000);
        Image image = Image.builder()
            .data(data)
            .build();

        assertThat(image).isNotNull();
        assertThat(image.data()).isSameAs(data);
    }

    @Test
    void of_creates_image_from_data() {
        ImageData data = createTestImage(10, 10, 0xFFFF0000);
        Image image = Image.of(data);

        assertThat(image.data()).isSameAs(data);
        assertThat(image.scaling()).isEqualTo(ImageScaling.FIT);
    }

    @Test
    void builder_defaults_to_fit_scaling() {
        Image image = Image.builder()
            .data(createTestImage(10, 10, 0xFFFF0000))
            .build();

        assertThat(image.scaling()).isEqualTo(ImageScaling.FIT);
    }

    @Test
    void builder_accepts_scaling() {
        Image image = Image.builder()
            .data(createTestImage(10, 10, 0xFFFF0000))
            .scaling(ImageScaling.STRETCH)
            .build();

        assertThat(image.scaling()).isEqualTo(ImageScaling.STRETCH);
    }

    @Test
    void builder_accepts_null_scaling_defaults_to_fit() {
        Image image = Image.builder()
            .data(createTestImage(10, 10, 0xFFFF0000))
            .scaling(null)
            .build();

        assertThat(image.scaling()).isEqualTo(ImageScaling.FIT);
    }

    @Test
    void builder_accepts_block() {
        Block block = Block.bordered();
        Image image = Image.builder()
            .data(createTestImage(10, 10, 0xFFFF0000))
            .block(block)
            .build();

        assertThat(image.block()).isSameAs(block);
    }

    @Test
    void builder_accepts_protocol() {
        HalfBlockProtocol protocol = new HalfBlockProtocol();
        Image image = Image.builder()
            .data(createTestImage(10, 10, 0xFFFF0000))
            .protocol(protocol)
            .build();

        assertThat(image.protocol()).isSameAs(protocol);
    }

    @Test
    void render_empty_area_does_nothing() {
        Image image = Image.builder()
            .data(createTestImage(10, 10, 0xFFFF0000))
            .build();

        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);
        // Should not throw
    }

    @Test
    void render_null_data_does_nothing() {
        Image image = Image.builder().build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);
        // Should not throw
    }

    @Test
    void render_with_half_block_protocol() {
        Image image = Image.builder()
            .data(createTestImage(10, 10, 0xFFFF0000))
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        // Should have rendered half-blocks
        boolean hasHalfBlock = false;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                String symbol = buffer.get(x, y).symbol();
                if ("▀".equals(symbol) || "▄".equals(symbol) || "█".equals(symbol)) {
                    hasHalfBlock = true;
                    break;
                }
            }
        }
        assertThat(hasHalfBlock).isTrue();
    }

    @Test
    void render_with_braille_protocol() {
        Image image = Image.builder()
            .data(createTestImage(10, 10, 0xFFFFFFFF))
            .protocol(new BrailleProtocol())
            .build();

        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        // Should have rendered braille patterns
        boolean hasBraille = false;
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                String symbol = buffer.get(x, y).symbol();
                int codePoint = symbol.codePointAt(0);
                if (codePoint >= 0x2800 && codePoint <= 0x28FF) {
                    hasBraille = true;
                    break;
                }
            }
        }
        assertThat(hasBraille).isTrue();
    }

    @Test
    void render_with_block_renders_border() {
        Image image = Image.builder()
            .data(createTestImage(10, 10, 0xFFFF0000))
            .block(Block.bordered())
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        // Block borders should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("┐");
        assertThat(buffer.get(0, 9).symbol()).isEqualTo("└");
        assertThat(buffer.get(9, 9).symbol()).isEqualTo("┘");
    }

    @Test
    void render_fit_scaling_maintains_aspect_ratio() {
        // Create wide image (20x10) to render in square area (10x10)
        Image image = Image.builder()
            .data(createTestImage(20, 10, 0xFFFF0000))
            .scaling(ImageScaling.FIT)
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);
        // Should render without error
    }

    @Test
    void render_stretch_scaling_fills_area() {
        Image image = Image.builder()
            .data(createTestImage(5, 5, 0xFFFF0000))
            .scaling(ImageScaling.STRETCH)
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);
        // Should render without error
    }

    @Test
    void render_none_scaling_preserves_original_size() {
        Image image = Image.builder()
            .data(createTestImage(5, 5, 0xFFFF0000))
            .scaling(ImageScaling.NONE)
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);
        // Should render without error
    }

    @Test
    void render_fill_scaling_covers_area() {
        Image image = Image.builder()
            .data(createTestImage(20, 10, 0xFFFF0000))
            .scaling(ImageScaling.FILL)
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 10);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);
        // Should render without error
    }

    private ImageData createTestImage(int width, int height, int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return ImageData.fromBufferedImage(image);
    }
}

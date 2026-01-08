/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageDataTest {

    @Test
    void fromBufferedImage_creates_image_data() {
        BufferedImage source = createTestImage(10, 10, 0xFFFF0000);
        ImageData data = ImageData.fromBufferedImage(source);

        assertThat(data.width()).isEqualTo(10);
        assertThat(data.height()).isEqualTo(10);
    }

    @Test
    void pixelAt_returns_correct_value() {
        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0xFFFF0000); // Red
        source.setRGB(1, 0, 0xFF00FF00); // Green
        source.setRGB(0, 1, 0xFF0000FF); // Blue
        source.setRGB(1, 1, 0xFFFFFFFF); // White

        ImageData data = ImageData.fromBufferedImage(source);

        assertThat(data.pixelAt(0, 0)).isEqualTo(0xFFFF0000);
        assertThat(data.pixelAt(1, 0)).isEqualTo(0xFF00FF00);
        assertThat(data.pixelAt(0, 1)).isEqualTo(0xFF0000FF);
        assertThat(data.pixelAt(1, 1)).isEqualTo(0xFFFFFFFF);
    }

    @Test
    void pixelAt_throws_for_invalid_coordinates() {
        ImageData data = ImageData.fromBufferedImage(createTestImage(5, 5, 0xFF000000));

        assertThatThrownBy(() -> data.pixelAt(-1, 0))
            .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> data.pixelAt(5, 0))
            .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> data.pixelAt(0, -1))
            .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> data.pixelAt(0, 5))
            .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void alpha_extracts_alpha_component() {
        assertThat(ImageData.alpha(0xFF123456)).isEqualTo(0xFF);
        assertThat(ImageData.alpha(0x80123456)).isEqualTo(0x80);
        assertThat(ImageData.alpha(0x00123456)).isEqualTo(0x00);
    }

    @Test
    void red_extracts_red_component() {
        assertThat(ImageData.red(0xFFFF0000)).isEqualTo(0xFF);
        assertThat(ImageData.red(0xFF800000)).isEqualTo(0x80);
        assertThat(ImageData.red(0xFF00FFFF)).isEqualTo(0x00);
    }

    @Test
    void green_extracts_green_component() {
        assertThat(ImageData.green(0xFF00FF00)).isEqualTo(0xFF);
        assertThat(ImageData.green(0xFF008000)).isEqualTo(0x80);
        assertThat(ImageData.green(0xFFFF00FF)).isEqualTo(0x00);
    }

    @Test
    void blue_extracts_blue_component() {
        assertThat(ImageData.blue(0xFF0000FF)).isEqualTo(0xFF);
        assertThat(ImageData.blue(0xFF000080)).isEqualTo(0x80);
        assertThat(ImageData.blue(0xFFFFFF00)).isEqualTo(0x00);
    }

    @Test
    void isVisible_returns_true_for_opaque_pixels() {
        assertThat(ImageData.isVisible(0xFFFF0000)).isTrue();
        assertThat(ImageData.isVisible(0x01000000)).isTrue();
    }

    @Test
    void isVisible_returns_false_for_transparent_pixels() {
        assertThat(ImageData.isVisible(0x00FF0000)).isFalse();
        assertThat(ImageData.isVisible(0x00000000)).isFalse();
    }

    @Test
    void resize_scales_image() {
        ImageData data = ImageData.fromBufferedImage(createTestImage(10, 10, 0xFFFF0000));
        ImageData resized = data.resize(20, 20);

        assertThat(resized.width()).isEqualTo(20);
        assertThat(resized.height()).isEqualTo(20);
    }

    @Test
    void resize_returns_same_instance_if_dimensions_match() {
        ImageData data = ImageData.fromBufferedImage(createTestImage(10, 10, 0xFFFF0000));
        ImageData resized = data.resize(10, 10);

        assertThat(resized).isSameAs(data);
    }

    @Test
    void resize_throws_for_invalid_dimensions() {
        ImageData data = ImageData.fromBufferedImage(createTestImage(10, 10, 0xFFFF0000));

        assertThatThrownBy(() -> data.resize(0, 10))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> data.resize(10, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> data.resize(-1, 10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void crop_extracts_region() {
        BufferedImage source = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        // Create a pattern: red in top-left quadrant
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                int color = (x < 5 && y < 5) ? 0xFFFF0000 : 0xFF00FF00;
                source.setRGB(x, y, color);
            }
        }

        ImageData data = ImageData.fromBufferedImage(source);
        ImageData cropped = data.crop(0, 0, 5, 5);

        assertThat(cropped.width()).isEqualTo(5);
        assertThat(cropped.height()).isEqualTo(5);
        // All pixels should be red
        assertThat(cropped.pixelAt(0, 0)).isEqualTo(0xFFFF0000);
        assertThat(cropped.pixelAt(4, 4)).isEqualTo(0xFFFF0000);
    }

    @Test
    void crop_throws_for_invalid_parameters() {
        ImageData data = ImageData.fromBufferedImage(createTestImage(10, 10, 0xFFFF0000));

        assertThatThrownBy(() -> data.crop(-1, 0, 5, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> data.crop(0, -1, 5, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> data.crop(0, 0, 0, 5))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> data.crop(0, 0, 5, 0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> data.crop(5, 5, 10, 10))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toBufferedImage_creates_correct_image() {
        BufferedImage source = createTestImage(5, 5, 0xFFABCDEF);
        ImageData data = ImageData.fromBufferedImage(source);
        BufferedImage result = data.toBufferedImage();

        assertThat(result.getWidth()).isEqualTo(5);
        assertThat(result.getHeight()).isEqualTo(5);
        assertThat(result.getRGB(2, 2)).isEqualTo(0xFFABCDEF);
    }

    @Test
    void toPng_creates_valid_png_bytes() throws Exception {
        ImageData data = ImageData.fromBufferedImage(createTestImage(10, 10, 0xFFFF0000));
        byte[] png = data.toPng();

        // PNG magic bytes: 137 80 78 71 13 10 26 10
        assertThat(png).hasSizeGreaterThan(8);
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
    }

    @Test
    void scaledDimensionsToFit_maintains_aspect_ratio() {
        ImageData data = ImageData.fromBufferedImage(createTestImage(200, 100, 0xFFFF0000));

        int[] dims = data.scaledDimensionsToFit(50, 50);
        assertThat(dims[0]).isEqualTo(50); // Width is limiting factor
        assertThat(dims[1]).isEqualTo(25); // Height scales proportionally
    }

    @Test
    void scaledDimensionsToFit_height_limiting() {
        ImageData data = ImageData.fromBufferedImage(createTestImage(100, 200, 0xFFFF0000));

        int[] dims = data.scaledDimensionsToFit(50, 50);
        assertThat(dims[0]).isEqualTo(25); // Width scales proportionally
        assertThat(dims[1]).isEqualTo(50); // Height is limiting factor
    }

    @Test
    void scaledDimensionsToFill_covers_area() {
        ImageData data = ImageData.fromBufferedImage(createTestImage(200, 100, 0xFFFF0000));

        int[] dims = data.scaledDimensionsToFill(50, 50);
        assertThat(dims[0]).isEqualTo(100); // Width overshoots
        assertThat(dims[1]).isEqualTo(50);  // Height exactly fills
    }

    @Test
    void fromBytes_loads_image_from_byte_array() throws Exception {
        ImageData original = ImageData.fromBufferedImage(createTestImage(5, 5, 0xFFFF0000));
        byte[] png = original.toPng();

        ImageData loaded = ImageData.fromBytes(png);
        assertThat(loaded.width()).isEqualTo(5);
        assertThat(loaded.height()).isEqualTo(5);
    }

    /**
     * Creates a test image filled with a single color.
     */
    private BufferedImage createTestImage(int width, int height, int argb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }
}

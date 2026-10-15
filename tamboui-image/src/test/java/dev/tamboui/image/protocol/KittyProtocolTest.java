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

class KittyProtocolTest {

    @Test
    void requiresRawOutput_returns_true() {
        KittyProtocol protocol = new KittyProtocol();
        assertThat(protocol.requiresRawOutput()).isTrue();
    }

    @Test
    void handlesOwnScaling_returns_true() {
        KittyProtocol protocol = new KittyProtocol();
        assertThat(protocol.handlesOwnScaling()).isTrue();
    }

    @Test
    void name_returns_kitty() {
        KittyProtocol protocol = new KittyProtocol();
        assertThat(protocol.name()).isEqualTo("Kitty");
    }

    @Test
    void protocolType_returns_kitty() {
        KittyProtocol protocol = new KittyProtocol();
        assertThat(protocol.protocolType()).isEqualTo(TerminalImageProtocol.KITTY);
    }

    @Test
    void resolution_returns_8x16() {
        KittyProtocol protocol = new KittyProtocol();
        ImageProtocol.Resolution res = protocol.resolution();
        assertThat(res.widthMultiplier()).isEqualTo(8);
        assertThat(res.heightMultiplier()).isEqualTo(16);
    }

    @Test
    void render_throws_when_rawOutput_null() {
        KittyProtocol protocol = new KittyProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        assertThatThrownBy(() -> protocol.render(image, area, buffer, null))
            .isInstanceOf(RuntimeIOException.class)
            .hasMessageContaining("Kitty protocol requires raw output stream");
    }

    @Test
    void render_empty_area_does_nothing() throws IOException {
        KittyProtocol protocol = new KittyProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(area);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        protocol.render(image, area, buffer, out);
        assertThat(out.size()).isEqualTo(0);
    }

    @Test
    void render_transmits_with_image_id_and_marks_buffer() throws IOException {
        KittyProtocol protocol = new KittyProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(1, 1, 4, 3);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 10));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        protocol.render(image, area, buffer, out);

        String output = new String(out.toByteArray(), StandardCharsets.US_ASCII);
        int imageId = protocol.lastImageId();
        assertThat(imageId).isGreaterThan(0);

        // Should include image id in control parameters and not emit delete on first render
        assertThat(output).doesNotContain("a=d");
        assertThat(output).contains("i=" + imageId);
        assertThat(output).contains("a=T,f=100,t=d,q=2,i=" + imageId + ",c=4,r=3");

        // Covered cells in buffer must be marked as CONTINUATION
        for (int y = 1; y < 4; y++) {
            for (int x = 1; x < 5; x++) {
                assertThat(buffer.get(x, y)).isEqualTo(Cell.CONTINUATION);
            }
        }
        // Uncovered cells must remain EMPTY
        assertThat(buffer.get(0, 0)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(5, 5)).isEqualTo(Cell.EMPTY);
    }

    @Test
    void render_consecutive_images_emits_delete_command_for_previous_id() throws IOException {
        KittyProtocol protocol = new KittyProtocol();
        ImageData image1 = createSolidImage(10, 10, 0xFFFF0000);
        ImageData image2 = createSolidImage(10, 10, 0xFF00FF00);
        Rect area1 = new Rect(0, 0, 8, 6);
        Rect area2 = new Rect(2, 2, 5, 4);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 10));

        // Render 1: first image
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        protocol.render(image1, area1, buffer, out1);
        int firstId = protocol.lastImageId();
        assertThat(firstId).isGreaterThan(0);

        String output1 = new String(out1.toByteArray(), StandardCharsets.US_ASCII);
        assertThat(output1).doesNotContain("a=d");
        assertThat(output1).contains("i=" + firstId);

        // Render 2: second image (replacement)
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        protocol.render(image2, area2, buffer, out2);
        int secondId = protocol.lastImageId();
        assertThat(secondId).isNotEqualTo(firstId);

        String output2 = new String(out2.toByteArray(), StandardCharsets.US_ASCII);
        // Must emit delete sequence for firstId with q=2 quiet flag before drawing second image
        String expectedDelete = "\033_Ga=d,d=i,i=" + firstId + ",q=2\033\\";
        assertThat(output2).startsWith(expectedDelete);
        assertThat(output2).contains("i=" + secondId);
    }

    @Test
    void deleteLast_emits_delete_and_resets_lastImageId() throws IOException {
        KittyProtocol protocol = new KittyProtocol();
        ImageData image = createSolidImage(10, 10, 0xFFFF0000);
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        protocol.render(image, area, buffer, out);
        int imageId = protocol.lastImageId();
        assertThat(imageId).isGreaterThan(0);

        ByteArrayOutputStream deleteOut = new ByteArrayOutputStream();
        protocol.deleteLast(deleteOut);

        String deleteOutput = new String(deleteOut.toByteArray(), StandardCharsets.US_ASCII);
        assertThat(deleteOutput).isEqualTo("\033_Ga=d,d=i,i=" + imageId + ",q=2\033\\");
        assertThat(protocol.lastImageId()).isEqualTo(0);

        // Subsequent deleteLast does nothing
        ByteArrayOutputStream secondDeleteOut = new ByteArrayOutputStream();
        protocol.deleteLast(secondDeleteOut);
        assertThat(secondDeleteOut.size()).isEqualTo(0);
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

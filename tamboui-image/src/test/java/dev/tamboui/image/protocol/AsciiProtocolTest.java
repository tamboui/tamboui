/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.capability.TerminalImageProtocol;
import dev.tamboui.layout.Rect;

import static org.assertj.core.api.Assertions.assertThat;

class AsciiProtocolTest {

    @Test
    void metadata_describes_a_one_by_one_ascii_protocol() {
        AsciiProtocol protocol = new AsciiProtocol();
        assertThat(protocol.requiresRawOutput()).isFalse();
        assertThat(protocol.name()).isEqualTo("ASCII");
        assertThat(protocol.protocolType()).isEqualTo(TerminalImageProtocol.ASCII);
        assertThat(protocol.resolution().widthMultiplier()).isEqualTo(1);
        assertThat(protocol.resolution().heightMultiplier()).isEqualTo(1);
    }

    @Test
    void handles_own_scaling_so_pixels_are_not_bilinear_averaged() {
        assertThat(new AsciiProtocol().handlesOwnScaling()).isTrue();
    }

    @Test
    void downscale_keeps_a_pixel_vivid_instead_of_blending_to_grey() throws IOException {
        // A bright pixel next to black: nearest-neighbour must keep it fully
        // saturated, not average it toward a dim grey.
        BufferedImage img = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xFFFF0000); // red
        img.setRGB(1, 0, 0xFF000000); // black
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        new AsciiProtocol().render(ImageData.fromBufferedImage(img), area, buffer, null);

        // Nearest-neighbour samples the first column (pure red), so the cell is a
        // visible, fully-saturated red glyph; bilinear averaging would have given
        // a dark grey-red that maps to a blank or muddy colour.
        assertThat(buffer.get(0, 0).symbol()).isNotEqualTo(" ");
        assertThat(buffer.get(0, 0).style().fg()).isPresent();
    }

    @Test
    void render_empty_area_does_nothing() throws IOException {
        new AsciiProtocol().render(solid(4, 4, 0xFFFFFFFF), new Rect(0, 0, 0, 0),
                Buffer.empty(new Rect(0, 0, 0, 0)), null);
        // Should not throw
    }

    @Test
    void bright_pixel_uses_a_dense_glyph_and_dark_pixel_is_blank() throws IOException {
        Rect area = new Rect(0, 0, 2, 1);
        BufferedImage img = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xFF000000); // black -> blank
        img.setRGB(1, 0, 0xFFFFFFFF); // white -> densest glyph '$'
        Buffer buffer = Buffer.empty(area);

        new AsciiProtocol().render(ImageData.fromBufferedImage(img), area, buffer, null);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("$");
    }

    @Test
    void colored_mode_sets_foreground_but_monochrome_does_not() throws IOException {
        Rect area = new Rect(0, 0, 1, 1);
        ImageData red = solid(1, 1, 0xFFFF0000);

        Buffer colored = Buffer.empty(area);
        new AsciiProtocol(true).render(red, area, colored, null);
        assertThat(colored.get(0, 0).style().fg()).isPresent();

        Buffer mono = Buffer.empty(area);
        new AsciiProtocol(false).render(red, area, mono, null);
        assertThat(mono.get(0, 0).style().fg()).isEmpty();
    }

    private static ImageData solid(int w, int h, int argb) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, argb);
            }
        }
        return ImageData.fromBufferedImage(img);
    }
}

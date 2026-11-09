/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.image.ImageData;
import dev.tamboui.layout.Rect;

import static org.assertj.core.api.Assertions.assertThat;

class KittyProtocolTest {

    private static final Pattern IMAGE_ID = Pattern.compile(",i=(\\d+),");

    @Test
    void transmits_raw_rgb_not_png() throws IOException {
        String out = render(new KittyProtocol(), image(2, 2), new Rect(0, 0, 4, 2));
        assertThat(out).contains("f=24");       // raw RGB
        assertThat(out).doesNotContain("f=100"); // not PNG
        assertThat(out).contains("s=2,v=2");     // pixel dimensions required for raw formats
    }

    @Test
    void reuses_one_image_id_across_frames_so_the_terminal_does_not_accumulate() throws IOException {
        KittyProtocol protocol = new KittyProtocol();
        ImageData frame = image(2, 2);
        Rect area = new Rect(0, 0, 4, 2);

        String first = render(protocol, frame, area);
        String second = render(protocol, frame, area);

        assertThat(imageId(first))
                .as("same instance must reuse its image id so each frame replaces the last")
                .isEqualTo(imageId(second));
        assertThat(first).contains("p=1");
    }

    @Test
    void separate_instances_use_separate_image_ids() throws IOException {
        String a = render(new KittyProtocol(), image(2, 2), new Rect(0, 0, 4, 2));
        String b = render(new KittyProtocol(), image(2, 2), new Rect(0, 0, 4, 2));
        assertThat(imageId(a)).isNotEqualTo(imageId(b));
    }

    private static String render(KittyProtocol protocol, ImageData image, Rect area) throws IOException {
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        protocol.render(image, area, Buffer.empty(area), raw);
        return new String(raw.toByteArray(), StandardCharsets.US_ASCII);
    }

    private static String imageId(String output) {
        Matcher m = IMAGE_ID.matcher(output);
        assertThat(m.find()).as("output must carry an image id").isTrue();
        return m.group(1);
    }

    private static ImageData image(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, 0xFF336699);
            }
        }
        return ImageData.fromBufferedImage(img);
    }
}

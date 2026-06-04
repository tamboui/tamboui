/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.image.ImageData;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.RawOutputContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the redraw-suppression behaviour shared by the native image protocols
 * (Kitty, iTerm2, Sixel).
 * <p>
 * The protocols emit their (potentially large) payload to the terminal on every render. Left
 * unchecked, that re-transmits the image on every frame of the render loop, which for iTerm2
 * grows the terminal process memory without bound. These tests pin the contract that an
 * unchanged image at an unchanged position is transmitted exactly once, while content,
 * position, or screen-generation changes force a re-transmission.
 *
 * @see NativeImageCache
 * @see RawOutputContext
 */
class NativeImageProtocolSkipTest {

    private static final Rect AREA_1 = new Rect(0, 0, 5, 3);
    private static final Rect AREA_2 = new Rect(2, 2, 5, 3);

    static Stream<Arguments> nativeProtocols() {
        return Stream.of(
            Arguments.of("Kitty", (Supplier<ImageProtocol>) KittyProtocol::new),
            Arguments.of("iTerm2", (Supplier<ImageProtocol>) ITermProtocol::new),
            Arguments.of("Sixel", (Supplier<ImageProtocol>) SixelProtocol::new)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nativeProtocols")
    void first_render_transmits(String name, Supplier<ImageProtocol> factory) throws IOException {
        ImageProtocol protocol = factory.get();
        ImageData image = solidImage(0xFFFF0000);
        GenerationOutput out = new GenerationOutput();

        render(protocol, image, AREA_1, out);

        assertThat(out.size()).as("first render must transmit the image").isGreaterThan(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nativeProtocols")
    void second_render_of_same_image_at_same_position_is_skipped(
            String name, Supplier<ImageProtocol> factory) throws IOException {
        ImageProtocol protocol = factory.get();
        ImageData image = solidImage(0xFFFF0000);
        GenerationOutput out = new GenerationOutput();

        render(protocol, image, AREA_1, out);
        out.reset();

        render(protocol, image, AREA_1, out);
        assertThat(out.size()).as("unchanged image must not be retransmitted").isZero();

        // And it stays skipped on further frames.
        render(protocol, image, AREA_1, out);
        assertThat(out.size()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nativeProtocols")
    void content_change_triggers_retransmission(
            String name, Supplier<ImageProtocol> factory) throws IOException {
        ImageProtocol protocol = factory.get();
        ImageData first = solidImage(0xFFFF0000);
        ImageData second = solidImage(0xFF00FF00);
        GenerationOutput out = new GenerationOutput();

        render(protocol, first, AREA_1, out);
        out.reset();

        render(protocol, second, AREA_1, out);
        assertThat(out.size()).as("a new image must be transmitted").isGreaterThan(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nativeProtocols")
    void position_change_triggers_retransmission(
            String name, Supplier<ImageProtocol> factory) throws IOException {
        ImageProtocol protocol = factory.get();
        ImageData image = solidImage(0xFFFF0000);
        GenerationOutput out = new GenerationOutput();

        render(protocol, image, AREA_1, out);
        out.reset();

        render(protocol, image, AREA_2, out);
        assertThat(out.size()).as("moving the image must transmit it at the new position").isGreaterThan(0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nativeProtocols")
    void generation_change_forces_redraw(
            String name, Supplier<ImageProtocol> factory) throws IOException {
        ImageProtocol protocol = factory.get();
        ImageData image = solidImage(0xFFFF0000);
        GenerationOutput out = new GenerationOutput();

        render(protocol, image, AREA_1, out);
        out.reset();

        // Same image, same position, but the screen was cleared (Terminal.clear()/resize()).
        out.generation++;
        render(protocol, image, AREA_1, out);
        assertThat(out.size())
            .as("a screen clear must force re-transmission even if image and position are unchanged")
            .isGreaterThan(0);

        // After the redraw it is stable again at the new generation.
        out.reset();
        render(protocol, image, AREA_1, out);
        assertThat(out.size()).isZero();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nativeProtocols")
    void returning_to_an_overwritten_position_retransmits(
            String name, Supplier<ImageProtocol> factory) throws IOException {
        ImageProtocol protocol = factory.get();
        ImageData image = solidImage(0xFFFF0000);
        GenerationOutput out = new GenerationOutput();

        // Show at AREA_1, then at the overlapping AREA_2 which paints over AREA_1's pixels.
        render(protocol, image, AREA_1, out);
        render(protocol, image, AREA_2, out);
        out.reset();

        // Returning to AREA_1 must retransmit — its pixels were overwritten by AREA_2.
        // Regression guard for the FIT -> FILL -> FIT scaling dance that wrongly skipped.
        render(protocol, image, AREA_1, out);
        assertThat(out.size())
            .as("returning to an overwritten position must retransmit")
            .isGreaterThan(0);
    }

    @Test
    void kitty_delete_commands_set_quiet_mode() throws IOException {
        // Without q=2 the terminal replies (e.g. ESC_Gi=1;OK ESC\) to graphics commands, and the
        // app reads that reply as input (stray digits in the focused field). Every Kitty command,
        // including the stale-image delete, must suppress the reply.
        KittyProtocol protocol = new KittyProtocol();
        ImageData image = solidImage(0xFFFF0000);
        GenerationOutput out = new GenerationOutput();

        render(protocol, image, AREA_1, out);
        out.reset();
        // Overlapping area -> deletes the previous image, then transmits the new one.
        render(protocol, image, AREA_2, out);

        boolean sawDelete = false;
        for (String command : out.text().split("\033_G")) {
            if (command.startsWith("a=d")) {
                sawDelete = true;
                assertThat(command).as("Kitty delete command must set q=2").contains("q=2");
            }
        }
        assertThat(sawDelete).as("a stale-image delete should have been emitted").isTrue();
    }

    @Test
    void plain_stream_without_context_still_skips_frame_to_frame() throws IOException {
        // A stream that does not expose a generation (e.g. a test or third-party stream) must
        // still benefit from frame-to-frame skipping; it simply cannot signal a screen clear.
        ImageProtocol protocol = new ITermProtocol();
        ImageData image = solidImage(0xFFFF0000);
        PlainOutput out = new PlainOutput();

        render(protocol, image, AREA_1, out);
        assertThat(out.size()).isGreaterThan(0);

        out.reset();
        render(protocol, image, AREA_1, out);
        assertThat(out.size()).isZero();
    }

    private static void render(ImageProtocol protocol, ImageData image, Rect area, OutputStream out)
            throws IOException {
        Buffer buffer = Buffer.empty(Rect.of(20, 20));
        protocol.render(image, area, buffer, out);
    }

    private static ImageData solidImage(int argb) {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return ImageData.fromBufferedImage(image);
    }

    /** Capturing output stream that exposes a controllable screen generation. */
    static final class GenerationOutput extends OutputStream implements RawOutputContext {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        long generation;

        @Override
        public long generation() {
            return generation;
        }

        @Override
        public void write(int b) {
            bytes.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            bytes.write(b, off, len);
        }

        int size() {
            return bytes.size();
        }

        String text() {
            return new String(bytes.toByteArray(), StandardCharsets.US_ASCII);
        }

        void reset() {
            bytes.reset();
        }
    }

    /** Capturing output stream that does NOT expose a generation. */
    static final class PlainOutput extends OutputStream {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            bytes.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            bytes.write(b, off, len);
        }

        int size() {
            return bytes.size();
        }

        void reset() {
            bytes.reset();
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.image.protocol.BrailleProtocol;
import dev.tamboui.image.protocol.HalfBlockProtocol;
import dev.tamboui.image.protocol.ITermProtocol;
import dev.tamboui.image.protocol.KittyProtocol;
import dev.tamboui.image.protocol.SixelProtocol;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.CompletedFrame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.widgets.block.Block;

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
        // 100x10 pixels fit into the 10x12 half-block pixels of a 10x6 area as 10x1 pixels
        Image image = Image.builder()
            .data(createTestImage(100, 10, 0xFFFF0000))
            .scaling(ImageScaling.FIT)
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 6);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        BufferAssertions.assertThat(buffer).hasContent(
            "          ",
            "          ",
            "▀▀▀▀▀▀▀▀▀▀",
            "          ",
            "          ",
            "          ");
    }

    @Test
    void render_fit_scaling_with_braille_maintains_aspect_ratio() {
        // 100x10 pixels fit into the 20x24 Braille dots of a 10x6 area as 20x2 dots
        Image image = Image.builder()
            .data(createTestImage(100, 10, 0xFFFFFFFF))
            .scaling(ImageScaling.FIT)
            .protocol(new BrailleProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 6);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        BufferAssertions.assertThat(buffer).hasContent(
            "          ",
            "          ",
            "⠛⠛⠛⠛⠛⠛⠛⠛⠛⠛",
            "          ",
            "          ",
            "          ");
    }

    @Test
    void render_stretch_scaling_fills_area() {
        Image image = Image.builder()
            .data(createTestImage(100, 10, 0xFFFF0000))
            .scaling(ImageScaling.STRETCH)
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 6);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        BufferAssertions.assertThat(buffer).hasContent(
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀");
    }

    @Test
    void render_stretch_scaling_with_braille_fills_area() {
        Image image = Image.builder()
            .data(createTestImage(100, 10, 0xFFFFFFFF))
            .scaling(ImageScaling.STRETCH)
            .protocol(new BrailleProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 6);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        BufferAssertions.assertThat(buffer).hasContent(
            "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
            "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
            "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
            "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
            "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
            "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿");
    }

    @Test
    void render_none_scaling_preserves_original_size() {
        // 4x4 pixels are 4x2 cells of half-blocks
        Image image = Image.builder()
            .data(createTestImage(4, 4, 0xFFFF0000))
            .scaling(ImageScaling.NONE)
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 6);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        BufferAssertions.assertThat(buffer).hasContent(
            "          ",
            "          ",
            "   ▀▀▀▀   ",
            "   ▀▀▀▀   ",
            "          ",
            "          ");
    }

    @Test
    void render_none_scaling_with_braille_preserves_original_size() {
        // 4x8 pixels are 2x2 cells of Braille dots
        Image image = Image.builder()
            .data(createTestImage(4, 8, 0xFFFFFFFF))
            .scaling(ImageScaling.NONE)
            .protocol(new BrailleProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 6);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        BufferAssertions.assertThat(buffer).hasContent(
            "          ",
            "          ",
            "    ⣿⣿    ",
            "    ⣿⣿    ",
            "          ",
            "          ");
    }

    @Test
    void render_fill_scaling_covers_area() {
        Image image = Image.builder()
            .data(createTestImage(100, 10, 0xFFFF0000))
            .scaling(ImageScaling.FILL)
            .protocol(new HalfBlockProtocol())
            .build();

        Rect area = new Rect(0, 0, 10, 6);
        Buffer buffer = Buffer.empty(area);

        image.render(area, buffer);

        BufferAssertions.assertThat(buffer).hasContent(
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀",
            "▀▀▀▀▀▀▀▀▀▀");
    }

    @Test
    void render_kitty_consecutive_draws_emit_delete_command() {
        TestBackend backend = new TestBackend(20, 10);
        Terminal<TestBackend> terminal = new Terminal<>(backend);
        KittyProtocol kitty = new KittyProtocol();

        ImageData img1 = createTestImage(10, 10, 0xFFFF0000);
        ImageData img2 = createTestImage(10, 10, 0xFF00FF00);

        // Frame 1: first image
        terminal.draw(frame -> frame.renderWidget(
            Image.builder().data(img1).protocol(kitty).build(),
            new Rect(0, 0, 10, 5)
        ));

        int firstId = kitty.lastImageId();
        assertThat(firstId).isGreaterThan(0);
        assertThat(backend.rawOutput()).doesNotContain("a=d");

        backend.reset();

        // Frame 2: second image (replaces first image)
        terminal.draw(frame -> frame.renderWidget(
            Image.builder().data(img2).protocol(kitty).build(),
            new Rect(0, 0, 10, 5)
        ));

        int secondId = kitty.lastImageId();
        assertThat(secondId).isNotEqualTo(firstId);
        // Must emit delete sequence for firstId
        assertThat(backend.rawOutput()).contains("\033_Ga=d,d=i,i=" + firstId + ",q=2\033\\");
    }

    @Test
    void render_iterm2_marks_display_area_in_buffer() {
        TestBackend backend = new TestBackend(20, 10);
        Terminal<TestBackend> terminal = new Terminal<>(backend);
        ITermProtocol iterm = new ITermProtocol();
        ImageData img = createTestImage(40, 20, 0xFFFF0000);

        CompletedFrame frame = terminal.draw(f -> f.renderWidget(
            Image.builder().data(img).scaling(ImageScaling.FILL).protocol(iterm).build(),
            new Rect(2, 1, 8, 4)
        ));

        Buffer buffer = frame.buffer();
        for (int y = 1; y < 5; y++) {
            for (int x = 2; x < 10; x++) {
                assertThat(buffer.get(x, y)).isEqualTo(Cell.CONTINUATION);
            }
        }
        assertThat(buffer.get(0, 0)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(1, 1)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(10, 1)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(2, 5)).isEqualTo(Cell.EMPTY);
    }

    @Test
    void render_sixel_marks_covered_area_in_buffer() {
        TestBackend backend = new TestBackend(20, 10);
        Terminal<TestBackend> terminal = new Terminal<>(backend);
        SixelProtocol sixel = new SixelProtocol();
        // 32x32 pixels = 4 columns x 2 rows
        ImageData img = createTestImage(32, 32, 0xFFFF0000);

        CompletedFrame frame = terminal.draw(f -> f.renderWidget(
            Image.builder().data(img).scaling(ImageScaling.NONE).protocol(sixel).build(),
            new Rect(1, 1, 6, 4)
        ));

        Buffer buffer = frame.buffer();
        // Covered cells (cols 1..4, rows 1..2)
        for (int y = 1; y <= 2; y++) {
            for (int x = 1; x <= 4; x++) {
                assertThat(buffer.get(x, y)).isEqualTo(Cell.CONTINUATION);
            }
        }
        // Uncovered cells in the widget area
        assertThat(buffer.get(5, 1)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(1, 3)).isEqualTo(Cell.EMPTY);
    }

    @Test
    void render_iterm2_shrinking_image_clears_uncovered_cells_via_diff() {
        TestBackend backend = new TestBackend(10, 5);
        Terminal<TestBackend> terminal = new Terminal<>(backend);
        ITermProtocol iterm = new ITermProtocol();

        ImageData largeImg = createTestImage(80, 40, 0xFFFF0000);
        ImageData smallImg = createTestImage(32, 16, 0xFF00FF00);

        // Frame 1: large 8x4 image
        CompletedFrame frame1 = terminal.draw(f -> f.renderWidget(
            Image.builder().data(largeImg).scaling(ImageScaling.FILL).protocol(iterm).build(),
            new Rect(0, 0, 8, 4)
        ));

        // Frame 2: shrunk 4x2 image
        CompletedFrame frame2 = terminal.draw(f -> f.renderWidget(
            Image.builder().data(smallImg).scaling(ImageScaling.FILL).protocol(iterm).build(),
            new Rect(0, 0, 4, 2)
        ));

        // Diff between frame 1 and frame 2
        DiffResult diff = new DiffResult(50);
        frame1.buffer().diff(frame2.buffer(), diff);

        // Uncovered cells must be dirty and contain Cell.EMPTY so the terminal erases them with spaces
        assertThat(diff.isEmpty()).isFalse();
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 8; x++) {
                if (x >= 4 || y >= 2) {
                    assertThat(frame2.buffer().get(x, y)).isEqualTo(Cell.EMPTY);
                } else {
                    assertThat(frame2.buffer().get(x, y)).isEqualTo(Cell.CONTINUATION);
                }
            }
        }
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

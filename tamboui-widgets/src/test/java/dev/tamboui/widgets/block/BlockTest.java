/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.symbols.merge.MergeStrategy;
import dev.tamboui.text.Line;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static dev.tamboui.assertj.BufferAssertions.assertThat;

class BlockTest {

    @Test
    @DisplayName("Block.bordered creates block with all borders")
    void bordered() {
        Block block = Block.bordered();
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        block.render(area, buffer);

        // Check corners (Plain border type is default)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("┐");
        assertThat(buffer.get(0, 4).symbol()).isEqualTo("└");
        assertThat(buffer.get(9, 4).symbol()).isEqualTo("┘");
    }

    @Test
    @DisplayName("Block inner area calculation with borders")
    void innerWithBorders() {
        Block block = Block.bordered();
        Rect area = new Rect(0, 0, 10, 10);

        Rect inner = block.inner(area);

        assertThat(inner).isEqualTo(new Rect(1, 1, 8, 8));
    }

    @Test
    @DisplayName("Block inner area with padding")
    void innerWithPadding() {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .padding(Padding.uniform(2))
            .build();
        Rect area = new Rect(0, 0, 20, 20);

        Rect inner = block.inner(area);

        // 1 for border + 2 for padding on each side
        assertThat(inner.x()).isEqualTo(3);
        assertThat(inner.y()).isEqualTo(3);
        assertThat(inner.width()).isEqualTo(14); // 20 - 2*3
        assertThat(inner.height()).isEqualTo(14);
    }

    @Test
    @DisplayName("Block inner area reserves space for title even without borders")
    void innerWithTitleNoBorders() {
        Block block = Block.builder()
            .title("Title")
            .build();
        Rect area = new Rect(0, 0, 20, 5);

        Rect inner = block.inner(area);

        // Title takes 1 line even without borders
        assertThat(inner.x()).isEqualTo(0);
        assertThat(inner.y()).isEqualTo(1);
        assertThat(inner.width()).isEqualTo(20);
        assertThat(inner.height()).isEqualTo(4);
    }

    @Test
    @DisplayName("Block inner area reserves space for bottom title even without borders")
    void innerWithBottomTitleNoBorders() {
        Block block = Block.builder()
            .titleBottom("Bottom")
            .build();
        Rect area = new Rect(0, 0, 20, 5);

        Rect inner = block.inner(area);

        // Bottom title takes 1 line even without borders
        assertThat(inner.x()).isEqualTo(0);
        assertThat(inner.y()).isEqualTo(0);
        assertThat(inner.width()).isEqualTo(20);
        assertThat(inner.height()).isEqualTo(4);
    }

    @Test
    @DisplayName("Block with title but no borders renders title and reserves inner space")
    void titleWithoutBordersRendersCorrectly() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 3));
        Block block = Block.builder()
            .title("Title")
            .build();
        block.render(buffer.area(), buffer);

        // Title should be on line 0
        Buffer expected = Buffer.withLines(
            "Title     ",
            "          ",
            "          "
        );
        assertThat(buffer).isEqualTo(expected);

        // Inner area should start at y=1
        Rect inner = block.inner(buffer.area());
        assertThat(inner).isEqualTo(new Rect(0, 1, 10, 2));
    }

    @Test
    @DisplayName("Block without borders")
    void noBorders() {
        Block block = Block.builder().build();
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        block.render(area, buffer);

        // No border characters should be drawn
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Block with title")
    void withTitle() {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .title(Title.from("Test"))
            .build();
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);

        block.render(area, buffer);

        // Title should appear in top border
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("T");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("s");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("t");
    }

    @Test
    @DisplayName("Block with border style")
    void withBorderStyle() {
        Style style = Style.EMPTY.fg(Color.RED);
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderStyle(style)
            .build();
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        block.render(area, buffer);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("Block with different border types")
    void borderTypes() {
        Rect area = new Rect(0, 0, 5, 3);

        // Plain border
        Block plainBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.PLAIN)
            .build();
        Buffer plainBuffer = Buffer.empty(area);
        plainBlock.render(area, plainBuffer);
        assertThat(plainBuffer.get(0, 0).symbol()).isEqualTo("┌");

        // Double border
        Block doubleBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.DOUBLE)
            .build();
        Buffer doubleBuffer = Buffer.empty(area);
        doubleBlock.render(area, doubleBuffer);
        assertThat(doubleBuffer.get(0, 0).symbol()).isEqualTo("╔");
    }

    @Test
    @DisplayName("Title inherits borderStyle")
    void titleInheritsBorderStyle() {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderStyle(Style.EMPTY.fg(Color.YELLOW))
            .title(Title.from("Test"))
            .build();
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);

        block.render(area, buffer);

        // Title should have yellow color from borderStyle
        assertThat(buffer.get(1, 0).style().fg()).contains(Color.YELLOW);
        assertThat(buffer.get(2, 0).style().fg()).contains(Color.YELLOW);
    }

    @Test
    @DisplayName("Block with QUADRANT_INSIDE border renders correctly")
    void quadrantInsideBorder() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 7, 4));
        Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.QUADRANT_INSIDE)
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines(
            "▗▄▄▄▄▄▖",
            "▐     ▌",
            "▐     ▌",
            "▝▀▀▀▀▀▘"
        );
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Block with QUADRANT_OUTSIDE border renders correctly")
    void quadrantOutsideBorder() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 7, 4));
        Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.QUADRANT_OUTSIDE)
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines(
            "▛▀▀▀▀▀▜",
            "▌     ▐",
            "▌     ▐",
            "▙▄▄▄▄▄▟"
        );
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Title with merge strategy preserves existing titles")
    void titleWithMergeStrategyPreservesExisting() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);

        // Render first block with title on the left
        Block leftBlock = Block.builder()
            .borders(Borders.ALL)
            .mergeBorders(MergeStrategy.EXACT)
            .title(Title.from("Left"))
            .build();
        leftBlock.render(new Rect(0, 0, 10, 5), buffer);

        // Render second block with title on the right (overlapping area)
        Block rightBlock = Block.builder()
            .borders(Borders.ALL)
            .mergeBorders(MergeStrategy.EXACT)
            .title(Title.from("Right"))
            .build();
        rightBlock.render(new Rect(10, 0, 10, 5), buffer);

        // Both titles should be visible (they don't overlap in x position)
        // Left title starts at x=1, Right title starts at x=11
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("L");
        assertThat(buffer.get(11, 0).symbol()).isEqualTo("R");
    }

    @Test
    @DisplayName("Title with merge strategy preserves non-overlapping titles")
    void titleWithMergeStrategyPreservesNonOverlappingTitles() {
        Rect area = new Rect(0, 0, 30, 5);
        Buffer buffer = Buffer.empty(area);

        // Render first block with title on the left
        Block block1 = Block.builder()
            .borders(Borders.ALL)
            .mergeBorders(MergeStrategy.EXACT)
            .title(Title.from("Left"))
            .build();
        block1.render(new Rect(0, 0, 15, 5), buffer);

        // Render second block with title on the right (different x position)
        Block block2 = Block.builder()
            .borders(Borders.ALL)
            .mergeBorders(MergeStrategy.EXACT)
            .title(Title.from("Right"))
            .build();
        block2.render(new Rect(15, 0, 15, 5), buffer);

        // Both titles should be visible at their respective positions
        // Left title starts at x=1 (after left border)
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("L");
        // Right title starts at x=16 (15 + 1 for left border)
        assertThat(buffer.get(16, 0).symbol()).isEqualTo("R");
    }

    @Test
    @DisplayName("Border merging with MergeStrategy.EXACT creates merged borders")
    void borderMergingExact() {
        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        // Render first block
        Block block1 = Block.builder()
            .borders(Borders.ALL)
            .mergeBorders(MergeStrategy.EXACT)
            .build();
        block1.render(new Rect(0, 0, 10, 5), buffer);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");

        // Render second block overlapping (should merge borders at intersection)
        Block block2 = Block.builder()
            .borders(Borders.ALL)
            .mergeBorders(MergeStrategy.EXACT)
            .build();
        block2.render(new Rect(5, 0, 10, 5), buffer);

        // The corner of the first block should still be "┌" (not merged at that point)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        // The corner of the second block should be "┐"
        assertThat(buffer.get(14, 0).symbol()).isEqualTo("┐");
    }

    @Test
    @DisplayName("Border merging with MergeStrategy.REPLACE")
    void borderMergingReplace() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        // Render first block
        Block block1 = Block.builder()
            .borders(Borders.ALL)
            .mergeBorders(MergeStrategy.REPLACE)
            .build();
        block1.render(area, buffer);

        // Render second block (should replace, not merge)
        Block block2 = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.DOUBLE)
            .mergeBorders(MergeStrategy.REPLACE)
            .build();
        block2.render(area, buffer);

        // Should be replaced with double border corner
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╔");
    }

    @Test
    @DisplayName("Title with merge strategy preserves overlapping titles on same line")
    void titleWithMergeStrategyPreservesOverlappingTitlesOnSameLine() {
        // Simulate the collapsed-borders-demo scenario
        // Left and right blocks have titles on the same line
        // Top block title should not overwrite them when using EXACT merge
        Rect area = new Rect(0, 0, 30, 5);
        Buffer buffer = Buffer.empty(area);

        // Render left block with title
        Block leftBlock = Block.builder()
            .borders(Borders.ALL)
            .mergeBorders(MergeStrategy.EXACT)
            .title(Title.from("Left Block"))
            .build();
        leftBlock.render(new Rect(0, 0, 15, 5), buffer);

        // Render right block with title (on same line y=0)
        Block rightBlock = Block.builder()
            .borders(Borders.ALL)
            .mergeBorders(MergeStrategy.EXACT)
            .title(Title.from("Right Block"))
            .build();
        rightBlock.render(new Rect(15, 0, 15, 5), buffer);

        // Verify both titles are visible
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("L"); // Left Block starts at x=1
        assertThat(buffer.get(16, 0).symbol()).isEqualTo("R"); // Right Block starts at x=16

        // Render top block with title (overlaps both, centered across full width)
        Block topBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.THICK)
            .mergeBorders(MergeStrategy.EXACT)
            .title(Title.from("Top Block").centered())
            .build();
        topBlock.render(new Rect(0, 0, 30, 5), buffer);

        // Both left and right titles should still be visible
        // "Top Block" is centered, so it's around x=10-18 (9 chars)
        // "Left Block" is at x=1-10 (10 chars), "Right Block" is at x=16-26 (10 chars)
        // They might overlap partially, but non-overlapping characters should be preserved
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("L"); // Should still be there
        assertThat(buffer.get(16, 0).symbol()).isEqualTo("R"); // Should still be there
    }

    @Test
    @DisplayName("Render plain border")
    void renderPlainBorder() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 3));
        Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.PLAIN)
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines(
            "┌────────┐",
            "│        │",
            "└────────┘"
        );
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Render rounded border")
    void renderRoundedBorder() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 3));
        Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines(
            "╭────────╮",
            "│        │",
            "╰────────╯"
        );
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Render double border")
    void renderDoubleBorder() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 3));
        Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.DOUBLE)
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines(
            "╔════════╗",
            "║        ║",
            "╚════════╝"
        );
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Render thick border")
    void renderThickBorder() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 3));
        Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.THICK)
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines(
            "┏━━━━━━━━┓",
            "┃        ┃",
            "┗━━━━━━━━┛"
        );
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Render block with title")
    void renderBlockWithTitle() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 3));
        Block.builder()
            .borders(Borders.ALL)
            .title("test")
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines(
            "┌test────┐",
            "│        │",
            "└────────┘"
        );
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Title top and bottom")
    void titleTopBottom() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 11, 3));
        Block.builder()
            .borders(Borders.ALL)
            .title("Top")
            .titleBottom("Bottom")
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines(
            "┌Top──────┐",
            "│         │",
            "└Bottom───┘"
        );
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Title alignment")
    void titleAlignment() {
        // Left aligned
        Buffer buffer = Buffer.empty(new Rect(0, 0, 8, 1));
        Block.builder()
            .title(Title.from("test").alignment(Alignment.LEFT))
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines("test    ");
        assertThat(buffer).isEqualTo(expected);

        // Center aligned
        buffer = Buffer.empty(new Rect(0, 0, 8, 1));
        Block.builder()
            .title(Title.from("test").alignment(Alignment.CENTER))
            .build()
            .render(buffer.area(), buffer);
        expected = Buffer.withLines("  test  ");
        assertThat(buffer).isEqualTo(expected);

        // Right aligned
        buffer = Buffer.empty(new Rect(0, 0, 8, 1));
        Block.builder()
            .title(Title.from("test").alignment(Alignment.RIGHT))
            .build()
            .render(buffer.area(), buffer);
        expected = Buffer.withLines("    test");
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Title alignment with Line")
    void titleAlignmentWithLine() {
        // Left aligned
        Buffer buffer = Buffer.empty(new Rect(0, 0, 8, 1));
        Block.builder()
            .title(Title.from(Line.from("test")).left())
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines("test    ");
        assertThat(buffer).isEqualTo(expected);

        // Center aligned
        buffer = Buffer.empty(new Rect(0, 0, 8, 1));
        Block.builder()
            .title(Title.from(Line.from("test")).centered())
            .build()
            .render(buffer.area(), buffer);
        expected = Buffer.withLines("  test  ");
        assertThat(buffer).isEqualTo(expected);

        // Right aligned
        buffer = Buffer.empty(new Rect(0, 0, 8, 1));
        Block.builder()
            .title(Title.from(Line.from("test")).right())
            .build()
            .render(buffer.area(), buffer);
        expected = Buffer.withLines("    test");
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Title bottom position")
    void titleBottomPosition() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 4, 2));
        Block.builder()
            .titleBottom("test")
            .build()
            .render(buffer.area(), buffer);
        Buffer expected = Buffer.withLines(
            "    ",
            "test"
        );
        assertThat(buffer).isEqualTo(expected);
    }

    static Stream<Arguments> mergeStrategyProvider() {
        return Stream.of(
            Arguments.of(MergeStrategy.REPLACE, "merge_replace.txt"),
            Arguments.of(MergeStrategy.EXACT, "merge_exact.txt"),
            Arguments.of(MergeStrategy.FUZZY, "merge_fuzzy.txt")
        );
    }

    @ParameterizedTest
    @MethodSource("mergeStrategyProvider")
    @DisplayName("Render merged borders with different merge strategies")
    void renderMergedBorders(MergeStrategy strategy, String expectedFile) throws IOException {
        // Test with all border types that have expected output in the test files
        // (The test files from Ratatui include Plain, Rounded, Thick, Double, and all dashed variants)
        BorderType[] borderTypes = {
            BorderType.PLAIN,
            BorderType.ROUNDED,
            BorderType.THICK,
            BorderType.DOUBLE,
            BorderType.LIGHT_DOUBLE_DASHED,
            BorderType.HEAVY_DOUBLE_DASHED,
            BorderType.LIGHT_TRIPLE_DASHED,
            BorderType.HEAVY_TRIPLE_DASHED,
            BorderType.LIGHT_QUADRUPLE_DASHED,
            BorderType.HEAVY_QUADRUPLE_DASHED
        };

        // Test rects: touching at corners, overlapping, touching vertical edges, touching horizontal edges
        Rect[][] rects = {
            {new Rect(0, 0, 5, 5), new Rect(4, 4, 5, 5)},      // touching at corners
            {new Rect(10, 0, 5, 5), new Rect(12, 2, 5, 5)},    // overlapping
            {new Rect(18, 0, 5, 5), new Rect(22, 0, 5, 5)},   // touching vertical edges
            {new Rect(28, 0, 5, 5), new Rect(28, 4, 5, 5)}     // touching horizontal edges
        };

        Buffer buffer = Buffer.empty(new Rect(0, 0, 43, 1000));

        int offsetY = 0;
        for (BorderType borderType1 : borderTypes) {
            for (BorderType borderType2 : borderTypes) {
                // Render title (format: "Plain + Rounded" to match expected files)
                String title = formatBorderTypeName(borderType1) + " + " + formatBorderTypeName(borderType2);
                buffer.setString(0, offsetY, title, Style.EMPTY);
                offsetY += 1;

                // Render blocks for each rect pair
                for (Rect[] rectPair : rects) {
                    Rect rect1 = new Rect(rectPair[0].x(), rectPair[0].y() + offsetY, rectPair[0].width(), rectPair[0].height());
                    Rect rect2 = new Rect(rectPair[1].x(), rectPair[1].y() + offsetY, rectPair[1].width(), rectPair[1].height());

                    Block.builder()
                        .borders(Borders.ALL)
                        .borderType(borderType1)
                        .mergeBorders(strategy)
                        .build()
                        .render(rect1, buffer);

                    Block.builder()
                        .borders(Borders.ALL)
                        .borderType(borderType2)
                        .mergeBorders(strategy)
                        .build()
                        .render(rect2, buffer);
                }
                offsetY += 9;
            }
        }

        // Load expected output from resource file and compare entire buffer (like Ratatui)
        String resourceFile = expectedFile;
        try {
            String expectedContent = loadResourceFile("dev/tamboui/widgets/block/" + resourceFile);
            Buffer expected = parseExpectedBufferFromContent(expectedContent);

            // Compare the entire buffer directly, just like Ratatui does
            assertThat(buffer).isEqualTo(expected);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load expected file: " + resourceFile, e);
        }
    }

    private String loadResourceFile(String resourcePath) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        }
    }

    private String formatBorderTypeName(BorderType type) {
        // Format Java enum name to match Rust format in expected files
        switch (type) {
            case PLAIN: return "Plain";
            case ROUNDED: return "Rounded";
            case THICK: return "Thick";
            case DOUBLE: return "Double";
            case LIGHT_DOUBLE_DASHED: return "LightDoubleDashed";
            case HEAVY_DOUBLE_DASHED: return "HeavyDoubleDashed";
            case LIGHT_TRIPLE_DASHED: return "LightTripleDashed";
            case HEAVY_TRIPLE_DASHED: return "HeavyTripleDashed";
            case LIGHT_QUADRUPLE_DASHED: return "LightQuadrupleDashed";
            case HEAVY_QUADRUPLE_DASHED: return "HeavyQuadrupleDashed";
            case QUADRANT_INSIDE: return "QuadrantInside";
            case QUADRANT_OUTSIDE: return "QuadrantOutside";
            default: return type.name();
        }
    }

    private Buffer parseExpectedBufferFromContent(String content) {
        // Parse the entire expected content into a buffer, just like Ratatui's Buffer::with_lines()
        // Normalize line endings (handle both \n and \r\n)
        content = content.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = content.split("\n");

        if (lines.length == 0) {
            return Buffer.empty(new Rect(0, 0, 0, 0));
        }

        // Find max width
        int maxWidth = 0;
        for (String line : lines) {
            if (line != null) {
                int width = line.codePointCount(0, line.length());
                maxWidth = Math.max(maxWidth, width);
            }
        }

        // Create buffer with exact dimensions needed
        Buffer buffer = Buffer.empty(new Rect(0, 0, Math.max(maxWidth, 43), lines.length));

        // Parse all lines into buffer
        for (int y = 0; y < lines.length; y++) {
            String line = lines[y];
            if (line != null) {
                // Truncate line to buffer width if needed
                int lineWidth = line.codePointCount(0, line.length());
                if (lineWidth > buffer.area().width()) {
                    int codePointCount = 0;
                    int endIndex = 0;
                    for (int i = 0; i < line.length() && codePointCount < buffer.area().width(); i++) {
                        if (Character.isHighSurrogate(line.charAt(i))) {
                            i++;
                        }
                        codePointCount++;
                        endIndex = i + 1;
                    }
                    line = line.substring(0, endIndex);
                }
                buffer.setString(0, y, line, Style.EMPTY);
            }
        }

        return buffer;
    }

}

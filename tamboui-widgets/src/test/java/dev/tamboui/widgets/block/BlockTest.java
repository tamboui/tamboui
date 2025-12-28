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

}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.sparkline;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.block.Block;

import static org.assertj.core.api.Assertions.*;

class SparklineTest {

    @Test
    @DisplayName("Sparkline renders basic data")
    void rendersBasicData() {
        Sparkline sparkline = Sparkline.from(0, 4, 8);
        Rect area = new Rect(0, 0, 3, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" "); // 0
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("▄"); // 4/8 = 0.5
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("█"); // 8/8 = 1.0
    }

    @Test
    @DisplayName("Sparkline renders with max value")
    void rendersWithMaxValue() {
        Sparkline sparkline = Sparkline.builder().data(50, 100).max(100).build();
        Rect area = new Rect(0, 0, 2, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("▄"); // 50/100 = 0.5
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("█"); // 100/100 = 1.0
    }

    @Test
    @DisplayName("Sparkline auto-scales to data max")
    void autoScalesToDataMax() {
        Sparkline sparkline = Sparkline.from(25, 50, 100);
        Rect area = new Rect(0, 0, 3, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        // Max is 100, so values are 0.25, 0.5, 1.0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("▂"); // ~0.25
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("▄"); // 0.5
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("█"); // 1.0
    }

    @Test
    @DisplayName("Sparkline renders with style")
    void rendersWithStyle() {
        Style style = Style.EMPTY.fg(Color.CYAN);
        Sparkline sparkline = Sparkline.builder().data(5).style(style).build();
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("Sparkline renders with block")
    void rendersWithBlock() {
        Sparkline sparkline = Sparkline.builder().data(8).max(8).block(Block.bordered()).build();
        Rect area = new Rect(0, 0, 5, 3);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        // Block corners
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("┐");
        // Sparkline inside block (bottom of inner area)
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Sparkline truncates data to fit area")
    void truncatesDataToFitArea() {
        Sparkline sparkline = Sparkline.from(1, 2, 3, 4, 5, 6, 7, 8);
        Rect area = new Rect(0, 0, 3, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        // Should show last 3 values (6, 7, 8 of max 8)
        // 6/8 = 0.75, 7/8 = 0.875, 8/8 = 1.0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("▆"); // 6/8
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("▇"); // 7/8
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("█"); // 8/8
    }

    @Test
    @DisplayName("Sparkline renders right-to-left")
    void rendersRightToLeft() {
        Sparkline sparkline = Sparkline.builder().data(0, 8)
                .direction(Sparkline.RenderDirection.RIGHT_TO_LEFT).build();
        Rect area = new Rect(0, 0, 2, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        // Right to left: data[1]=8 at x=1, data[0]=0 at x=0
        assertThat(buffer.get(1, 0).symbol()).isEqualTo(" "); // 0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█"); // 8
    }

    @Test
    @DisplayName("Sparkline with THREE_LEVELS bar set")
    void withThreeLevelsBarSet() {
        Sparkline sparkline = Sparkline.builder().data(0, 4, 8)
                .barSet(Sparkline.BarSet.THREE_LEVELS).build();
        Rect area = new Rect(0, 0, 3, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" "); // 0
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("▄"); // mid
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("█"); // full
    }

    @Test
    @DisplayName("Sparkline handles empty data")
    void handlesEmptyData() {
        Sparkline sparkline = Sparkline.from();
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        // Should not throw
        sparkline.render(area, buffer);

        // Buffer should remain empty
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Sparkline handles empty area")
    void handlesEmptyArea() {
        Sparkline sparkline = Sparkline.from(1, 2, 3);
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 5));

        // Should not throw
        sparkline.render(area, buffer);
    }

    @Test
    @DisplayName("Sparkline handles all zero data")
    void handlesAllZeroData() {
        Sparkline sparkline = Sparkline.from(0, 0, 0);
        Rect area = new Rect(0, 0, 3, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        // All should be empty (avoiding division by zero)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Sparkline from List")
    void fromList() {
        Sparkline sparkline = Sparkline.from(Arrays.asList(0L, 4L, 8L));
        Rect area = new Rect(0, 0, 3, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        assertThat(buffer.get(2, 0).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Sparkline builder with int array")
    void builderWithIntArray() {
        Sparkline sparkline = Sparkline.builder().data(0, 4, 8).build();
        Rect area = new Rect(0, 0, 3, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        assertThat(buffer.get(2, 0).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("BarSet NINE_LEVELS covers all levels")
    void barSetNineLevels() {
        Sparkline.BarSet set = Sparkline.BarSet.NINE_LEVELS;

        assertThat(set.symbolForLevel(0.0)).isEqualTo(" ");
        assertThat(set.symbolForLevel(0.125)).isEqualTo("▁");
        assertThat(set.symbolForLevel(0.25)).isEqualTo("▂");
        assertThat(set.symbolForLevel(0.375)).isEqualTo("▃");
        assertThat(set.symbolForLevel(0.5)).isEqualTo("▄");
        assertThat(set.symbolForLevel(0.625)).isEqualTo("▅");
        assertThat(set.symbolForLevel(0.75)).isEqualTo("▆");
        assertThat(set.symbolForLevel(0.875)).isEqualTo("▇");
        assertThat(set.symbolForLevel(1.0)).isEqualTo("█");
    }

    @Test
    @DisplayName("BarSet handles edge cases")
    void barSetEdgeCases() {
        Sparkline.BarSet set = Sparkline.BarSet.NINE_LEVELS;

        assertThat(set.symbolForLevel(-0.5)).isEqualTo(" ");
        assertThat(set.symbolForLevel(1.5)).isEqualTo("█");
    }

    @Test
    @DisplayName("Sparkline renders at bottom of area")
    void rendersAtBottomOfArea() {
        Sparkline sparkline = Sparkline.from(8);
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        // Sparkline should render at bottom row
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo(" ");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Sparkline with fewer data points than width")
    void fewerDataPointsThanWidth() {
        Sparkline sparkline = Sparkline.from(4, 8);
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        // Only 2 data points rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("▄"); // 4/8
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("█"); // 8/8
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" "); // empty
    }

    @Test
    @DisplayName("autoMax clears explicit max")
    void autoMaxClearsExplicitMax() {
        Sparkline sparkline = Sparkline.builder().data(50, 100).max(200).autoMax().build();
        Rect area = new Rect(0, 0, 2, 1);
        Buffer buffer = Buffer.empty(area);

        sparkline.render(area, buffer);

        // Without explicit max, 100 is the max
        // 50/100 = 0.5, 100/100 = 1.0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("▄");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("█");
    }
}

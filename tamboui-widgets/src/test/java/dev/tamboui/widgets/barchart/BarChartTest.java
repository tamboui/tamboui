/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.barchart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.TestStylePropertyResolver;
import dev.tamboui.widgets.block.Block;

import static org.assertj.core.api.Assertions.*;

class BarChartTest {

    @Test
    @DisplayName("Bar creates with value")
    void barCreatesWithValue() {
        Bar bar = Bar.of(75);
        assertThat(bar.value()).isEqualTo(75);
        assertThat(bar.label()).isEmpty();
    }

    @Test
    @DisplayName("Bar creates with value and label")
    void barCreatesWithValueAndLabel() {
        Bar bar = Bar.of(75, "Sales");
        assertThat(bar.value()).isEqualTo(75);
        assertThat(bar.label()).isPresent();
        assertThat(bar.label().get().rawContent()).isEqualTo("Sales");
    }

    @Test
    @DisplayName("Bar builder with all options")
    void barBuilderWithAllOptions() {
        Bar bar = Bar.builder()
            .value(100)
            .label("Test")
            .textValue("100%")
            .style(Style.EMPTY.fg(Color.RED))
            .valueStyle(Style.EMPTY.fg(Color.GREEN))
            .build();

        assertThat(bar.value()).isEqualTo(100);
        assertThat(bar.textValue()).contains("100%");
        assertThat(bar.displayValue()).isEqualTo("100%");
        assertThat(bar.style()).isPresent();
        assertThat(bar.valueStyle()).isPresent();
    }

    @Test
    @DisplayName("Bar displayValue uses textValue when set")
    void barDisplayValueUsesTextValue() {
        Bar bar = Bar.builder().value(50).textValue("50%").build();
        assertThat(bar.displayValue()).isEqualTo("50%");
    }

    @Test
    @DisplayName("Bar displayValue uses numeric value when textValue not set")
    void barDisplayValueUsesNumeric() {
        Bar bar = Bar.of(50);
        assertThat(bar.displayValue()).isEqualTo("50");
    }

    @Test
    @DisplayName("BarGroup creates from values")
    void barGroupCreatesFromValues() {
        BarGroup group = BarGroup.of(10, 20, 30);
        assertThat(group.size()).isEqualTo(3);
        assertThat(group.maxValue()).isEqualTo(30);
    }

    @Test
    @DisplayName("BarGroup creates from bars")
    void barGroupCreatesFromBars() {
        BarGroup group = BarGroup.of(
            Bar.of(10, "A"),
            Bar.of(20, "B")
        );
        assertThat(group.size()).isEqualTo(2);
        assertThat(group.label()).isEmpty();
    }

    @Test
    @DisplayName("BarGroup creates with label")
    void barGroupCreatesWithLabel() {
        BarGroup group = BarGroup.of("Q1",
            Bar.of(100, "Jan"),
            Bar.of(150, "Feb")
        );
        assertThat(group.label()).isPresent();
        assertThat(group.label().get().rawContent()).isEqualTo("Q1");
    }

    @Test
    @DisplayName("BarGroup builder")
    void barGroupBuilder() {
        BarGroup group = BarGroup.builder()
            .label("Test")
            .addBar(10)
            .addBar(20, "B")
            .addBar(Bar.of(30))
            .build();

        assertThat(group.size()).isEqualTo(3);
        assertThat(group.maxValue()).isEqualTo(30);
    }

    @Test
    @DisplayName("BarChart renders vertical bars")
    void rendersVerticalBars() {
        BarChart chart = BarChart.builder()
            .data(BarGroup.of(8))
            .max(8)
            .barWidth(1)
            .barGap(0)
            .build();
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Full bar should render
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("█");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("█");
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("BarChart renders half-height bar")
    void rendersHalfHeightBar() {
        BarChart chart = BarChart.builder()
            .data(BarGroup.of(4))
            .max(8)
            .barWidth(1)
            .build();
        Rect area = new Rect(0, 0, 3, 4);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Half-height bar (value 4 of max 8 = 50%)
        assertThat(buffer.get(0, 3).symbol()).isEqualTo("█");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("BarChart renders multiple bars")
    void rendersMultipleBars() {
        BarChart chart = BarChart.builder()
            .data(BarGroup.of(4, 8))
            .max(8)
            .barWidth(1)
            .barGap(1)
            .build();
        Rect area = new Rect(0, 0, 5, 4);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // First bar at x=0
        assertThat(buffer.get(0, 3).symbol()).isEqualTo("█");
        // Gap at x=1
        assertThat(buffer.get(1, 3).symbol()).isEqualTo(" ");
        // Second bar at x=2
        assertThat(buffer.get(2, 3).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("BarChart renders with bar style")
    void rendersWithBarStyle() {
        BarChart chart = BarChart.builder()
            .data(BarGroup.of(8))
            .max(8)
            .barStyle(Style.EMPTY.fg(Color.CYAN))
            .build();
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer.get(0, 2).style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("BarChart renders with block")
    void rendersWithBlock() {
        BarChart chart = BarChart.builder()
            .data(BarGroup.of(8))
            .max(8)
            .block(Block.bordered())
            .build();
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Block corners
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("┐");
        // Bar inside block
        assertThat(buffer.get(1, 3).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("BarChart renders horizontal bars")
    void rendersHorizontalBars() {
        BarChart chart = BarChart.builder()
            .data(BarGroup.of(Bar.of(8, "A")))
            .max(8)
            .direction(Direction.HORIZONTAL)
            .barWidth(1)
            .build();
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Label on left, bar extends right
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        // Bar should have full blocks
        boolean hasFullBlock = false;
        for (int x = 2; x < 10; x++) {
            if (buffer.get(x, 0).symbol().equals("█")) {
                hasFullBlock = true;
                break;
            }
        }
        assertThat(hasFullBlock).isTrue();
    }

    @Test
    @DisplayName("BarChart with multiple groups")
    void withMultipleGroups() {
        BarChart chart = BarChart.builder()
            .data(
                BarGroup.of(4),
                BarGroup.of(8)
            )
            .max(8)
            .barWidth(1)
            .barGap(0)
            .groupGap(1)
            .build();
        Rect area = new Rect(0, 0, 5, 4);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // First group bar at x=0
        assertThat(buffer.get(0, 3).symbol()).isEqualTo("█");
        // Gap at x=1
        assertThat(buffer.get(1, 3).symbol()).isEqualTo(" ");
        // Second group bar at x=2
        assertThat(buffer.get(2, 3).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("BarChart handles empty data")
    void handlesEmptyData() {
        BarChart chart = BarChart.builder().build();
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        // Should not throw
        chart.render(area, buffer);
    }

    @Test
    @DisplayName("BarChart handles empty area")
    void handlesEmptyArea() {
        BarChart chart = BarChart.builder()
            .data(BarGroup.of(1, 2, 3))
            .build();
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 5));

        // Should not throw
        chart.render(area, buffer);
    }

    @Test
    @DisplayName("BarChart with wider bars")
    void withWiderBars() {
        BarChart chart = BarChart.builder()
            .data(BarGroup.of(8))
            .max(8)
            .barWidth(3)
            .build();
        Rect area = new Rect(0, 0, 5, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Bar should be 3 columns wide
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("█");
        assertThat(buffer.get(1, 2).symbol()).isEqualTo("█");
        assertThat(buffer.get(2, 2).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("BarSet NINE_LEVELS has correct symbols")
    void barSetNineLevels() {
        BarChart.BarSet set = BarChart.BarSet.NINE_LEVELS;
        String[] symbols = set.symbols();

        assertThat(symbols).hasSize(9);
        assertThat(symbols[0]).isEqualTo(" ");
        assertThat(symbols[8]).isEqualTo("█");
    }

    @Test
    @DisplayName("BarSet HORIZONTAL has correct symbols")
    void barSetHorizontal() {
        BarChart.BarSet set = BarChart.BarSet.HORIZONTAL;

        assertThat(set.empty()).isEqualTo(" ");
        assertThat(set.full()).isEqualTo("█");
        assertThat(set.half()).isEqualTo("▌");
    }

    @Test
    @DisplayName("BarChart auto-scales to max value")
    void autoScalesToMaxValue() {
        BarChart chart = BarChart.builder()
            .data(BarGroup.of(50, 100))
            .barWidth(1)
            .barGap(1)
            .build();
        Rect area = new Rect(0, 0, 5, 4);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Second bar (100) should be full height
        assertThat(buffer.get(2, 3).symbol()).isEqualTo("█");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Bar value cannot be negative")
    void barValueCannotBeNegative() {
        Bar bar = Bar.builder().value(-10).build();
        assertThat(bar.value()).isEqualTo(0);
    }

    @Test
    @DisplayName("BarChart uses BAR_COLOR property from StylePropertyResolver")
    void usesBarColorProperty() {
        BarChart chart = BarChart.builder()
                .data(BarGroup.of(8))
                .max(8)
                .styleResolver(TestStylePropertyResolver.of("bar-color", Color.CYAN))
                .build();
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        BufferAssertions.assertThat(buffer).at(0, 2).hasForeground(Color.CYAN);
    }
}

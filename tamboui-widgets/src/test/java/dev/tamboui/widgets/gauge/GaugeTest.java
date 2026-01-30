/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.gauge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.TestStylePropertyResolver;
import dev.tamboui.widgets.block.Block;

import static org.assertj.core.api.Assertions.*;

class GaugeTest {

    @Test
    @DisplayName("Gauge renders at 0%")
    void rendersAtZeroPercent() {
        Gauge gauge = Gauge.percent(0);
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // Label "0%" should be centered - in 10 chars, "0%" (2 chars) starts at
        // position 4
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("0");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("%");
        // No filled blocks at 0%
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Gauge renders at 100%")
    void rendersAtHundredPercent() {
        Gauge gauge = Gauge.percent(100);
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // All cells should have full block (except where label overwrites)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Gauge renders at 50%")
    void rendersAtFiftyPercent() {
        Gauge gauge = Gauge.builder().percent(50).label("") // Empty label to check fill without
                                                            // interference
                .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // First 5 cells should be filled
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("█");
        // Cells after position 5 should be empty
        assertThat(buffer.get(5, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Gauge with ratio")
    void withRatio() {
        Gauge gauge = Gauge.ratio(0.25);
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // First 5 cells (25% of 20) should be filled
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Gauge with custom label")
    void withCustomLabel() {
        Gauge gauge = Gauge.builder().percent(50).label("Loading...").build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // "Loading..." is 10 chars, centered in 20 = starts at position 5
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("L");
        assertThat(buffer.get(6, 0).symbol()).isEqualTo("o");
    }

    @Test
    @DisplayName("Gauge with gauge style")
    void withGaugeStyle() {
        Style gaugeStyle = Style.EMPTY.fg(Color.GREEN);
        Gauge gauge = Gauge.builder().percent(100).gaugeStyle(gaugeStyle).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.GREEN);
    }

    @Test
    @DisplayName("Gauge with block")
    void withBlock() {
        Gauge gauge = Gauge.builder().percent(50).block(Block.bordered()).build();
        Rect area = new Rect(0, 0, 12, 3);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // Block border should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        assertThat(buffer.get(11, 0).symbol()).isEqualTo("┐");
        // Gauge renders inside block
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Gauge without unicode uses only full blocks")
    void withoutUnicode() {
        Gauge gauge = Gauge.builder().ratio(0.15) // 15% of 10 = 1.5 cells
                .useUnicode(false).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // Only first cell should be full block, no partial
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        // Second cell should not have partial block
        assertThat(buffer.get(1, 0).symbol()).isNotEqualTo("█");
    }

    @Test
    @DisplayName("Gauge with unicode uses partial blocks")
    void withUnicode() {
        Gauge gauge = Gauge.builder().ratio(0.15) // 15% of 10 = 1.5 cells
                .useUnicode(true).label("") // Empty label to not interfere
                .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // First cell should be full block
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        // Second cell should have partial block (4/8 = ▌)
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("▌");
    }

    @Test
    @DisplayName("Gauge percent validation")
    void percentValidation() {
        assertThatThrownBy(() -> Gauge.builder().percent(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gauge.builder().percent(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Gauge ratio validation")
    void ratioValidation() {
        assertThatThrownBy(() -> Gauge.builder().ratio(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gauge.builder().ratio(1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Gauge renders in multi-line area")
    void multiLineArea() {
        Gauge gauge = Gauge.percent(50);
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // All rows should have the gauge
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("█");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Gauge uses GAUGE_COLOR property from StylePropertyResolver")
    void usesGaugeColorProperty() {
        Gauge gauge = Gauge.builder().percent(100)
                .styleResolver(TestStylePropertyResolver.of("gauge-color", Color.MAGENTA)).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        BufferAssertions.assertThat(buffer).at(0, 0).hasForeground(Color.MAGENTA);
    }
}

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

import static org.assertj.core.api.Assertions.*;

class LineGaugeTest {

    @Test
    @DisplayName("LineGauge renders at 0%")
    void rendersAtZeroPercent() {
        LineGauge gauge = LineGauge.percent(0);
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // All unfilled
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("─");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("─");
    }

    @Test
    @DisplayName("LineGauge renders at 100%")
    void rendersAtHundredPercent() {
        LineGauge gauge = LineGauge.percent(100);
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // All filled
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("━");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("━");
    }

    @Test
    @DisplayName("LineGauge renders at 50%")
    void rendersAtFiftyPercent() {
        LineGauge gauge = LineGauge.percent(50);
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // First half filled
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("━");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("━");
        // Second half unfilled
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("─");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("─");
    }

    @Test
    @DisplayName("LineGauge with ratio")
    void withRatio() {
        LineGauge gauge = LineGauge.ratio(0.25);
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // First 5 cells (25% of 20) filled
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("━");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("━");
        // Rest unfilled
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("─");
    }

    @Test
    @DisplayName("LineGauge with label")
    void withLabel() {
        LineGauge gauge = LineGauge.builder().percent(50).label("CPU: ").build();
        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // Label at start
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("C");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("P");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("U");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo(":");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo(" ");
        // Gauge starts after label (position 5)
        // Remaining width is 10, 50% = 5 filled
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("━");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("━");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("─");
    }

    @Test
    @DisplayName("LineGauge with filled style")
    void withFilledStyle() {
        Style filledStyle = Style.EMPTY.fg(Color.GREEN);
        LineGauge gauge = LineGauge.builder().percent(100).filledStyle(filledStyle).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.GREEN);
    }

    @Test
    @DisplayName("LineGauge with unfilled style")
    void withUnfilledStyle() {
        Style unfilledStyle = Style.EMPTY.fg(Color.DARK_GRAY);
        LineGauge gauge = LineGauge.builder().percent(0).unfilledStyle(unfilledStyle).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.DARK_GRAY);
    }

    @Test
    @DisplayName("LineGauge with thick line set")
    void withThickLineSet() {
        LineGauge gauge = LineGauge.builder().percent(50).lineSet(LineGauge.THICK).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        // Both filled and unfilled use thick lines
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("━");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("━");
    }

    @Test
    @DisplayName("LineGauge with double line set")
    void withDoubleLineSet() {
        LineGauge gauge = LineGauge.builder().percent(50).lineSet(LineGauge.DOUBLE).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("═");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("═");
    }

    @Test
    @DisplayName("LineGauge with custom line set")
    void withCustomLineSet() {
        LineGauge.LineSet customSet = new LineGauge.LineSet(".", "#");
        LineGauge gauge = LineGauge.builder().percent(50).lineSet(customSet).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("#");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo(".");
    }

    @Test
    @DisplayName("LineGauge percent validation")
    void percentValidation() {
        assertThatThrownBy(() -> LineGauge.builder().percent(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LineGauge.builder().percent(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("LineGauge ratio validation")
    void ratioValidation() {
        assertThatThrownBy(() -> LineGauge.builder().ratio(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LineGauge.builder().ratio(1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("LineSet validation")
    void lineSetValidation() {
        // Empty strings should throw IllegalArgumentException
        assertThatThrownBy(() -> new LineGauge.LineSet("", "#"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LineGauge.LineSet(".", ""))
                .isInstanceOf(IllegalArgumentException.class);
        // Valid LineSet should work
        LineGauge.LineSet validSet = new LineGauge.LineSet(".", "#");
        assertThat(validSet.unfilled()).isEqualTo(".");
        assertThat(validSet.filled()).isEqualTo("#");
    }

    @Test
    @DisplayName("LineGauge uses FILLED_COLOR property from StylePropertyResolver")
    void usesFilledColorProperty() {
        LineGauge gauge = LineGauge.builder().percent(100)
                .styleResolver(TestStylePropertyResolver.of("filled-color", Color.GREEN)).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        BufferAssertions.assertThat(buffer).at(0, 0).hasForeground(Color.GREEN);
    }

    @Test
    @DisplayName("LineGauge uses UNFILLED_COLOR property from StylePropertyResolver")
    void usesUnfilledColorProperty() {
        LineGauge gauge = LineGauge.builder().percent(0)
                .styleResolver(TestStylePropertyResolver.of("unfilled-color", Color.DARK_GRAY))
                .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        gauge.render(area, buffer);

        BufferAssertions.assertThat(buffer).at(0, 0).hasForeground(Color.DARK_GRAY);
    }
}

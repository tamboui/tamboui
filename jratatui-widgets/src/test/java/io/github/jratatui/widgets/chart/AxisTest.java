/*
 * Copyright (c) 2025 JRatatui Contributors
 * SPDX-License-Identifier: MIT
 */
package io.github.jratatui.widgets.chart;

import io.github.jratatui.layout.Alignment;
import io.github.jratatui.style.Color;
import io.github.jratatui.style.Style;
import io.github.jratatui.text.Line;
import io.github.jratatui.text.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AxisTest {

    @Test
    void defaults_creates_axis_with_zero_bounds() {
        var axis = Axis.defaults();

        assertThat(axis.min()).isEqualTo(0.0);
        assertThat(axis.max()).isEqualTo(0.0);
        assertThat(axis.range()).isEqualTo(0.0);
        assertThat(axis.hasLabels()).isFalse();
        assertThat(axis.title()).isEmpty();
    }

    @Test
    void builder_title_string() {
        var axis = Axis.builder()
            .title("X Axis")
            .build();

        assertThat(axis.title()).isPresent();
        assertThat(axis.title().get().rawContent()).isEqualTo("X Axis");
    }

    @Test
    void builder_title_line() {
        var line = Line.from("Y Axis");
        var axis = Axis.builder()
            .title(line)
            .build();

        assertThat(axis.title()).isPresent();
        assertThat(axis.title().get()).isEqualTo(line);
    }

    @Test
    void builder_title_spans() {
        var axis = Axis.builder()
            .title(Span.styled("X", Style.EMPTY.fg(Color.RED)), Span.raw(" Axis"))
            .build();

        assertThat(axis.title()).isPresent();
        assertThat(axis.title().get().rawContent()).isEqualTo("X Axis");
    }

    @Test
    void builder_title_null_clears_title() {
        var axis = Axis.builder()
            .title("Test")
            .title((String) null)
            .build();

        assertThat(axis.title()).isEmpty();
    }

    @Test
    void builder_bounds_min_max() {
        var axis = Axis.builder()
            .bounds(0, 100)
            .build();

        assertThat(axis.min()).isEqualTo(0.0);
        assertThat(axis.max()).isEqualTo(100.0);
        assertThat(axis.range()).isEqualTo(100.0);
    }

    @Test
    void builder_bounds_array() {
        var axis = Axis.builder()
            .bounds(new double[] {-50, 50})
            .build();

        assertThat(axis.min()).isEqualTo(-50.0);
        assertThat(axis.max()).isEqualTo(50.0);
        assertThat(axis.range()).isEqualTo(100.0);
    }

    @Test
    void builder_bounds_null_keeps_default() {
        var axis = Axis.builder()
            .bounds(null)
            .build();

        assertThat(axis.min()).isEqualTo(0.0);
        assertThat(axis.max()).isEqualTo(0.0);
    }

    @Test
    void builder_labels_strings() {
        var axis = Axis.builder()
            .labels("0", "25", "50", "75", "100")
            .build();

        assertThat(axis.hasLabels()).isTrue();
        assertThat(axis.labels()).hasSize(5);
        assertThat(axis.labels().get(0).content()).isEqualTo("0");
        assertThat(axis.labels().get(4).content()).isEqualTo("100");
    }

    @Test
    void builder_labels_spans() {
        var span1 = Span.styled("Min", Style.EMPTY.fg(Color.GREEN));
        var span2 = Span.styled("Max", Style.EMPTY.fg(Color.RED));

        var axis = Axis.builder()
            .labels(span1, span2)
            .build();

        assertThat(axis.labels()).hasSize(2);
        assertThat(axis.labels().get(0)).isEqualTo(span1);
        assertThat(axis.labels().get(1)).isEqualTo(span2);
    }

    @Test
    void builder_labels_list() {
        var labels = List.of(Span.raw("A"), Span.raw("B"), Span.raw("C"));

        var axis = Axis.builder()
            .labels(labels)
            .build();

        assertThat(axis.labels()).hasSize(3);
    }

    @Test
    void builder_labels_null_clears_labels() {
        var axis = Axis.builder()
            .labels("1", "2", "3")
            .labels((String[]) null)
            .build();

        assertThat(axis.hasLabels()).isFalse();
    }

    @Test
    void builder_addLabel_string() {
        var axis = Axis.builder()
            .addLabel("First")
            .addLabel("Second")
            .build();

        assertThat(axis.labels()).hasSize(2);
        assertThat(axis.labels().get(0).content()).isEqualTo("First");
        assertThat(axis.labels().get(1).content()).isEqualTo("Second");
    }

    @Test
    void builder_addLabel_span() {
        var span = Span.styled("Label", Style.EMPTY.fg(Color.CYAN));

        var axis = Axis.builder()
            .addLabel(span)
            .build();

        assertThat(axis.labels()).hasSize(1);
        assertThat(axis.labels().get(0)).isEqualTo(span);
    }

    @Test
    void builder_style() {
        var style = Style.EMPTY.fg(Color.MAGENTA);

        var axis = Axis.builder()
            .style(style)
            .build();

        assertThat(axis.style()).isEqualTo(style);
    }

    @Test
    void style_returns_empty_when_null() {
        var axis = Axis.builder().build();

        assertThat(axis.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    void builder_labelsAlignment() {
        var axis = Axis.builder()
            .labelsAlignment(Alignment.CENTER)
            .build();

        assertThat(axis.labelsAlignment()).isEqualTo(Alignment.CENTER);
    }

    @Test
    void builder_labelsAlignment_null_defaults_to_left() {
        var axis = Axis.builder()
            .labelsAlignment(null)
            .build();

        assertThat(axis.labelsAlignment()).isEqualTo(Alignment.LEFT);
    }

    @Test
    void bounds_returns_clone() {
        var axis = Axis.builder()
            .bounds(0, 100)
            .build();

        var bounds = axis.bounds();
        bounds[0] = 999;

        // Original should not be modified
        assertThat(axis.min()).isEqualTo(0.0);
    }

    @Test
    void labels_returns_immutable_list() {
        var axis = Axis.builder()
            .labels("A", "B", "C")
            .build();

        var labels = axis.labels();

        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> labels.add(Span.raw("D"))
        );
    }
}

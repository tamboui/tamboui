/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.chart;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Alignment;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

import static org.assertj.core.api.Assertions.assertThat;

class AxisTest {

    @Test
    void defaults_creates_axis_with_zero_bounds() {
        Axis axis = Axis.defaults();

        assertThat(axis.min()).isEqualTo(0.0);
        assertThat(axis.max()).isEqualTo(0.0);
        assertThat(axis.range()).isEqualTo(0.0);
        assertThat(axis.hasLabels()).isFalse();
        assertThat(axis.title()).isEmpty();
    }

    @Test
    void builder_title_string() {
        Axis axis = Axis.builder()
            .title("X Axis")
            .build();

        assertThat(axis.title()).isPresent();
        assertThat(axis.title().get().rawContent()).isEqualTo("X Axis");
    }

    @Test
    void builder_title_line() {
        Line line = Line.from("Y Axis");
        Axis axis = Axis.builder()
            .title(line)
            .build();

        assertThat(axis.title()).isPresent();
        assertThat(axis.title().get()).isEqualTo(line);
    }

    @Test
    void builder_title_spans() {
        Axis axis = Axis.builder()
            .title(Span.styled("X", Style.EMPTY.fg(Color.RED)), Span.raw(" Axis"))
            .build();

        assertThat(axis.title()).isPresent();
        assertThat(axis.title().get().rawContent()).isEqualTo("X Axis");
    }

    @Test
    void builder_title_null_clears_title() {
        Axis axis = Axis.builder()
            .title("Test")
            .title((String) null)
            .build();

        assertThat(axis.title()).isEmpty();
    }

    @Test
    void builder_bounds_min_max() {
        Axis axis = Axis.builder()
            .bounds(0, 100)
            .build();

        assertThat(axis.min()).isEqualTo(0.0);
        assertThat(axis.max()).isEqualTo(100.0);
        assertThat(axis.range()).isEqualTo(100.0);
    }

    @Test
    void builder_bounds_array() {
        Axis axis = Axis.builder()
            .bounds(new double[] {-50, 50})
            .build();

        assertThat(axis.min()).isEqualTo(-50.0);
        assertThat(axis.max()).isEqualTo(50.0);
        assertThat(axis.range()).isEqualTo(100.0);
    }

    @Test
    void builder_bounds_null_keeps_default() {
        Axis axis = Axis.builder()
            .bounds(null)
            .build();

        assertThat(axis.min()).isEqualTo(0.0);
        assertThat(axis.max()).isEqualTo(0.0);
    }

    @Test
    void builder_labels_strings() {
        Axis axis = Axis.builder()
            .labels("0", "25", "50", "75", "100")
            .build();

        assertThat(axis.hasLabels()).isTrue();
        assertThat(axis.labels()).hasSize(5);
        assertThat(axis.labels().get(0).content()).isEqualTo("0");
        assertThat(axis.labels().get(4).content()).isEqualTo("100");
    }

    @Test
    void builder_labels_spans() {
        Span span1 = Span.styled("Min", Style.EMPTY.fg(Color.GREEN));
        Span span2 = Span.styled("Max", Style.EMPTY.fg(Color.RED));

        Axis axis = Axis.builder()
            .labels(span1, span2)
            .build();

        assertThat(axis.labels()).hasSize(2);
        assertThat(axis.labels().get(0)).isEqualTo(span1);
        assertThat(axis.labels().get(1)).isEqualTo(span2);
    }

    @Test
    void builder_labels_list() {
        List<Span> labels = Arrays.asList(Span.raw("A"), Span.raw("B"), Span.raw("C"));

        Axis axis = Axis.builder()
            .labels(labels)
            .build();

        assertThat(axis.labels()).hasSize(3);
    }

    @Test
    void builder_labels_null_clears_labels() {
        Axis axis = Axis.builder()
            .labels("1", "2", "3")
            .labels((String[]) null)
            .build();

        assertThat(axis.hasLabels()).isFalse();
    }

    @Test
    void builder_addLabel_string() {
        Axis axis = Axis.builder()
            .addLabel("First")
            .addLabel("Second")
            .build();

        assertThat(axis.labels()).hasSize(2);
        assertThat(axis.labels().get(0).content()).isEqualTo("First");
        assertThat(axis.labels().get(1).content()).isEqualTo("Second");
    }

    @Test
    void builder_addLabel_span() {
        Span span = Span.styled("Label", Style.EMPTY.fg(Color.CYAN));

        Axis axis = Axis.builder()
            .addLabel(span)
            .build();

        assertThat(axis.labels()).hasSize(1);
        assertThat(axis.labels().get(0)).isEqualTo(span);
    }

    @Test
    void builder_style() {
        Style style = Style.EMPTY.fg(Color.MAGENTA);

        Axis axis = Axis.builder()
            .style(style)
            .build();

        assertThat(axis.style()).isEqualTo(style);
    }

    @Test
    void style_returns_empty_when_null() {
        Axis axis = Axis.builder().build();

        assertThat(axis.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    void builder_labelsAlignment() {
        Axis axis = Axis.builder()
            .labelsAlignment(Alignment.CENTER)
            .build();

        assertThat(axis.labelsAlignment()).isEqualTo(Alignment.CENTER);
    }

    @Test
    void builder_labelsAlignment_null_defaults_to_left() {
        Axis axis = Axis.builder()
            .labelsAlignment(null)
            .build();

        assertThat(axis.labelsAlignment()).isEqualTo(Alignment.LEFT);
    }

    @Test
    void bounds_returns_clone() {
        Axis axis = Axis.builder()
            .bounds(0, 100)
            .build();

        double[] bounds = axis.bounds();
        bounds[0] = 999;

        // Original should not be modified
        assertThat(axis.min()).isEqualTo(0.0);
    }

    @Test
    void labels_returns_immutable_list() {
        Axis axis = Axis.builder()
            .labels("A", "B", "C")
            .build();

        List<Span> labels = axis.labels();

        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> labels.add(Span.raw("D"))
        );
    }
}

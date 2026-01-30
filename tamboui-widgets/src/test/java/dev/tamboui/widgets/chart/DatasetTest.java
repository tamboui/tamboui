/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.chart;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;

import static org.assertj.core.api.Assertions.assertThat;

class DatasetTest {

    @Test
    void builder_creates_dataset_with_defaults() {
        Dataset dataset = Dataset.builder().build();

        assertThat(dataset.name()).isEmpty();
        assertThat(dataset.data()).isEmpty();
        assertThat(dataset.marker()).isEqualTo(Dataset.Marker.DOT);
        assertThat(dataset.graphType()).isEqualTo(GraphType.SCATTER);
        assertThat(dataset.style()).isEqualTo(Style.EMPTY);
        assertThat(dataset.hasName()).isFalse();
        assertThat(dataset.size()).isEqualTo(0);
    }

    @Test
    void builder_name_string() {
        Dataset dataset = Dataset.builder().name("Test Dataset").build();

        assertThat(dataset.hasName()).isTrue();
        assertThat(dataset.name()).isPresent();
        assertThat(dataset.name().get().rawContent()).isEqualTo("Test Dataset");
    }

    @Test
    void builder_name_line() {
        Line line = Line.from("Dataset Name");
        Dataset dataset = Dataset.builder().name(line).build();

        assertThat(dataset.name()).isPresent();
        assertThat(dataset.name().get()).isEqualTo(line);
    }

    @Test
    void builder_name_null_clears_name() {
        Dataset dataset = Dataset.builder().name("Test").name((String) null).build();

        assertThat(dataset.hasName()).isFalse();
        assertThat(dataset.name()).isEmpty();
    }

    @Test
    void builder_data_array() {
        double[][] data = new double[][]{{0, 0}, {1, 1}, {2, 4}};
        Dataset dataset = Dataset.builder().data(data).build();

        assertThat(dataset.size()).isEqualTo(3);
        assertThat(dataset.data()[0]).isEqualTo(new double[]{0, 0});
        assertThat(dataset.data()[1]).isEqualTo(new double[]{1, 1});
        assertThat(dataset.data()[2]).isEqualTo(new double[]{2, 4});
    }

    @Test
    void builder_data_clones_input() {
        double[][] data = new double[][]{{0, 0}, {1, 1}};
        Dataset dataset = Dataset.builder().data(data).build();

        // Modify original
        data[0][0] = 999;

        // Dataset should not be affected
        assertThat(dataset.data()[0][0]).isEqualTo(0);
    }

    @Test
    void builder_data_null_creates_empty() {
        Dataset dataset = Dataset.builder().data((double[][]) null).build();

        assertThat(dataset.data()).isEmpty();
        assertThat(dataset.size()).isEqualTo(0);
    }

    @Test
    void builder_data_list() {
        List<double[]> data = Arrays.asList(new double[]{0, 0}, new double[]{1, 2},
                new double[]{2, 4});
        Dataset dataset = Dataset.builder().data(data).build();

        assertThat(dataset.size()).isEqualTo(3);
    }

    @Test
    void builder_data_empty_list() {
        Dataset dataset = Dataset.builder().data(Arrays.asList()).build();

        assertThat(dataset.data()).isEmpty();
    }

    @Test
    void builder_data_null_list() {
        Dataset dataset = Dataset.builder().data((List<double[]>) null).build();

        assertThat(dataset.data()).isEmpty();
    }

    @Test
    void builder_addPoint() {
        Dataset dataset = Dataset.builder().addPoint(0, 0).addPoint(1, 1).addPoint(2, 4).build();

        assertThat(dataset.size()).isEqualTo(3);
        assertThat(dataset.data()[0]).isEqualTo(new double[]{0, 0});
        assertThat(dataset.data()[1]).isEqualTo(new double[]{1, 1});
        assertThat(dataset.data()[2]).isEqualTo(new double[]{2, 4});
    }

    @Test
    void builder_marker_dot() {
        Dataset dataset = Dataset.builder().marker(Dataset.Marker.DOT).build();

        assertThat(dataset.marker()).isEqualTo(Dataset.Marker.DOT);
        assertThat(dataset.marker().symbol()).isEqualTo("•");
    }

    @Test
    void builder_marker_block() {
        Dataset dataset = Dataset.builder().marker(Dataset.Marker.BLOCK).build();

        assertThat(dataset.marker()).isEqualTo(Dataset.Marker.BLOCK);
        assertThat(dataset.marker().symbol()).isEqualTo("█");
    }

    @Test
    void builder_marker_bar() {
        Dataset dataset = Dataset.builder().marker(Dataset.Marker.BAR).build();

        assertThat(dataset.marker()).isEqualTo(Dataset.Marker.BAR);
        assertThat(dataset.marker().symbol()).isEqualTo("▄");
    }

    @Test
    void builder_marker_braille() {
        Dataset dataset = Dataset.builder().marker(Dataset.Marker.BRAILLE).build();

        assertThat(dataset.marker()).isEqualTo(Dataset.Marker.BRAILLE);
        assertThat(dataset.marker().symbol()).isEqualTo("⣿");
    }

    @Test
    void builder_marker_null_defaults_to_dot() {
        Dataset dataset = Dataset.builder().marker(Dataset.Marker.BLOCK).marker(null).build();

        assertThat(dataset.marker()).isEqualTo(Dataset.Marker.DOT);
    }

    @Test
    void builder_graphType_scatter() {
        Dataset dataset = Dataset.builder().graphType(GraphType.SCATTER).build();

        assertThat(dataset.graphType()).isEqualTo(GraphType.SCATTER);
    }

    @Test
    void builder_graphType_line() {
        Dataset dataset = Dataset.builder().graphType(GraphType.LINE).build();

        assertThat(dataset.graphType()).isEqualTo(GraphType.LINE);
    }

    @Test
    void builder_graphType_bar() {
        Dataset dataset = Dataset.builder().graphType(GraphType.BAR).build();

        assertThat(dataset.graphType()).isEqualTo(GraphType.BAR);
    }

    @Test
    void builder_graphType_null_defaults_to_scatter() {
        Dataset dataset = Dataset.builder().graphType(GraphType.LINE).graphType(null).build();

        assertThat(dataset.graphType()).isEqualTo(GraphType.SCATTER);
    }

    @Test
    void builder_style() {
        Style style = Style.EMPTY.fg(Color.RED).bold();
        Dataset dataset = Dataset.builder().style(style).build();

        assertThat(dataset.style()).isEqualTo(style);
    }

    @Test
    void style_returns_empty_when_null() {
        Dataset dataset = Dataset.builder().build();

        assertThat(dataset.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    void of_data_only() {
        double[][] data = new double[][]{{0, 0}, {1, 1}};
        Dataset dataset = Dataset.of(data);

        assertThat(dataset.size()).isEqualTo(2);
        assertThat(dataset.hasName()).isFalse();
        assertThat(dataset.graphType()).isEqualTo(GraphType.SCATTER);
    }

    @Test
    void of_name_and_data() {
        double[][] data = new double[][]{{0, 0}, {1, 1}};
        Dataset dataset = Dataset.of("Test", data);

        assertThat(dataset.size()).isEqualTo(2);
        assertThat(dataset.hasName()).isTrue();
        assertThat(dataset.name().get().rawContent()).isEqualTo("Test");
    }

    @Test
    void marker_enum_values() {
        assertThat(Dataset.Marker.values()).hasSize(4);
        assertThat(Dataset.Marker.DOT.symbol()).isEqualTo("•");
        assertThat(Dataset.Marker.BLOCK.symbol()).isEqualTo("█");
        assertThat(Dataset.Marker.BAR.symbol()).isEqualTo("▄");
        assertThat(Dataset.Marker.BRAILLE.symbol()).isEqualTo("⣿");
    }
}

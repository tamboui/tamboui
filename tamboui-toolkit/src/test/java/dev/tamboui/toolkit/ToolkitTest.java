/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.BarChartElement;
import dev.tamboui.toolkit.elements.CalendarElement;
import dev.tamboui.toolkit.elements.CanvasElement;
import dev.tamboui.toolkit.elements.ChartElement;
import dev.tamboui.toolkit.elements.Column;
import dev.tamboui.toolkit.elements.GaugeElement;
import dev.tamboui.toolkit.elements.LineGaugeElement;
import dev.tamboui.toolkit.elements.ListElement;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.elements.Row;
import dev.tamboui.toolkit.elements.ScrollbarElement;
import dev.tamboui.toolkit.elements.Spacer;
import dev.tamboui.toolkit.elements.SparklineElement;
import dev.tamboui.toolkit.elements.TableElement;
import dev.tamboui.toolkit.elements.TabsElement;
import dev.tamboui.toolkit.elements.TextElement;
import dev.tamboui.toolkit.elements.TextInputElement;
import dev.tamboui.layout.Constraint;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.scrollbar.ScrollbarState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the Dsl factory methods.
 */
class ToolkitTest {

    @Nested
    @DisplayName("Text factory methods")
    class TextFactoryTests {

        @Test
        @DisplayName("text(String) creates TextElement")
        void textWithString() {
            TextElement element = text("Hello");
            assertThat(element).isInstanceOf(TextElement.class);
        }

        @Test
        @DisplayName("text(Object) creates TextElement with toString")
        void textWithObject() {
            TextElement element = text(42);
            assertThat(element).isInstanceOf(TextElement.class);
        }

        @Test
        @DisplayName("text(Object...) concatenates values")
        void textWithMultipleValues() {
            TextElement element = text("Count: ", 42, " items");
            assertThat(element).isInstanceOf(TextElement.class);
        }
    }

    @Nested
    @DisplayName("Container factory methods")
    class ContainerFactoryTests {

        @Test
        @DisplayName("panel() creates empty Panel")
        void emptyPanel() {
            Panel element = panel();
            assertThat(element).isInstanceOf(Panel.class);
        }

        @Test
        @DisplayName("panel(String, Element...) creates Panel with title and children")
        void panelWithTitleAndChildren() {
            Panel element = panel("Title", text("content"));
            assertThat(element).isInstanceOf(Panel.class);
        }

        @Test
        @DisplayName("row() creates empty Row")
        void emptyRow() {
            Row element = row();
            assertThat(element).isInstanceOf(Row.class);
        }

        @Test
        @DisplayName("row(Element...) creates Row with children")
        void rowWithChildren() {
            Row element = row(text("left"), text("right"));
            assertThat(element).isInstanceOf(Row.class);
        }

        @Test
        @DisplayName("column() creates empty Column")
        void emptyColumn() {
            Column element = column();
            assertThat(element).isInstanceOf(Column.class);
        }

        @Test
        @DisplayName("column(Element...) creates Column with children")
        void columnWithChildren() {
            Column element = column(text("top"), text("bottom"));
            assertThat(element).isInstanceOf(Column.class);
        }

        @Test
        @DisplayName("spacer() creates fill Spacer")
        void fillSpacer() {
            Spacer element = spacer();
            assertThat(element).isInstanceOf(Spacer.class);
            assertThat(element.constraint()).isEqualTo(Constraint.fill());
        }

        @Test
        @DisplayName("spacer(int) creates fixed Spacer")
        void fixedSpacer() {
            Spacer element = spacer(5);
            assertThat(element).isInstanceOf(Spacer.class);
            assertThat(element.constraint()).isEqualTo(Constraint.length(5));
        }
    }

    @Nested
    @DisplayName("Constraint factory methods")
    class ConstraintFactoryTests {

        @Test
        @DisplayName("length(int) creates length constraint")
        void lengthConstraint() {
            assertThat(length(10)).isEqualTo(Constraint.length(10));
        }

        @Test
        @DisplayName("percent(int) creates percentage constraint")
        void percentConstraint() {
            assertThat(percent(50)).isEqualTo(Constraint.percentage(50));
        }

        @Test
        @DisplayName("fill() creates fill constraint")
        void fillConstraint() {
            assertThat(fill()).isEqualTo(Constraint.fill());
        }

        @Test
        @DisplayName("fill(int) creates weighted fill constraint")
        void weightedFillConstraint() {
            assertThat(fill(2)).isEqualTo(Constraint.fill(2));
        }

        @Test
        @DisplayName("min(int) creates minimum constraint")
        void minConstraint() {
            assertThat(min(5)).isEqualTo(Constraint.min(5));
        }

        @Test
        @DisplayName("max(int) creates maximum constraint")
        void maxConstraint() {
            assertThat(max(20)).isEqualTo(Constraint.max(20));
        }

        @Test
        @DisplayName("ratio(int, int) creates ratio constraint")
        void ratioConstraint() {
            assertThat(ratio(1, 3)).isEqualTo(Constraint.ratio(1, 3));
        }
    }

    @Nested
    @DisplayName("Gauge factory methods")
    class GaugeFactoryTests {

        @Test
        @DisplayName("gauge() creates empty GaugeElement")
        void emptyGauge() {
            GaugeElement element = gauge();
            assertThat(element).isInstanceOf(GaugeElement.class);
        }

        @Test
        @DisplayName("gauge(double) creates GaugeElement with ratio")
        void gaugeWithRatio() {
            GaugeElement element = gauge(0.75);
            assertThat(element).isInstanceOf(GaugeElement.class);
        }

        @Test
        @DisplayName("gauge(int) creates GaugeElement with percent")
        void gaugeWithPercent() {
            GaugeElement element = gauge(75);
            assertThat(element).isInstanceOf(GaugeElement.class);
        }
    }

    @Nested
    @DisplayName("LineGauge factory methods")
    class LineGaugeFactoryTests {

        @Test
        @DisplayName("lineGauge() creates empty LineGaugeElement")
        void emptyLineGauge() {
            LineGaugeElement element = lineGauge();
            assertThat(element).isInstanceOf(LineGaugeElement.class);
        }

        @Test
        @DisplayName("lineGauge(double) creates LineGaugeElement with ratio")
        void lineGaugeWithRatio() {
            LineGaugeElement element = lineGauge(0.5);
            assertThat(element).isInstanceOf(LineGaugeElement.class);
        }

        @Test
        @DisplayName("lineGauge(int) creates LineGaugeElement with percent")
        void lineGaugeWithPercent() {
            LineGaugeElement element = lineGauge(50);
            assertThat(element).isInstanceOf(LineGaugeElement.class);
        }
    }

    @Nested
    @DisplayName("Sparkline factory methods")
    class SparklineFactoryTests {

        @Test
        @DisplayName("sparkline() creates empty SparklineElement")
        void emptySparkline() {
            SparklineElement element = sparkline();
            assertThat(element).isInstanceOf(SparklineElement.class);
        }

        @Test
        @DisplayName("sparkline(long...) creates SparklineElement with data")
        void sparklineWithLongData() {
            SparklineElement element = sparkline(1L, 2L, 3L, 4L, 5L);
            assertThat(element).isInstanceOf(SparklineElement.class);
        }

        @Test
        @DisplayName("sparkline(int...) creates SparklineElement with data")
        void sparklineWithIntData() {
            SparklineElement element = sparkline(1, 2, 3, 4, 5);
            assertThat(element).isInstanceOf(SparklineElement.class);
        }

        @Test
        @DisplayName("sparkline(Collection) creates SparklineElement with data")
        void sparklineWithCollection() {
            SparklineElement element = sparkline(Arrays.asList(1, 2, 3));
            assertThat(element).isInstanceOf(SparklineElement.class);
        }
    }

    @Nested
    @DisplayName("List factory methods")
    class ListFactoryTests {

        @Test
        @DisplayName("list() creates empty ListElement")
        void emptyList() {
            ListElement<?> element = list();
            assertThat(element).isInstanceOf(ListElement.class);
        }

        @Test
        @DisplayName("list(String...) creates ListElement with items")
        void listWithItems() {
            ListElement<?> element = list("Item 1", "Item 2", "Item 3");
            assertThat(element).isInstanceOf(ListElement.class);
        }

        @Test
        @DisplayName("list(List<String>) creates ListElement with items")
        void listWithItemsList() {
            ListElement<?> element = list(Arrays.asList("A", "B", "C"));
            assertThat(element).isInstanceOf(ListElement.class);
        }
    }

    @Nested
    @DisplayName("Table factory methods")
    class TableFactoryTests {

        @Test
        @DisplayName("table() creates empty TableElement")
        void emptyTable() {
            TableElement element = table();
            assertThat(element).isInstanceOf(TableElement.class);
        }
    }

    @Nested
    @DisplayName("Tabs factory methods")
    class TabsFactoryTests {

        @Test
        @DisplayName("tabs() creates empty TabsElement")
        void emptyTabs() {
            TabsElement element = tabs();
            assertThat(element).isInstanceOf(TabsElement.class);
        }

        @Test
        @DisplayName("tabs(String...) creates TabsElement with titles")
        void tabsWithTitles() {
            TabsElement element = tabs("Home", "Settings", "About");
            assertThat(element).isInstanceOf(TabsElement.class);
        }

        @Test
        @DisplayName("tabs(List<String>) creates TabsElement with titles")
        void tabsWithTitlesList() {
            TabsElement element = tabs(Arrays.asList("Tab 1", "Tab 2"));
            assertThat(element).isInstanceOf(TabsElement.class);
        }
    }

    @Nested
    @DisplayName("TextInput factory methods")
    class TextInputFactoryTests {

        @Test
        @DisplayName("textInput() creates TextInputElement with new state")
        void emptyTextInput() {
            TextInputElement element = textInput();
            assertThat(element).isInstanceOf(TextInputElement.class);
        }

        @Test
        @DisplayName("textInput(TextInputState) creates TextInputElement with state")
        void textInputWithState() {
            TextInputState state = new TextInputState();
            TextInputElement element = textInput(state);
            assertThat(element).isInstanceOf(TextInputElement.class);
        }
    }

    @Nested
    @DisplayName("BarChart factory methods")
    class BarChartFactoryTests {

        @Test
        @DisplayName("barChart() creates empty BarChartElement")
        void emptyBarChart() {
            BarChartElement element = barChart();
            assertThat(element).isInstanceOf(BarChartElement.class);
        }

        @Test
        @DisplayName("barChart(long...) creates BarChartElement with data")
        void barChartWithData() {
            BarChartElement element = barChart(10L, 20L, 30L);
            assertThat(element).isInstanceOf(BarChartElement.class);
        }
    }

    @Nested
    @DisplayName("Chart factory methods")
    class ChartFactoryTests {

        @Test
        @DisplayName("chart() creates empty ChartElement")
        void emptyChart() {
            ChartElement element = chart();
            assertThat(element).isInstanceOf(ChartElement.class);
        }
    }

    @Nested
    @DisplayName("Canvas factory methods")
    class CanvasFactoryTests {

        @Test
        @DisplayName("canvas() creates CanvasElement with default bounds")
        void emptyCanvas() {
            CanvasElement element = canvas();
            assertThat(element).isInstanceOf(CanvasElement.class);
        }

        @Test
        @DisplayName("canvas(bounds) creates CanvasElement with custom bounds")
        void canvasWithBounds() {
            CanvasElement element = canvas(-10, 10, -10, 10);
            assertThat(element).isInstanceOf(CanvasElement.class);
        }
    }

    @Nested
    @DisplayName("Calendar factory methods")
    class CalendarFactoryTests {

        @Test
        @DisplayName("calendar() creates CalendarElement for current month")
        void calendarForCurrentMonth() {
            CalendarElement element = calendar();
            assertThat(element).isInstanceOf(CalendarElement.class);
        }

        @Test
        @DisplayName("calendar(LocalDate) creates CalendarElement for specific month")
        void calendarForSpecificMonth() {
            CalendarElement element = calendar(LocalDate.of(2025, 6, 15));
            assertThat(element).isInstanceOf(CalendarElement.class);
        }
    }

    @Nested
    @DisplayName("Scrollbar factory methods")
    class ScrollbarFactoryTests {

        @Test
        @DisplayName("scrollbar() creates ScrollbarElement")
        void emptyScrollbar() {
            ScrollbarElement element = scrollbar();
            assertThat(element).isInstanceOf(ScrollbarElement.class);
        }

        @Test
        @DisplayName("scrollbar(ScrollbarState) creates ScrollbarElement with state")
        void scrollbarWithState() {
            ScrollbarState state = new ScrollbarState().contentLength(100);
            ScrollbarElement element = scrollbar(state);
            assertThat(element).isInstanceOf(ScrollbarElement.class);
        }

        @Test
        @DisplayName("scrollbar(int, int, int) creates ScrollbarElement with parameters")
        void scrollbarWithParameters() {
            ScrollbarElement element = scrollbar(100, 20, 50);
            assertThat(element).isInstanceOf(ScrollbarElement.class);
        }
    }

    @Nested
    @DisplayName("Lazy elements")
    class LazyElementTests {

        @Test
        @DisplayName("lazy(Supplier) creates lazy element")
        void lazyElement() {
            Element element = lazy(() -> text("Dynamic"));
            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("panel with Supplier creates lazy content")
        void panelWithLazyContent() {
            int[] counter = {0};
            Panel element = panel("Counter", () -> text("Count: " + (++counter[0])));
            assertThat(element).isInstanceOf(Panel.class);
        }
    }
}

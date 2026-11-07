/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.widgets.table.TableState;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TableElement.
 */
class TableElementTest extends AbstractElementTest {

    @Test
    @DisplayName("TableElement fluent API chains correctly")
    void fluentApiChaining() {
        TableState state = new TableState();
        TableElement element = table()
            .header("Name", "Age", "City")
            .row("Alice", "30", "NYC")
            .row("Bob", "25", "LA")
            .state(state)
            .widths(Constraint.percentage(40), Constraint.length(10), Constraint.fill())
            .highlightColor(Color.YELLOW)
            .title("Users")
            .rounded()
            .borderColor(Color.CYAN);

        assertThat(element).isInstanceOf(TableElement.class);
    }

    @Test
    @DisplayName("table() creates empty element")
    void emptyTable() {
        TableElement element = table();
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("header() sets column headers")
    void headerMethod() {
        TableElement element = table().header("Col1", "Col2", "Col3");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("row() adds data rows")
    void rowMethod() {
        TableElement element = table()
            .header("Name", "Value")
            .row("A", "1")
            .row("B", "2")
            .row("C", "3");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("widths() sets column widths")
    void widthsMethod() {
        TableElement element = table()
            .widths(Constraint.length(10), Constraint.fill());
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("TableElement renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        TableState state = new TableState();

        table()
            .header("Name", "Age")
            .row("Alice", "30")
            .row("Bob", "25")
            .state(state)
            .widths(Constraint.fill(), Constraint.length(10))
            .title("Table")
            .rounded()
            .render(frame, area, RenderContext.empty());

        // Check border is rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
    }

    @Test
    @DisplayName("TableElement with selection")
    void withSelection() {
        Rect area = new Rect(0, 0, 30, 8);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        TableState state = new TableState();
        state.select(1);

        table()
            .header("X", "Y")
            .row("A", "1")
            .row("B", "2")
            .state(state)
            .highlightColor(Color.GREEN)
            .render(frame, area, RenderContext.empty());

        // Should render without error
        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 10));
        Frame frame = Frame.forTesting(buffer);
        TableState state = new TableState();

        // Should not throw
        table()
            .header("A")
            .row("1")
            .state(state)
            .render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("TableElement without explicit state creates internal state")
    void internalState() {
        Rect area = new Rect(0, 0, 30, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Should not throw even without state
        table()
            .header("Col")
            .row("Value")
            .render(frame, area, RenderContext.empty());
    }

    @Test
    @DisplayName("highlightSymbol sets selection indicator")
    void highlightSymbol() {
        TableElement element = table()
            .highlightSymbol("→ ");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("columnSpacing sets gap between columns")
    void columnSpacing() {
        TableElement element = table()
            .columnSpacing(2);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("footer() sets table footer")
    void footerMethod() {
        TableElement element = table()
            .header("H1", "H2")
            .footer("Total", "100");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("styleAttributes exposes title")
    void styleAttributes_exposesTitle() {
        assertThat(table().title("Data").styleAttributes()).containsEntry("title", "Data");
    }

    @Test
    @DisplayName("preferredHeight returns content height for table elements")
    void preferredHeightReturnsContentHeight() {
        TableElement element = table()
            .header("Name", "Age")
            .row("Alice", "30")
            .row("Bob", "25");

        // 1 header row + 2 data rows = 3
        assertThat(element.preferredSize(-1, -1, null).heightOr(0)).isEqualTo(3);
        assertThat(element.preferredSize(80, -1, RenderContext.empty()).heightOr(0)).isEqualTo(3);
    }

    @Test
    @DisplayName("Attribute selector [title] affects Table border color")
    void attributeSelector_title_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "TableElement[title=\"Users\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        table().header("Name", "Age").row("Alice", "30").title("Users").rounded().render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("columnAlignments configures alignments and renders aligned content")
    void columnAlignmentsConfigurationAndRender() {
        TableElement table = table()
            .header("A", "B")
            .row("1", "2")
            .columnAlignments(Alignment.LEFT, Alignment.RIGHT)
            .columnSpacing(0)
            .highlightSymbol("")
            .widths(Constraint.length(5), Constraint.length(5));

        assertThat(table.columnAlignments()).containsExactly(Alignment.LEFT, Alignment.RIGHT);

        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        table.render(frame, area, RenderContext.empty());

        // Header row: "A" at 0, "B" right-aligned at 5 + 4 = 9
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("B");

        // Data row: "1" at 0, "2" right-aligned at 9
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("1");
        assertThat(buffer.get(9, 1).symbol()).isEqualTo("2");
    }

    @Test
    @DisplayName("columnAlignment sets alignment for individual column index")
    void individualColumnAlignment() {
        TableElement table = table()
            .columnAlignment(1, Alignment.RIGHT);

        assertThat(table.columnAlignments()).hasSize(2);
        assertThat(table.columnAlignments().get(0)).isNull();
        assertThat(table.columnAlignments().get(1)).isEqualTo(Alignment.RIGHT);
    }
}

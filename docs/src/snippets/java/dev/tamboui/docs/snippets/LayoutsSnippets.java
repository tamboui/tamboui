package dev.tamboui.docs.snippets;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.ContentAlignment;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.columns.ColumnOrder;
import dev.tamboui.layout.columns.Columns;
import dev.tamboui.layout.dock.Dock;
import dev.tamboui.layout.flow.Flow;
import dev.tamboui.layout.grid.Grid;
import dev.tamboui.layout.stack.Stack;
import dev.tamboui.widget.Widget;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Code snippets for layouts.adoc documentation.
 * Each method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings({"unused", "UnnecessaryLocalVariable"})
public class LayoutsSnippets {

    // Placeholder widgets for examples
    Widget widget1, widget2, widget3, widget4, widget5, widget6;
    Widget headerWidget, contentWidget, sidebarWidget, footerWidget;
    Widget article1, article2, article3, article4;
    Widget cpuPanel, memoryPanel, diskPanel, netUpPanel, netDownPanel, uptimePanel;
    Widget header, content, sidebar, footer;
    Widget a, b, c, d, e, f;
    Widget navWidget, mainWidget;
    Widget mainPanel, sidePanel1, sidePanel2, statusBar;
    Widget statusBarWidget, outlineWidget, editorWidget;
    Widget backgroundWidget, dialogWidget;
    Widget tag1Widget, tag2Widget, tag3Widget;
    Rect area;
    Buffer buffer;

    void columnsBasic() {
        // tag::columns-basic[]
        Columns columns = Columns.builder()
            .children(widget1, widget2, widget3, widget4)
            .columnCount(2)
            .spacing(1)
            .order(ColumnOrder.ROW_FIRST)
            .build();

        columns.render(area, buffer);
        // end::columns-basic[]
    }

    void columnsAdvanced() {
        // tag::columns-advanced[]
        // Custom column widths and row-first ordering
        Columns grid = Columns.builder()
            .children(headerWidget, contentWidget, sidebarWidget, footerWidget)
            .columnCount(2)
            .columnWidths(Constraint.length(20), Constraint.fill())
            .rowHeights(3, 10)
            .spacing(1)
            .build();

        // Column-first ordering: fills top-to-bottom per column
        Columns newspaperLayout = Columns.builder()
            .children(article1, article2, article3, article4)
            .columnCount(2)
            .order(ColumnOrder.COLUMN_FIRST)
            .build();
        // end::columns-advanced[]
    }

    void gridChildrenBasic() {
        // tag::grid-children-basic[]
        Grid grid = Grid.builder()
            .children(widget1, widget2, widget3, widget4, widget5, widget6)
            .columnCount(3)
            .horizontalGutter(1)
            .verticalGutter(1)
            .build();

        grid.render(area, buffer);
        // end::grid-children-basic[]
    }

    void gridChildrenAdvanced() {
        // tag::grid-children-advanced[]
        // Grid with custom column widths and gutter
        Grid dashboard = Grid.builder()
            .children(cpuPanel, memoryPanel, diskPanel,
                      netUpPanel, netDownPanel, uptimePanel)
            .columnCount(3)
            .columnConstraints(
                Constraint.length(16),  // fixed first column
                Constraint.fill(),      // remaining columns fill
                Constraint.fill()
            )
            .horizontalGutter(2)
            .verticalGutter(1)
            .build();

        // Grid with row constraints
        Grid sized = Grid.builder()
            .children(header, content, sidebar, footer)
            .columnCount(2)
            .rowConstraints(Constraint.length(3), Constraint.fill())
            .build();

        // Column constraint cycling: single constraint applied to all columns
        Grid uniform = Grid.builder()
            .children(a, b, c, d, e, f)
            .columnCount(3)
            .columnConstraints(Constraint.length(10))  // all 3 cols get length(10)
            .build();
        // end::grid-children-advanced[]
    }

    void gridAreaBasic() {
        // tag::grid-area-basic[]
        // "Holy grail" layout with spanning regions
        Grid layout = Grid.builder()
            .gridAreas("header header header",
                       "nav    main   main",
                       "nav    main   main",
                       "footer footer footer")
            .area("header", headerWidget)
            .area("nav", navWidget)
            .area("main", mainWidget)
            .area("footer", footerWidget)
            .horizontalGutter(1)
            .verticalGutter(1)
            .build();

        layout.render(area, buffer);
        // end::grid-area-basic[]
    }

    void gridAreaAdvanced() {
        // tag::grid-area-advanced[]
        // Dashboard with 2x2 spanning main area
        Grid dashboard = Grid.builder()
            .gridAreas("A A B",
                       "A A C",
                       "D D D")
            .area("A", mainPanel)    // 2x2 span
            .area("B", sidePanel1)
            .area("C", sidePanel2)
            .area("D", statusBar)    // full-width span
            .horizontalGutter(1)
            .verticalGutter(1)
            .build();

        // Empty cells with dot notation
        Grid sparse = Grid.builder()
            .gridAreas("A . B",
                       ". C .")
            .area("A", widget1)
            .area("B", widget2)
            .area("C", widget3)
            .build();
        // end::grid-area-advanced[]
    }

    void dockExample() {
        // tag::dock[]
        Dock dock = Dock.builder()
            .top(headerWidget)
            .bottom(statusBarWidget)
            .left(sidebarWidget)
            .right(outlineWidget)
            .center(editorWidget)
            .topHeight(Constraint.length(3))
            .bottomHeight(Constraint.length(1))
            .leftWidth(Constraint.length(20))
            .rightWidth(Constraint.length(20))
            .build();

        dock.render(area, buffer);
        // end::dock[]
    }

    void stackExample() {
        // tag::stack[]
        Stack stack = Stack.builder()
            .children(backgroundWidget, dialogWidget)
            .alignment(ContentAlignment.STRETCH)
            .build();

        stack.render(area, buffer);
        // end::stack[]
    }

    void flowExample() {
        // tag::flow[]
        Flow flow = Flow.builder()
            .item(tag1Widget, 8)
            .item(tag2Widget, 12)
            .item(tag3Widget, 6)
            .horizontalSpacing(1)
            .verticalSpacing(1)
            .build();

        flow.render(area, buffer);
        // end::flow[]
    }
}

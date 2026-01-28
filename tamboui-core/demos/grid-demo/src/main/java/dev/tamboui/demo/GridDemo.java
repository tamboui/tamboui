///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.layout.grid.Grid;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;

/**
 * Demo TUI application showcasing the Grid widget.
 * <p>
 * Demonstrates CSS Grid-inspired layout features:
 * - Grid with explicit column count
 * - Horizontal and vertical gutter spacing
 * - Column constraints (fixed and fill)
 * - Row constraints
 * - Flex modes for space distribution
 */
public class GridDemo {

    private boolean running = true;

    private GridDemo() {
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new GridDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            // Event loop
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                }
            }
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Header
                Constraint.fill(),     // Main content
                Constraint.length(3)   // Footer
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Grid Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into two sections: top grid and bottom grid
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        renderDashboardGrid(frame, rows.get(0));
        renderConstrainedGrid(frame, rows.get(1));
    }

    /**
     * Renders a 3-column dashboard-style grid with uniform cells and gutter.
     */
    private void renderDashboardGrid(Frame frame, Rect area) {
        Block outerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .title(Title.from(
                Line.from(
                    Span.raw(" Dashboard Grid ").bold().green(),
                    Span.raw("(3 cols, gutter 1) ").dim()
                )
            ))
            .build();

        frame.renderWidget(outerBlock, area);
        Rect inner = outerBlock.inner(area);

        Grid grid = Grid.builder()
            .children(
                metricPanel("CPU", "45%", Color.GREEN),
                metricPanel("Memory", "72%", Color.YELLOW),
                metricPanel("Disk", "23%", Color.CYAN),
                metricPanel("Net ↑", "1.2 MB/s", Color.BLUE),
                metricPanel("Net ↓", "5.7 MB/s", Color.MAGENTA),
                metricPanel("Uptime", "3d 14h", Color.WHITE)
            )
            .columnCount(3)
            .horizontalGutter(1)
            .verticalGutter(1)
            .build();

        frame.renderWidget(grid, inner);
    }

    /**
     * Renders a grid with column constraints to demonstrate mixed sizing.
     */
    private void renderConstrainedGrid(Frame frame, Rect area) {
        Block outerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .title(Title.from(
                Line.from(
                    Span.raw(" Constrained Grid ").bold().blue(),
                    Span.raw("(fixed + fill cols, gutter 2) ").dim()
                )
            ))
            .build();

        frame.renderWidget(outerBlock, area);
        Rect inner = outerBlock.inner(area);

        Grid grid = Grid.builder()
            .children(
                labelPanel("Status", "● Online", Color.GREEN),
                labelPanel("Region", "us-east-1", Color.CYAN),
                labelPanel("Alerts", "0 critical\n2 warnings", Color.YELLOW),
                labelPanel("Health", "● Healthy", Color.GREEN),
                labelPanel("Version", "v2.4.1", Color.MAGENTA),
                labelPanel("Logs", "1,234 entries\n3 errors", Color.RED)
            )
            .columnCount(3)
            .columnConstraints(
                Constraint.length(16),
                Constraint.fill(),
                Constraint.fill()
            )
            .horizontalGutter(2)
            .verticalGutter(1)
            .build();

        frame.renderWidget(grid, inner);
    }

    /**
     * Creates a metric panel widget with a bordered block, title, and value.
     */
    private static Paragraph metricPanel(String label, String value, Color color) {
        return Paragraph.builder()
            .text(Text.from(
                Line.from(Span.raw(value).bold().fg(color))
            ))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(color))
                .title(Title.from(label))
                .build())
            .build();
    }

    /**
     * Creates a label panel widget with a bordered block and multi-line text.
     */
    private static Paragraph labelPanel(String label, String value, Color color) {
        return Paragraph.builder()
            .text(Text.from(
                Line.from(Span.raw(value).fg(color))
            ))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(Title.from(
                    Line.from(Span.raw(label).bold().fg(color))
                ))
                .build())
            .build();
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw("q").bold().yellow(),
            Span.raw(" Quit").dim()
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(helpLine))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }
}

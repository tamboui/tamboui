///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;

import java.time.Duration;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing dynamic layout switching between Flow, Dock, Grid,
 * and Columns using the Toolkit DSL.
 * <p>
 * Press 1-4 to switch layouts, +/- to adjust spacing.
 */
public class LayoutDemo {

    // Varied-width labels shared across Columns, Grid, and Flow to highlight layout differences
    private static final String[] ITEMS = {
        "OK", "Cancel", "Submit Form", "X", "Help & Docs",
        "Settings", "Go", "Advanced Options", "Save",
        "Export as PDF", "Hi", "View All Results"
    };

    private static final Color[] COLORS = {
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.MAGENTA, Color.LIGHT_RED, Color.LIGHT_GREEN,
        Color.LIGHT_BLUE, Color.LIGHT_YELLOW, Color.LIGHT_CYAN, Color.LIGHT_MAGENTA
    };

    private enum LayoutMode {
        COLUMNS("Columns"),
        GRID("Grid"),
        FLOW("Flow"),
        DOCK("Dock");

        final String label;

        LayoutMode(String label) {
            this.label = label;
        }
    }

    private LayoutMode mode = LayoutMode.COLUMNS;
    private int spacing = 1;

    private LayoutDemo() {
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new LayoutDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        var config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(100))
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> column(
                // Header
                panel(row(
                    text(" Layout Demo ").bold().cyan(),
                    spacer(),
                    text(" [1] Columns ").fg(mode == LayoutMode.COLUMNS ? Color.YELLOW : Color.DARK_GRAY),
                    text(" [2] Grid ").fg(mode == LayoutMode.GRID ? Color.YELLOW : Color.DARK_GRAY),
                    text(" [3] Flow ").fg(mode == LayoutMode.FLOW ? Color.YELLOW : Color.DARK_GRAY),
                    text(" [4] Dock ").fg(mode == LayoutMode.DOCK ? Color.YELLOW : Color.DARK_GRAY),
                    spacer(),
                    text(" [+/-] Spacing:" + spacing + " ").dim(),
                    text(" [q] Quit ").dim()
                )).rounded().borderColor(Color.DARK_GRAY).length(3),

                // Content area
                panel(mode.label, renderLayout())
                    .rounded()
                    .borderColor(Color.CYAN)
                    .fill()

            ).focusable().onKeyEvent(event -> {
                if (event.isChar('1')) {
                    mode = LayoutMode.COLUMNS;
                    return EventResult.HANDLED;
                }
                if (event.isChar('2')) {
                    mode = LayoutMode.GRID;
                    return EventResult.HANDLED;
                }
                if (event.isChar('3')) {
                    mode = LayoutMode.FLOW;
                    return EventResult.HANDLED;
                }
                if (event.isChar('4')) {
                    mode = LayoutMode.DOCK;
                    return EventResult.HANDLED;
                }
                if (event.isChar('+') || event.isChar('=')) {
                    spacing = Math.min(spacing + 1, 5);
                    return EventResult.HANDLED;
                }
                if (event.isChar('-')) {
                    spacing = Math.max(spacing - 1, 0);
                    return EventResult.HANDLED;
                }
                return EventResult.UNHANDLED;
            }));
        }
    }

    private Element renderLayout() {
        return switch (mode) {
            case COLUMNS -> renderColumns();
            case GRID -> renderGrid();
            case FLOW -> renderFlow();
            case DOCK -> renderDock();
        };
    }

    private Element[] makeCards() {
        Element[] cards = new Element[ITEMS.length];
        for (int i = 0; i < ITEMS.length; i++) {
            cards[i] = card(ITEMS[i], COLORS[i]);
        }
        return cards;
    }

    private Element renderColumns() {
        // Column-first ordering: items fill top-to-bottom, then next column
        return columns(makeCards())
            .columnCount(3)
            .columnFirst()
            .spacing(spacing);
    }

    private Element renderGrid() {
        // Row-first with per-column width constraints (narrow, wide, medium)
        return grid(makeCards())
            .gridSize(3)
            .gridColumns(Constraint.length(14), Constraint.fill(), Constraint.length(20))
            .gutter(spacing);
    }

    private Element renderFlow() {
        // Natural wrapping: items flow left-to-right at their preferred width
        return flow(makeCards())
            .spacing(spacing)
            .rowSpacing(spacing);
    }

    private Element renderDock() {
        return dock()
            .top(panel("Header",
                text("  Menu Bar — File  Edit  View  Help").fg(Color.WHITE)
            ).rounded().borderColor(Color.BLUE))
            .bottom(panel("Status",
                row(
                    text("  Ready").green(),
                    spacer(),
                    text("Spacing: " + spacing + "  ").dim()
                )
            ).rounded().borderColor(Color.DARK_GRAY))
            .left(panel("Navigator",
                column(
                    text("  ▸ src/").bold().yellow(),
                    text("    main/").dim(),
                    text("    test/").dim(),
                    text("  ▸ build/").yellow(),
                    text("  README.md").cyan()
                )
            ).rounded().borderColor(Color.GREEN))
            .right(panel("Outline",
                column(
                    text("  ● class App").yellow(),
                    text("    ├ main()").dim(),
                    text("    ├ render()").dim(),
                    text("    └ run()").dim()
                )
            ).rounded().borderColor(Color.MAGENTA))
            .center(panel("Editor",
                column(
                    text("  1  public class App {").white(),
                    text("  2      // Center region").green(),
                    text("  3      // of the Dock layout").green(),
                    text("  4  }").white()
                )
            ).rounded().borderColor(Color.CYAN))
            .topHeight(Constraint.length(4))
            .bottomHeight(Constraint.length(4))
            .leftWidth(Constraint.length(22))
            .rightWidth(Constraint.length(22));
    }

    private static Element card(String label, Color color) {
        return panel(text("  " + label + "  ").bold().fg(color))
            .rounded()
            .borderColor(color);
    }
}

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
import dev.tamboui.layout.dock.Dock;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Demo TUI application showcasing the Dock widget.
 * <p>
 * Demonstrates a 5-region dock layout (top, bottom, left, right, center)
 * — the most common TUI application structure.
 */
public class DockDemo {

    private boolean running = true;

    private DockDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new DockDemo().run();
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

            backend.onResize(() -> {
                terminal.draw(this::ui);
            });

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
        renderDock(frame, layout.get(1));
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
                    Span.raw("Dock Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderDock(Frame frame, Rect area) {
        Dock dock = Dock.builder()
            .top(topPanel())
            .bottom(bottomPanel())
            .left(leftPanel())
            .right(rightPanel())
            .center(centerPanel())
            .topHeight(Constraint.length(3))
            .bottomHeight(Constraint.length(3))
            .leftWidth(Constraint.length(20))
            .rightWidth(Constraint.length(20))
            .build();

        frame.renderWidget(dock, area);
    }

    private static Paragraph topPanel() {
        return Paragraph.builder()
            .text(Text.from(
                Line.from(
                    Span.raw("  File  Edit  View  Help").fg(Color.WHITE)
                )
            ))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.BLUE))
                .title(Title.from(
                    Line.from(Span.raw(" Menu Bar ").bold().blue())
                ))
                .build())
            .build();
    }

    private static Paragraph bottomPanel() {
        return Paragraph.builder()
            .text(Text.from(
                Line.from(
                    Span.raw("  Ready").green(),
                    Span.raw("  |  ").dim(),
                    Span.raw("Ln 1, Col 1").dim(),
                    Span.raw("  |  ").dim(),
                    Span.raw("UTF-8").dim()
                )
            ))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(Title.from(
                    Line.from(Span.raw(" Status ").dim())
                ))
                .build())
            .build();
    }

    private static Paragraph leftPanel() {
        return Paragraph.builder()
            .text(Text.from(
                Line.from(Span.raw("  ▸ src/").bold().yellow()),
                Line.from(Span.raw("    main/").dim()),
                Line.from(Span.raw("    test/").dim()),
                Line.from(Span.raw("  ▸ build/").yellow()),
                Line.from(Span.raw("  ▸ docs/").yellow()),
                Line.from(Span.raw("  README.md").cyan()),
                Line.from(Span.raw("  build.gradle").cyan())
            ))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(
                    Line.from(Span.raw(" Explorer ").bold().green())
                ))
                .build())
            .build();
    }

    private static Paragraph rightPanel() {
        return Paragraph.builder()
            .text(Text.from(
                Line.from(Span.raw("  Outline").bold()),
                Line.from(Span.raw("  ─────────").dim()),
                Line.from(Span.raw("  ● class Dock").yellow()),
                Line.from(Span.raw("    ├ builder()").dim()),
                Line.from(Span.raw("    ├ render()").dim()),
                Line.from(Span.raw("    └ Builder").dim())
            ))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title(Title.from(
                    Line.from(Span.raw(" Outline ").bold().magenta())
                ))
                .build())
            .build();
    }

    private static Paragraph centerPanel() {
        return Paragraph.builder()
            .text(Text.from(
                Line.from(
                    Span.raw("  1 ").dim(),
                    Span.raw("public class ").magenta(),
                    Span.raw("DockDemo ").yellow(),
                    Span.raw("{").white()
                ),
                Line.from(
                    Span.raw("  2 ").dim(),
                    Span.raw("    // Main content area").green()
                ),
                Line.from(
                    Span.raw("  3 ").dim(),
                    Span.raw("    // This is the center region").green()
                ),
                Line.from(
                    Span.raw("  4 ").dim(),
                    Span.raw("    // of the Dock layout").green()
                ),
                Line.from(
                    Span.raw("  5 ").dim(),
                    Span.raw("}").white()
                ),
                Line.empty(),
                Line.from(Span.raw("  The Dock widget provides a 5-region layout:").cyan()),
                Line.from(Span.raw("  top, bottom, left, right, and center.").cyan()),
                Line.from(Span.raw("  Each region has configurable sizing.").cyan())
            ))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(
                    Line.from(Span.raw(" Editor ").bold().cyan())
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

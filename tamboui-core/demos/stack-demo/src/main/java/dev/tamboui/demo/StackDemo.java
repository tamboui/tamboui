///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.ContentAlignment;
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
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.layout.stack.Stack;

/**
 * Demo TUI application showcasing the Stack widget.
 * <p>
 * Demonstrates overlapping layers using the painter's algorithm —
 * useful for dialogs, popups, and floating overlays.
 */
public class StackDemo {

    private boolean running = true;
    private boolean showDialog = true;

    private StackDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new StackDemo().run();
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
                } else if (c == 'd' || c == 'D') {
                    showDialog = !showDialog;
                }
            }
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.fill(),
                Constraint.length(3)
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderStack(frame, layout.get(1));
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
                    Span.raw("Stack Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderStack(Frame frame, Rect area) {
        // Background layer — fills entire area
        Paragraph background = Paragraph.builder()
            .text(Text.from(
                Line.from(Span.raw("  This is the background content layer.").dim()),
                Line.from(Span.raw("  It fills the entire stack area.").dim()),
                Line.empty(),
                Line.from(Span.raw("  The Stack widget renders children in order,").dim()),
                Line.from(Span.raw("  with each layer painted on top of the previous.").dim()),
                Line.empty(),
                Line.from(Span.raw("  Press 'd' to toggle the dialog overlay.").dim())
            ))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(Title.from(
                    Line.from(Span.raw(" Background Layer ").dim())
                ))
                .build())
            .build();

        if (showDialog) {
            // Dialog layer — rendered on top
            Paragraph dialog = Paragraph.builder()
                .text(Text.from(
                    Line.empty(),
                    Line.from(Span.raw("  This dialog floats on top").bold().white()),
                    Line.from(Span.raw("  of the background layer.").white()),
                    Line.empty(),
                    Line.from(Span.raw("  Stack uses painter's algo:").cyan()),
                    Line.from(Span.raw("  last child wins per cell.").cyan()),
                    Line.empty(),
                    Line.from(
                        Span.raw("  Press ").dim(),
                        Span.raw("d").bold().yellow(),
                        Span.raw(" to dismiss").dim()
                    )
                ))
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.DOUBLE)
                    .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                    .title(Title.from(
                        Line.from(Span.raw(" Dialog ").bold().yellow())
                    ).centered())
                    .build())
                .build();

            Stack.builder()
                .children(background, dialog)
                .build()
                .render(area, frame.buffer());
        } else {
            background.render(area, frame.buffer());
        }
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" d").bold().yellow(),
            Span.raw(" Toggle dialog  ").dim(),
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

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
import dev.tamboui.layout.flow.Flow;
import dev.tamboui.layout.flow.FlowItem;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Demo TUI application showcasing the Flow widget.
 * <p>
 * Demonstrates a wrap layout where items flow left-to-right
 * and wrap to the next line — useful for tag clouds, chip lists,
 * and button groups.
 */
public class FlowDemo {

    private static final String[] TAGS = {
        "Java", "Kotlin", "Rust", "Python", "Go", "TypeScript",
        "C++", "Ruby", "Swift", "Dart", "Scala", "Haskell",
        "Elixir", "Clojure", "Zig", "Nim", "OCaml", "F#",
        "Lua", "Julia", "R", "MATLAB", "Perl", "PHP"
    };

    private static final Color[] TAG_COLORS = {
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.MAGENTA, Color.WHITE, Color.LIGHT_RED,
        Color.LIGHT_GREEN, Color.LIGHT_BLUE, Color.LIGHT_YELLOW, Color.LIGHT_CYAN
    };

    private boolean running = true;
    private int spacing = 1;

    private FlowDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new FlowDemo().run();
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
                switch (c) {
                    case 'q', 'Q', 3 -> running = false;
                    case '+', '=' -> spacing = Math.min(spacing + 1, 5);
                    case '-' -> spacing = Math.max(spacing - 1, 0);
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
        renderFlow(frame, layout.get(1));
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
                    Span.raw("Flow Demo ").yellow(),
                    Span.raw("[spacing=" + spacing + "]").dim()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderFlow(Frame frame, Rect area) {
        Block outerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.GREEN))
            .title(Title.from(
                Line.from(
                    Span.raw(" Programming Languages ").bold().green(),
                    Span.raw("(resize to see wrapping) ").dim()
                )
            ))
            .build();

        frame.renderWidget(outerBlock, area);
        Rect inner = outerBlock.inner(area);

        var builder = Flow.builder()
            .horizontalSpacing(spacing)
            .verticalSpacing(1);

        for (int i = 0; i < TAGS.length; i++) {
            String tag = TAGS[i];
            Color color = TAG_COLORS[i % TAG_COLORS.length];
            int tagWidth = tag.length() + 4; // " [tag] " with padding

            Paragraph tagWidget = Paragraph.builder()
                .text(Text.from(
                    Line.from(Span.raw(" " + tag + " ").bold().fg(color))
                ))
                .block(Block.builder()
                    .borders(Borders.ALL)
                    .borderType(BorderType.ROUNDED)
                    .borderStyle(Style.EMPTY.fg(color))
                    .build())
                .build();

            builder.item(tagWidget, tagWidth, 3);
        }

        builder.build().render(inner, frame.buffer());
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" +/-").bold().yellow(),
            Span.raw(" Spacing  ").dim(),
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

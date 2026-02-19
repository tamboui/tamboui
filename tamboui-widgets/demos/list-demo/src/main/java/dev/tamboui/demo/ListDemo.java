///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.io.IOException;
import java.util.List;

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
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

/**
 * Demo TUI application showcasing the List widget.
 * <p>
 * Demonstrates various List features:
 * - Basic list with selection
 * - Highlight styles and symbols
 * - Multiline list items
 * - Styled list items
 * - Block integration
 * - Navigation
 */
public class ListDemo {

    private enum FocusedList {
        BASIC,
        MULTILINE
    }

    private boolean running = true;
    private final ListState basicListState = new ListState();
    private final ListState multilineListState = new ListState();
    private FocusedList focusedList = FocusedList.BASIC;

    private ListDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new ListDemo().run();
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
                    terminal.draw(this::ui);
            });

            // Select first item in both lists
            basicListState.selectFirst();
            multilineListState.selectFirst();

            // Event loop
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == -2 || c == -1) {
                    continue;
                }

                boolean needsRedraw = handleInput(c, backend);
                if (needsRedraw) {
                    // Redraw handled in loop
                }
            }
        }
    }

    private boolean handleInput(int c, Backend backend) throws IOException {
        // Handle escape sequences
        if (c == 27) {
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50);
                int code = backend.read(50);
                return handleEscapeSequence(code);
            }
            return false;
        }

        return switch (c) {
            case 'q', 'Q', 3 -> {
                running = false;
                yield true;
            }
            case '\t' -> {
                // Switch focus between lists
                focusedList = focusedList == FocusedList.BASIC 
                    ? FocusedList.MULTILINE 
                    : FocusedList.BASIC;
                yield true;
            }
            case 'j', 'J' -> {
                if (focusedList == FocusedList.BASIC) {
                    basicListState.selectNext(getItems().size());
                } else {
                    multilineListState.selectNext(getMultilineItems().size());
                }
                yield true;
            }
            case 'k', 'K' -> {
                if (focusedList == FocusedList.BASIC) {
                    basicListState.selectPrevious();
                } else {
                    multilineListState.selectPrevious();
                }
                yield true;
            }
            case 'g' -> {
                if (focusedList == FocusedList.BASIC) {
                    basicListState.selectFirst();
                } else {
                    multilineListState.selectFirst();
                }
                yield true;
            }
            case 'G' -> {
                if (focusedList == FocusedList.BASIC) {
                    basicListState.selectLast(getItems().size());
                } else {
                    multilineListState.selectLast(getMultilineItems().size());
                }
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleEscapeSequence(int code) {
        return switch (code) {
            case 'A' -> { // Up arrow
                if (focusedList == FocusedList.BASIC) {
                    basicListState.selectPrevious();
                } else {
                    multilineListState.selectPrevious();
                }
                yield true;
            }
            case 'B' -> { // Down arrow
                if (focusedList == FocusedList.BASIC) {
                    basicListState.selectNext(getItems().size());
                } else {
                    multilineListState.selectNext(getMultilineItems().size());
                }
                yield true;
            }
            default -> false;
        };
    }

    private List<String> getItems() {
        return List.of("Item 1", "Item 2", "Item 3", "Item 4");
    }

    private List<ListItem> getMultilineItems() {
        return List.of(
            ListItem.from(Text.from(
                Line.from("[Remy]: I'm building one now."),
                Line.from("It even supports multiline text!")
            )),
            ListItem.from(Line.from("[Gusteau]: With enough passion, yes.")),
            ListItem.from(Line.from("[Remy]: But can anyone build a TUI in Java?")),
            ListItem.from(Line.from("[Gusteau]: With enough passion, yes!")),
            ListItem.from(Line.from(
                Span.raw("[System]: ").bold().fg(Color.CYAN),
                Span.raw("List widget supports ").fg(Color.WHITE),
                Span.raw("styled text").bold().fg(Color.YELLOW),
                Span.raw(" in items!").fg(Color.WHITE)
            )),
            ListItem.from(Line.from(
                Span.raw("[Info]: ").bold().fg(Color.GREEN),
                Span.raw("You can use ").fg(Color.WHITE),
                Span.raw("multiple spans").italic().fg(Color.MAGENTA),
                Span.raw(" per line.").fg(Color.WHITE)
            ))
        );
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
                    Span.raw("List Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into 2 rows
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .spacing(1)
            .split(area);

        // Top: Basic list
        renderBasicList(frame, rows.get(0));

        // Bottom: Multiline list with styles
        renderMultilineList(frame, rows.get(1));
    }

    /**
     * Render a basic list with selection.
     */
    private void renderBasicList(Frame frame, Rect area) {
        ListWidget list = ListWidget.builder()
            .items("Item 1", "Item 2", "Item 3", "Item 4")
            .style(Style.EMPTY.fg(Color.WHITE))
            .highlightStyle(Style.EMPTY.reversed())
            .highlightSymbol("> ")
            .build();

        frame.renderStatefulWidget(list, area, basicListState);
    }

    /**
     * Render a list with multiline items and custom styling.
     */
    private void renderMultilineList(Frame frame, Rect area) {
        ListWidget list = ListWidget.builder()
            .items(
                ListItem.from(Text.from(
                    Line.from("[Remy]: I'm building one now."),
                    Line.from("It even supports multiline text!")
                )),
                ListItem.from(Line.from("[Gusteau]: With enough passion, yes.")),
                ListItem.from(Line.from("[Remy]: But can anyone build a TUI in Java?")),
                ListItem.from(Line.from("[Gusteau]: With enough passion, yes!")),
                ListItem.from(Line.from(
                    Span.raw("[System]: ").bold().fg(Color.CYAN),
                    Span.raw("List widget supports ").fg(Color.WHITE),
                    Span.raw("styled text").bold().fg(Color.YELLOW),
                    Span.raw(" in items!").fg(Color.WHITE)
                )),
                ListItem.from(Line.from(
                    Span.raw("[Info]: ").bold().fg(Color.GREEN),
                    Span.raw("You can use ").fg(Color.WHITE),
                    Span.raw("multiple spans").italic().fg(Color.MAGENTA),
                    Span.raw(" per line.").fg(Color.WHITE)
                ))
            )
            .style(Style.EMPTY.fg(Color.WHITE))
            .highlightStyle(Style.EMPTY.fg(Color.YELLOW).italic())
            .highlightSymbol(Line.from("> ").fg(Color.RED))
            .build();

        frame.renderStatefulWidget(list, area, multilineListState);
    }

    private void renderFooter(Frame frame, Rect area) {
        Integer selected;
        String selectedText;
        if (focusedList == FocusedList.BASIC) {
            selected = basicListState.selected();
            selectedText = selected != null && selected < getItems().size()
                ? getItems().get(selected)
                : "None";
        } else {
            selected = multilineListState.selected();
            List<ListItem> items = getMultilineItems();
            selectedText = selected != null && selected < items.size()
                ? items.get(selected).content().lines().get(0).rawContent()
                : "None";
        }

        Line helpLine = Line.from(
            Span.raw(" Focus: ").dim(),
            Span.raw(focusedList == FocusedList.BASIC ? "Basic" : "Multiline").bold().cyan(),
            Span.raw("  Selected: ").dim(),
            Span.raw(selectedText).bold().cyan(),
            Span.raw("   "),
            Span.raw("Tab").bold().yellow(),
            Span.raw(" Switch  ").dim(),
            Span.raw("j/k/↑/↓").bold().yellow(),
            Span.raw(" Navigate  ").dim(),
            Span.raw("g/G").bold().yellow(),
            Span.raw(" First/Last  ").dim(),
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

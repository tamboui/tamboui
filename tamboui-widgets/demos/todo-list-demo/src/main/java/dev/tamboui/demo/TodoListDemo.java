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
import dev.tamboui.text.MarkupParser;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.layout.Padding;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.style.Overflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo TUI application showcasing a Todo List with selectable items.
 * <p>
 * Demonstrates:
 * - Todo items with status (Todo/Completed)
 * - List widget with selection and navigation
 * - Toggle status with Enter/Right arrow
 * - Show item info below the list
 * - Different styling for completed vs todo items
 * - Alternating row background colors
 */
public class TodoListDemo {

    private enum Status {
        TODO,
        COMPLETED
    }

    private static class TodoItem {
        final String todo;
        final String info;
        Status status;

        TodoItem(Status status, String todo, String info) {
            this.status = status;
            this.todo = todo;
            this.info = info;
        }
    }

    private boolean running = true;
    private final ListState listState = new ListState();
    private final List<TodoItem> todoItems = new ArrayList<>();

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new TodoListDemo().run();
    }

    private TodoListDemo() {
        // Initialize with default todo items
        todoItems.add(new TodoItem(Status.TODO, "Rewrite everything with your programming language of choice!",
            "I can't hold my inner voice. He tells me to rewrite the complete universe with Java"));
        todoItems.add(new TodoItem(Status.COMPLETED, "Rewrite all of your tui apps with TamboUI",
            "Yes, you heard that right. Go and replace your tui with TamboUI."));
        todoItems.add(new TodoItem(Status.TODO, "Pet your cat",
            "Minnak loves to be pet by you! Don't forget to pet and give some treats!"));
        todoItems.add(new TodoItem(Status.TODO, "Walk with your dog",
            "Max is bored, go walk with him!"));
        todoItems.add(new TodoItem(Status.COMPLETED, "Pay the bills",
            "Pay the train subscription!!!"));
        todoItems.add(new TodoItem(Status.COMPLETED, "Refactor list example",
            "If you see this info that means I completed this task!"));
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

            // Select first item
            listState.selectFirst();

            // Event loop
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == -2 || c == -1) {
                    continue;
                }

                handleInput(c, backend);
            }
        }
    }

    private void handleInput(int c, Backend backend) throws IOException {
        // Handle escape sequences
        if (c == 27) {
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50);
                int code = backend.read(50);
                handleEscapeSequence(code);
            }
            return;
        }

        switch (c) {
            case 'q', 'Q', 3 -> running = false;
            case 'h', 'H' -> listState.select(null); // Unselect
            case 'j', 'J' -> listState.selectNext(todoItems.size());
            case 'k', 'K' -> listState.selectPrevious();
            case 'g' -> listState.selectFirst();
            case 'G' -> listState.selectLast(todoItems.size());
            case 'l', 'L', '\n', '\r' -> toggleStatus(); // Toggle status
            default -> {
                // No action
            }
        }
    }

    private void handleEscapeSequence(int code) {
        switch (code) {
            case 'A' -> listState.selectPrevious(); // Up arrow
            case 'B' -> listState.selectNext(todoItems.size()); // Down arrow
            case 'C' -> toggleStatus(); // Right arrow
            case 'D' -> listState.select(null); // Left arrow
            case 'H' -> listState.selectFirst(); // Home
            case 'F' -> listState.selectLast(todoItems.size()); // End
            default -> {
                // No action
            }
        }
    }

    private void toggleStatus() {
        Integer selected = listState.selected();
        if (selected != null && selected >= 0 && selected < todoItems.size()) {
            TodoItem item = todoItems.get(selected);
            item.status = item.status == Status.TODO ? Status.COMPLETED : Status.TODO;
        }
    }

    private List<ListItem> buildListItems() {
        List<ListItem> items = new ArrayList<>();
        for (int i = 0; i < todoItems.size(); i++) {
            TodoItem todoItem = todoItems.get(i);
            Color bgColor = (i % 2 == 0) 
                ? Color.indexed(235)  // Dark gray (similar to SLATE.c950)
                : Color.indexed(236); // Slightly lighter (similar to SLATE.c900)

            Text line;
            if (todoItem.status == Status.TODO) {
                line = MarkupParser.parse("[light-gray] :black_square_button: " + todoItem.todo);
            } else {
                line = MarkupParser.parse("[green] :white_check_mark: " + todoItem.todo);
            }

            ListItem item = ListItem.from(line)
                .style(Style.EMPTY.bg(bgColor));
            items.add(item);
        }
        return items;
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(2),  // Header
                Constraint.fill(),     // Main content
                Constraint.length(1)   // Footer
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Paragraph header = Paragraph.builder()
            .text(Text.from(Line.from("TamboUI Todo List Example").bold()))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build();

        frame.renderWidget(header, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        var layout = Layout.vertical()
            .constraints(
                Constraint.fill(),  // List area
                Constraint.fill()   // Info area
            )
            .split(area);

        renderList(frame, layout.get(0));
        renderSelectedItemInfo(frame, layout.get(1));
    }

    private void renderList(Frame frame, Rect area) {
        List<ListItem> items = buildListItems();

        Block block = Block.builder()
            .borders(Borders.TOP_ONLY)
            .borderType(BorderType.PLAIN)
            .borderStyle(Style.EMPTY.fg(Color.indexed(100)).bg(Color.indexed(235)))
            .title(Title.from(Line.from("TODO List").centered()))
            .style(Style.EMPTY.bg(Color.indexed(235)))
            .build();

        ListWidget list = ListWidget.builder()
            .items(items)
            .style(Style.EMPTY.fg(Color.indexed(250)))
            .highlightStyle(Style.EMPTY.bg(Color.indexed(238)).bold())
            .highlightSymbol(">")
            .block(block)
            .build();

        frame.renderStatefulWidget(list, area, listState);
    }

    private void renderSelectedItemInfo(Frame frame, Rect area) {
        String info;
        Integer selected = listState.selected();
        
        if (selected != null && selected >= 0 && selected < todoItems.size()) {
            TodoItem item = todoItems.get(selected);
            if (item.status == Status.COMPLETED) {
                info = "✓ DONE: " + item.info;
            } else {
                info = "☐ TODO: " + item.info;
            }
        } else {
            info = "Nothing selected...";
        }

        Block block = Block.builder()
            .borders(Borders.TOP_ONLY)
            .borderType(BorderType.PLAIN)
            .borderStyle(Style.EMPTY.fg(Color.indexed(100)).bg(Color.indexed(235)))
            .title(Title.from(Line.from("TODO Info").centered()))
            .style(Style.EMPTY.bg(Color.indexed(235)))
            .padding(Padding.horizontal(1))
            .build();

        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(info))
            .block(block)
            .style(Style.EMPTY.fg(Color.indexed(250)))
            .overflow(Overflow.WRAP_WORD)
            .build();

        frame.renderWidget(paragraph, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw("Use ").dim(),
            Span.raw("↓↑").bold().yellow(),
            Span.raw(" to move, ").dim(),
            Span.raw("←").bold().yellow(),
            Span.raw(" to unselect, ").dim(),
            Span.raw("→/Enter").bold().yellow(),
            Span.raw(" to change status, ").dim(),
            Span.raw("g/G").bold().yellow(),
            Span.raw(" to go top/bottom.").dim()
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(helpLine))
            .alignment(dev.tamboui.layout.Alignment.CENTER)
            .build();

        frame.renderWidget(footer, area);
    }
}


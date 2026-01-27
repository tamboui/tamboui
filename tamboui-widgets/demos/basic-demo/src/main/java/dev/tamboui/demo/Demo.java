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
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.list.ListItem;
import dev.tamboui.widgets.list.ListState;
import dev.tamboui.widgets.list.ListWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo TUI application showcasing TamboUI features.
 */
public class Demo {

    private static final String[] DEFAULT_ITEMS = {
        "Item 1 - First item in the list",
        "Item 2 - Second item",
        "Item 3 - Third item",
        "Item 4 - Fourth item",
        "Item 5 - Fifth item"
    };

    private enum FocusedWidget {
        LIST,
        INPUT
    }

    private boolean running = true;
    private final ListState listState = new ListState();
    private final TextInputState inputState = new TextInputState();
    private final List<String> items = new ArrayList<>(List.of(DEFAULT_ITEMS));
    private FocusedWidget focused = FocusedWidget.LIST;
    private int counter = 0;

    private Demo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new Demo().run();
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

            // Initial draw
            terminal.draw(this::ui);

            // Event loop
            while (running) {
                int c = backend.read(100);
                if (c == -2) {
                    // Timeout - increment counter and redraw
                    counter++;
                    terminal.draw(this::ui);
                    continue;
                }
                if (c == -1) {
                    continue;
                }

                boolean needsRedraw = handleInput(c, backend);
                if (needsRedraw) {
                    terminal.draw(this::ui);
                }
            }
        }
    }

    private boolean handleInput(int c, Backend backend) throws IOException {
        // Handle escape sequences (arrow keys, etc.)
        if (c == 27) { // ESC
            int next = backend.peek(50);
            if (next == '[') {
                backend.read(50); // consume '['
                int code = backend.read(50);
                return handleEscapeSequence(code, backend);
            }
            // Plain ESC - could be used to cancel/unfocus
            return false;
        }

        // Handle Tab to switch focus
        if (c == '\t') {
            focused = focused == FocusedWidget.LIST ? FocusedWidget.INPUT : FocusedWidget.LIST;
            return true;
        }

        // Handle based on focused widget
        if (focused == FocusedWidget.INPUT) {
            return handleInputModeKey(c);
        } else {
            return handleListModeKey(c);
        }
    }

    private boolean handleEscapeSequence(int code, Backend backend) throws IOException {
        return switch (code) {
            case 'A' -> { // Up arrow
                if (focused == FocusedWidget.LIST) {
                    listState.selectPrevious();
                }
                yield true;
            }
            case 'B' -> { // Down arrow
                if (focused == FocusedWidget.LIST) {
                    listState.selectNext(items.size());
                }
                yield true;
            }
            case 'C' -> { // Right arrow
                if (focused == FocusedWidget.INPUT) {
                    inputState.moveCursorRight();
                }
                yield true;
            }
            case 'D' -> { // Left arrow
                if (focused == FocusedWidget.INPUT) {
                    inputState.moveCursorLeft();
                }
                yield true;
            }
            case 'H' -> { // Home
                if (focused == FocusedWidget.INPUT) {
                    inputState.moveCursorToStart();
                }
                yield true;
            }
            case 'F' -> { // End
                if (focused == FocusedWidget.INPUT) {
                    inputState.moveCursorToEnd();
                }
                yield true;
            }
            case '3' -> { // Delete key (ESC[3~)
                int tilde = backend.read(50);
                if (tilde == '~' && focused == FocusedWidget.INPUT) {
                    inputState.deleteForward();
                }
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleListModeKey(int c) {
        return switch (c) {
            case 'q', 'Q' -> {
                running = false;
                yield true;
            }
            case 'j', 'J' -> {
                listState.selectNext(items.size());
                yield true;
            }
            case 'k', 'K' -> {
                listState.selectPrevious();
                yield true;
            }
            case 'd', 'D' -> {
                // Delete selected item
                Integer selected = listState.selected();
                if (selected != null && selected < items.size()) {
                    items.remove((int) selected);
                    if (selected >= items.size() && selected > 0) {
                        listState.select(selected - 1);
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleInputModeKey(int c) {
        return switch (c) {
            case 'q' -> {
                // In input mode, 'q' is just a character, not quit
                // Only quit with Ctrl+C or when list is focused
                inputState.insert((char) c);
                yield true;
            }
            case 127, 8 -> { // Backspace
                inputState.deleteBackward();
                yield true;
            }
            case '\r', '\n' -> { // Enter - add item to list
                String text = inputState.text().trim();
                if (!text.isEmpty()) {
                    items.add(text);
                    inputState.clear();
                    // Select the newly added item
                    listState.select(items.size() - 1);
                }
                yield true;
            }
            case 1 -> { // Ctrl+A - move to start
                inputState.moveCursorToStart();
                yield true;
            }
            case 5 -> { // Ctrl+E - move to end
                inputState.moveCursorToEnd();
                yield true;
            }
            case 21 -> { // Ctrl+U - clear input
                inputState.clear();
                yield true;
            }
            case 3 -> { // Ctrl+C - quit
                running = false;
                yield true;
            }
            default -> {
                // Regular character input
                if (c >= 32 && c < 127) {
                    inputState.insert((char) c);
                    yield true;
                }
                yield false;
            }
        };
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        // Split into 4 areas: header, main content, input, footer
        List<Rect> mainLayout = Layout.vertical()
            .constraints(
                Constraint.length(3),    // Header
                Constraint.fill(),       // Main content
                Constraint.length(3),    // Input field
                Constraint.length(3)     // Footer
            )
            .split(area);

        renderHeader(frame, mainLayout.get(0));
        renderMain(frame, mainLayout.get(1));
        renderInput(frame, mainLayout.get(2));
        renderFooter(frame, mainLayout.get(3));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMain(Frame frame, Rect area) {
        // Split main area into left and right panels
        List<Rect> mainPanels = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .spacing(1)
            .split(area);

        renderList(frame, mainPanels.get(0));
        renderInfo(frame, mainPanels.get(1));
    }

    private void renderList(Frame frame, Rect area) {
        List<ListItem> listItems = items.stream()
            .map(ListItem::from)
            .toList();

        boolean isFocused = focused == FocusedWidget.LIST;
        Color borderColor = isFocused ? Color.GREEN : Color.DARK_GRAY;

        ListWidget list = ListWidget.builder()
            .items(listItems)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(borderColor))
                .title(Title.from(
                    Line.from(
                        Span.raw("Items "),
                        Span.raw(isFocused ? "(focused)" : "").dim()
                    )
                ))
                .titleBottom(Title.from("j/k/↑/↓ navigate, d delete").right())
                .build())
            .highlightStyle(Style.EMPTY.bg(Color.BLUE).fg(Color.WHITE).bold())
            .highlightSymbol("▶ ")
            .build();

        frame.renderStatefulWidget(list, area, listState);
    }

    private void renderInfo(Frame frame, Rect area) {
        Integer selected = listState.selected();
        String selectedText = selected != null && selected < items.size()
            ? items.get(selected)
            : "None";

        Text infoText = Text.from(
            Line.from(Span.raw("Selected: ").bold(), Span.raw(selectedText).cyan()),
            Line.empty(),
            Line.from(Span.raw("Items count: ").bold(), Span.raw(String.valueOf(items.size())).yellow()),
            Line.empty(),
            Line.from(Span.raw("Counter: ").bold(), Span.raw(String.valueOf(counter)).yellow()),
            Line.empty(),
            Line.from(Span.raw("Input text: ").bold()),
            Line.from(Span.raw("  \"" + inputState.text() + "\"").green()),
            Line.empty(),
            Line.from(Span.raw("Focus: ").bold(), Span.raw(focused.name()).magenta())
        );

        Paragraph info = Paragraph.builder()
            .text(infoText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title("Information")
                .build())
            .build();

        frame.renderWidget(info, area);
    }

    private void renderInput(Frame frame, Rect area) {
        boolean isFocused = focused == FocusedWidget.INPUT;
        Color borderColor = isFocused ? Color.YELLOW : Color.DARK_GRAY;

        TextInput input = TextInput.builder()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(borderColor))
                .title(Title.from(
                    Line.from(
                        Span.raw("Add Item "),
                        Span.raw(isFocused ? "(focused)" : "").dim()
                    )
                ))
                .titleBottom(Title.from("Enter to add, Tab to switch focus").right())
                .build())
            .style(Style.EMPTY.fg(Color.WHITE))
            .cursorStyle(Style.EMPTY.reversed())
            .placeholder("Type here and press Enter...")
            .placeholderStyle(Style.EMPTY.dim().italic())
            .build();

        if (isFocused) {
            input.renderWithCursor(area, frame.buffer(), inputState, frame);
        } else {
            frame.renderStatefulWidget(input, area, inputState);
        }
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" Tab").bold().yellow(),
            Span.raw(" Switch focus  ").dim(),
            Span.raw("q").bold().yellow(),
            Span.raw(" Quit  ").dim(),
            Span.raw("↑↓").bold().yellow(),
            Span.raw(" Navigate  ").dim(),
            Span.raw("Enter").bold().yellow(),
            Span.raw(" Add item  ").dim(),
            Span.raw("Ctrl+C").bold().yellow(),
            Span.raw(" Force quit").dim()
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

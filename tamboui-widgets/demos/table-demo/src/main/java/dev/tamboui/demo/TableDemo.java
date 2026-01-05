///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST

/*
 * Copyright (c) 2025 TamboUI Contributors
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
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.table.Cell;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.Table;
import dev.tamboui.widgets.table.TableState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo TUI application showcasing the Table widget.
 */
public class TableDemo {

    private static final List<String[]> DATA = List.of(
        new String[]{"Alice Johnson", "Software Engineer", "Engineering", "$120,000", "San Francisco"},
        new String[]{"Bob Smith", "Product Manager", "Product", "$135,000", "New York"},
        new String[]{"Charlie Brown", "Data Scientist", "Analytics", "$125,000", "Seattle"},
        new String[]{"Diana Ross", "UX Designer", "Design", "$95,000", "Austin"},
        new String[]{"Edward Chen", "DevOps Engineer", "Engineering", "$115,000", "Portland"},
        new String[]{"Fiona Garcia", "Frontend Developer", "Engineering", "$105,000", "Denver"},
        new String[]{"George Wilson", "Backend Developer", "Engineering", "$110,000", "Chicago"},
        new String[]{"Hannah Lee", "QA Engineer", "Engineering", "$90,000", "Boston"},
        new String[]{"Ivan Petrov", "Security Analyst", "Security", "$130,000", "Washington DC"},
        new String[]{"Julia Martinez", "Technical Writer", "Documentation", "$85,000", "Miami"},
        new String[]{"Kevin O'Brien", "Sales Engineer", "Sales", "$140,000", "Los Angeles"},
        new String[]{"Laura Kim", "HR Manager", "Human Resources", "$100,000", "Atlanta"}
    );

    private boolean running = true;
    private final TableState tableState = new TableState();

    public static void main(String[] args) throws Exception {
        new TableDemo().run();
    }

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

            // Select first row
            tableState.selectFirst();

            // Initial draw
            terminal.draw(this::ui);

            // Event loop
            while (running) {
                int c = backend.read(100);
                if (c == -2 || c == -1) {
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
            case 'j', 'J' -> {
                tableState.selectNext(DATA.size());
                yield true;
            }
            case 'k', 'K' -> {
                tableState.selectPrevious();
                yield true;
            }
            case 'g' -> {
                tableState.selectFirst();
                yield true;
            }
            case 'G' -> {
                tableState.selectLast(DATA.size());
                yield true;
            }
            default -> false;
        };
    }

    private boolean handleEscapeSequence(int code) {
        return switch (code) {
            case 'A' -> { // Up
                tableState.selectPrevious();
                yield true;
            }
            case 'B' -> { // Down
                tableState.selectNext(DATA.size());
                yield true;
            }
            default -> false;
        };
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        List<Rect> layout = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Header
                Constraint.fill(),     // Table
                Constraint.length(5),  // Details
                Constraint.length(3)   // Footer
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderTable(frame, layout.get(1));
        renderDetails(frame, layout.get(2));
        renderFooter(frame, layout.get(3));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Table Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderTable(Frame frame, Rect area) {
        // Create header row
        Row header = Row.from(
            Cell.from("Name").style(Style.EMPTY.bold()),
            Cell.from("Title").style(Style.EMPTY.bold()),
            Cell.from("Department").style(Style.EMPTY.bold()),
            Cell.from("Salary").style(Style.EMPTY.bold()),
            Cell.from("Location").style(Style.EMPTY.bold())
        ).style(Style.EMPTY.fg(Color.YELLOW));

        // Create data rows with alternating colors
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < DATA.size(); i++) {
            String[] data = DATA.get(i);
            Style rowStyle = i % 2 == 0 ? Style.EMPTY : Style.EMPTY.bg(Color.indexed(236));
            rows.add(Row.from(data).style(rowStyle));
        }

        Table table = Table.builder()
            .header(header)
            .rows(rows)
            .widths(
                Constraint.percentage(20),  // Name
                Constraint.percentage(20),  // Title
                Constraint.percentage(20),  // Department
                Constraint.length(12),      // Salary
                Constraint.fill()           // Location
            )
            .highlightStyle(Style.EMPTY.bg(Color.BLUE).fg(Color.WHITE).bold())
            .highlightSymbol("▶ ")
            .columnSpacing(1)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(
                    Line.from(
                        Span.raw("Employees "),
                        Span.raw("(" + DATA.size() + " total)").dim()
                    )
                ))
                .build())
            .build();

        frame.renderStatefulWidget(table, area, tableState);
    }

    private void renderDetails(Frame frame, Rect area) {
        Integer selected = tableState.selected();
        Text detailsText;

        if (selected != null && selected < DATA.size()) {
            String[] data = DATA.get(selected);
            detailsText = Text.from(
                Line.from(
                    Span.raw("Name: ").bold(),
                    Span.raw(data[0]).cyan()
                ),
                Line.from(
                    Span.raw("Title: ").bold(),
                    Span.raw(data[1]).green()
                ),
                Line.from(
                    Span.raw("Department: ").bold(),
                    Span.raw(data[2]).yellow(),
                    Span.raw("  |  Salary: ").bold(),
                    Span.raw(data[3]).magenta(),
                    Span.raw("  |  Location: ").bold(),
                    Span.raw(data[4]).blue()
                )
            );
        } else {
            detailsText = Text.from(Line.from(Span.raw("No employee selected").dim()));
        }

        Paragraph details = Paragraph.builder()
            .text(detailsText)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title("Details")
                .build())
            .build();

        frame.renderWidget(details, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" j/↓").bold().yellow(),
            Span.raw(" Down  ").dim(),
            Span.raw("k/↑").bold().yellow(),
            Span.raw(" Up  ").dim(),
            Span.raw("g").bold().yellow(),
            Span.raw(" First  ").dim(),
            Span.raw("G").bold().yellow(),
            Span.raw(" Last  ").dim(),
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

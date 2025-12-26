///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
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
import dev.tamboui.widgets.calendar.CalendarEventStore;
import dev.tamboui.widgets.calendar.Monthly;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Demo TUI application showcasing the Calendar widget.
 * <p>
 * Demonstrates monthly calendar views with:
 * - Different styling options
 * - Event highlighting
 * - First day of week configuration
 * - Multi-month displays
 */
public class CalendarDemo {

    private boolean running = true;
    private LocalDate currentDate = LocalDate.now();
    private DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY;
    private boolean showSurrounding = true;

    public static void main(String[] args) throws Exception {
        new CalendarDemo().run();
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

            // Event loop
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                handleInput(c);
            }
        }
    }

    private void handleInput(int c) {
        switch (c) {
            case 'q', 'Q', 3 -> running = false;
            case 'h', 'H' -> currentDate = currentDate.minusMonths(1);
            case 'l', 'L' -> currentDate = currentDate.plusMonths(1);
            case 'j', 'J' -> currentDate = currentDate.plusYears(1);
            case 'k', 'K' -> currentDate = currentDate.minusYears(1);
            case 't', 'T' -> currentDate = LocalDate.now();
            case 's', 'S' -> showSurrounding = !showSurrounding;
            case 'f', 'F' -> {
                // Cycle first day of week
                firstDayOfWeek = switch (firstDayOfWeek) {
                    case MONDAY -> DayOfWeek.SUNDAY;
                    case SUNDAY -> DayOfWeek.SATURDAY;
                    default -> DayOfWeek.MONDAY;
                };
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
        renderCalendars(frame, layout.get(1));
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
                    Span.raw("Calendar Demo ").yellow(),
                    Span.raw("[" + currentDate.getMonth().name() + " " + currentDate.getYear() + "]").dim()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderCalendars(Frame frame, Rect area) {
        // Create layout: 2x2 grid of calendars
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        var topCols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(rows.get(0));

        var bottomCols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(rows.get(1));

        // Current month with events
        renderCurrentMonth(frame, topCols.get(0));

        // Previous month
        renderPreviousMonth(frame, topCols.get(1));

        // Next month
        renderNextMonth(frame, bottomCols.get(0));

        // Style showcase
        renderStyleShowcase(frame, bottomCols.get(1));
    }

    private void renderCurrentMonth(Frame frame, Rect area) {
        // Create event store with various dates styled
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(currentDate);

        var events = CalendarEventStore.today(Style.EMPTY.fg(Color.RED).bold())
            .add(currentDate, Style.EMPTY.fg(Color.YELLOW).bold());

        // Add some example holidays/events
        LocalDate christmas = LocalDate.of(currentDate.getYear(), 12, 25);
        LocalDate newYear = LocalDate.of(currentDate.getYear(), 1, 1);
        LocalDate valentines = LocalDate.of(currentDate.getYear(), 2, 14);
        LocalDate july4 = LocalDate.of(currentDate.getYear(), 7, 4);

        events = events
            .add(christmas, Style.EMPTY.fg(Color.GREEN).bold())
            .add(newYear, Style.EMPTY.fg(Color.MAGENTA).bold())
            .add(valentines, Style.EMPTY.fg(Color.LIGHT_RED).bold())
            .add(july4, Style.EMPTY.fg(Color.BLUE).bold());

        // Mark weekends
        LocalDate weekendDate = ym.atDay(1);
        while (weekendDate.getMonth() == currentDate.getMonth()) {
            if (weekendDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                weekendDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                if (!events.contains(weekendDate)) {
                    events = events.add(weekendDate, Style.EMPTY.dim());
                }
            }
            weekendDate = weekendDate.plusDays(1);
        }

        var builder = Monthly.builder(currentDate, events)
            .monthHeaderStyle(Style.EMPTY.bold().fg(Color.CYAN))
            .weekdaysHeaderStyle(Style.EMPTY.fg(Color.YELLOW))
            .defaultStyle(Style.EMPTY.fg(Color.WHITE))
            .firstDayOfWeek(firstDayOfWeek)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(Line.from(
                    Span.raw(" Current Month ").cyan()
                )))
                .build());

        if (showSurrounding) {
            builder.surroundingStyle(Style.EMPTY.dim());
        }

        var calendar = builder.build();
        frame.renderWidget(calendar, area);
    }

    private void renderPreviousMonth(Frame frame, Rect area) {
        LocalDate prevMonth = currentDate.minusMonths(1);

        var calendar = Monthly.of(prevMonth, d -> Style.EMPTY)
            .showMonthHeader(Style.EMPTY.bold())
            .showWeekdaysHeader(Style.EMPTY.dim())
            .firstDayOfWeek(firstDayOfWeek)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(Title.from(Line.from(
                    Span.raw(" Previous Month ").dim()
                )))
                .build());

        if (showSurrounding) {
            calendar = calendar.showSurrounding(Style.EMPTY.dim());
        }

        frame.renderWidget(calendar, area);
    }

    private void renderNextMonth(Frame frame, Rect area) {
        LocalDate nextMonth = currentDate.plusMonths(1);

        var calendar = Monthly.of(nextMonth, d -> Style.EMPTY)
            .showMonthHeader(Style.EMPTY.bold())
            .showWeekdaysHeader(Style.EMPTY.dim())
            .firstDayOfWeek(firstDayOfWeek)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(Title.from(Line.from(
                    Span.raw(" Next Month ").dim()
                )))
                .build());

        if (showSurrounding) {
            calendar = calendar.showSurrounding(Style.EMPTY.dim());
        }

        frame.renderWidget(calendar, area);
    }

    private void renderStyleShowcase(Frame frame, Rect area) {
        // Rainbow style: each day of week has different color
        var calendar = Monthly.of(currentDate, date -> {
                return switch (date.getDayOfWeek()) {
                    case MONDAY -> Style.EMPTY.fg(Color.RED);
                    case TUESDAY -> Style.EMPTY.fg(Color.YELLOW);
                    case WEDNESDAY -> Style.EMPTY.fg(Color.GREEN);
                    case THURSDAY -> Style.EMPTY.fg(Color.CYAN);
                    case FRIDAY -> Style.EMPTY.fg(Color.BLUE);
                    case SATURDAY -> Style.EMPTY.fg(Color.MAGENTA);
                    case SUNDAY -> Style.EMPTY.fg(Color.WHITE);
                };
            })
            .showMonthHeader(Style.EMPTY.bold().fg(Color.MAGENTA))
            .showWeekdaysHeader(Style.EMPTY.fg(Color.GREEN))
            .firstDayOfWeek(firstDayOfWeek)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title(Title.from(Line.from(
                    Span.raw(" Rainbow Days ").magenta()
                )))
                .build());

        if (showSurrounding) {
            calendar = calendar.showSurrounding(Style.EMPTY.dim());
        }

        frame.renderWidget(calendar, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        String fdow = switch (firstDayOfWeek) {
            case MONDAY -> "Mon";
            case SUNDAY -> "Sun";
            case SATURDAY -> "Sat";
            default -> firstDayOfWeek.name();
        };

        Line helpLine = Line.from(
            Span.raw(" h/l").bold().yellow(),
            Span.raw(" Month ").dim(),
            Span.raw("j/k").bold().yellow(),
            Span.raw(" Year ").dim(),
            Span.raw("t").bold().yellow(),
            Span.raw(" Today ").dim(),
            Span.raw("f").bold().yellow(),
            Span.raw(" First:" + fdow + " ").dim(),
            Span.raw("s").bold().yellow(),
            Span.raw(" Surround:" + (showSurrounding ? "ON" : "OFF") + " ").dim(),
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

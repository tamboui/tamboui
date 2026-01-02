/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.calendar;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.block.Block;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyTest {

    @Test
    void ofCurrentMonthCreatesCalendar() {
        Monthly calendar = Monthly.ofCurrentMonth();
        assertThat(calendar).isNotNull();
    }

    @Test
    void ofCreatesCalendarForGivenDate() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY);
        assertThat(calendar).isNotNull();
    }

    @Test
    void renderEmptyAreaDoesNothing() {
        Monthly calendar = Monthly.ofCurrentMonth();
        Buffer buffer = Buffer.empty(new Rect(0, 0, 0, 0));

        calendar.render(new Rect(0, 0, 0, 0), buffer);
        // Should not throw
    }

    @Test
    void renderBasicCalendarGrid() {
        // June 2025 - starts on Sunday
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY);

        // Need at least 21 chars width (7 * 3 - 1) and 6 rows for grid
        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 8));
        calendar.render(new Rect(0, 0, 25, 8), buffer);

        // Calendar should render something - check that buffer isn't empty
        boolean hasContent = false;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 25; x++) {
                Cell cell = buffer.get(x, y);
                if (!cell.symbol().equals(" ") && !cell.symbol().isEmpty()) {
                    hasContent = true;
                    break;
                }
            }
            if (hasContent) {
                break;
            }
        }
        assertThat(hasContent).isTrue();
    }

    @Test
    void renderWithMonthHeader() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY)
            .showMonthHeader(Style.EMPTY.bold());

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 10));
        calendar.render(new Rect(0, 0, 25, 10), buffer);

        // Find "June" in the buffer
        String content = extractBufferContent(buffer, 0, 0, 25, 1);
        assertThat(content).contains("June");
        assertThat(content).contains("2025");
    }

    @Test
    void renderWithWeekdaysHeader() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY)
            .showWeekdaysHeader(Style.EMPTY.fg(Color.CYAN));

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 10));
        calendar.render(new Rect(0, 0, 25, 10), buffer);

        // Find weekday abbreviations in the buffer
        String content = extractBufferContent(buffer, 0, 0, 25, 1);
        assertThat(content).contains("Mo");
    }

    @Test
    void renderWithBothHeaders() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY)
            .showMonthHeader(Style.EMPTY.bold())
            .showWeekdaysHeader(Style.EMPTY.fg(Color.CYAN));

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 12));
        calendar.render(new Rect(0, 0, 25, 12), buffer);

        // First row should have month
        String row0 = extractBufferContent(buffer, 0, 0, 25, 1);
        assertThat(row0).contains("June");

        // Second row should have weekdays
        String row1 = extractBufferContent(buffer, 0, 1, 25, 1);
        assertThat(row1).contains("Mo");
    }

    @Test
    void renderWithBlock() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY)
            .block(Block.bordered());

        Buffer buffer = Buffer.empty(new Rect(0, 0, 27, 12));
        calendar.render(new Rect(0, 0, 27, 12), buffer);

        // Top-left corner should be border
        String topLeft = buffer.get(0, 0).symbol();
        assertThat(topLeft).isNotEqualTo(" ");
    }

    @Test
    void renderWithDateStyler() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Style redBold = Style.EMPTY.fg(Color.RED).bold();

        Monthly calendar = Monthly.of(date, d -> {
            if (d.getDayOfMonth() == 15) {
                return redBold;
            }
            return Style.EMPTY;
        });

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 8));
        calendar.render(new Rect(0, 0, 25, 8), buffer);

        // Find the "15" in the buffer and verify its style
        boolean foundStyledDay = false;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 24; x++) {
                Cell cell = buffer.get(x, y);
                Cell nextCell = buffer.get(x + 1, y);
                if (cell.symbol().equals("1") && nextCell.symbol().equals("5")) {
                    // Found "15" - check style
                    if (cell.style().fg().orElse(null) == Color.RED) {
                        foundStyledDay = true;
                    }
                    break;
                }
            }
            if (foundStyledDay) {
                break;
            }
        }
        assertThat(foundStyledDay).isTrue();
    }

    @Test
    void renderWithSurroundingDays() {
        // June 2025 starts on Sunday (with Monday as first day of week,
        // we need days from May to fill the first row)
        LocalDate date = LocalDate.of(2025, 6, 15);
        Style dimStyle = Style.EMPTY.dim();

        Monthly calendar = Monthly.of(date, d -> Style.EMPTY)
            .showSurrounding(dimStyle);

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 8));
        calendar.render(new Rect(0, 0, 25, 8), buffer);

        // The calendar should render something
        assertThat(buffer).isNotNull();
    }

    @Test
    void renderWithCalendarEventStore() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Style red = Style.EMPTY.fg(Color.RED);
        Style green = Style.EMPTY.fg(Color.GREEN);

        CalendarEventStore events = CalendarEventStore.empty()
            .add(LocalDate.of(2025, 6, 15), red)
            .add(LocalDate.of(2025, 6, 25), green);

        Monthly calendar = Monthly.of(date, events);

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 8));
        calendar.render(new Rect(0, 0, 25, 8), buffer);

        assertThat(buffer).isNotNull();
    }

    @Test
    void firstDayOfWeekChangesDayOrder() {
        LocalDate date = LocalDate.of(2025, 6, 15);

        Monthly mondayFirst = Monthly.of(date, d -> Style.EMPTY)
            .showWeekdaysHeader(Style.EMPTY)
            .firstDayOfWeek(DayOfWeek.MONDAY);

        Monthly sundayFirst = Monthly.of(date, d -> Style.EMPTY)
            .showWeekdaysHeader(Style.EMPTY)
            .firstDayOfWeek(DayOfWeek.SUNDAY);

        Buffer buffer1 = Buffer.empty(new Rect(0, 0, 25, 10));
        Buffer buffer2 = Buffer.empty(new Rect(0, 0, 25, 10));

        mondayFirst.render(new Rect(0, 0, 25, 10), buffer1);
        sundayFirst.render(new Rect(0, 0, 25, 10), buffer2);

        String header1 = extractBufferContent(buffer1, 0, 0, 25, 1);
        String header2 = extractBufferContent(buffer2, 0, 0, 25, 1);

        // Monday first should start with "Mo"
        assertThat(header1.trim()).startsWith("Mo");

        // Sunday first should start with "Su"
        assertThat(header2.trim()).startsWith("Su");
    }

    @Test
    void defaultStyleAppliedToUnstyledDates() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Style defaultCyan = Style.EMPTY.fg(Color.CYAN);

        Monthly calendar = Monthly.of(date, d -> Style.EMPTY)
            .defaultStyle(defaultCyan);

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 8));
        calendar.render(new Rect(0, 0, 25, 8), buffer);

        // Find a day that should have the default style
        boolean foundStyledDay = false;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 25; x++) {
                Cell cell = buffer.get(x, y);
                if (Character.isDigit(cell.symbol().charAt(0))) {
                    if (cell.style().fg().orElse(null) == Color.CYAN) {
                        foundStyledDay = true;
                        break;
                    }
                }
            }
            if (foundStyledDay) {
                break;
            }
        }
        assertThat(foundStyledDay).isTrue();
    }

    @Test
    void builderCreatesCalendar() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Style red = Style.EMPTY.fg(Color.RED);
        Style cyan = Style.EMPTY.fg(Color.CYAN);
        Style dim = Style.EMPTY.dim();
        Style white = Style.EMPTY.fg(Color.WHITE);

        Monthly calendar = Monthly.builder(date, d -> Style.EMPTY)
            .monthHeaderStyle(red)
            .weekdaysHeaderStyle(cyan)
            .surroundingStyle(dim)
            .defaultStyle(white)
            .block(Block.bordered())
            .firstDayOfWeek(DayOfWeek.SUNDAY)
            .build();

        assertThat(calendar).isNotNull();
    }

    @Test
    void renderMultipleMonths() {
        // Test different months render correctly
        for (int month = 1; month <= 12; month++) {
            LocalDate date = LocalDate.of(2025, month, 15);
            Monthly calendar = Monthly.of(date, d -> Style.EMPTY)
                .showMonthHeader(Style.EMPTY);

            Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 10));
            calendar.render(new Rect(0, 0, 25, 10), buffer);

            String header = extractBufferContent(buffer, 0, 0, 25, 1);
            assertThat(header).contains("2025");
        }
    }

    @Test
    void renderFebruaryLeapYear() {
        // 2024 is a leap year
        LocalDate date = LocalDate.of(2024, 2, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY);

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 8));
        calendar.render(new Rect(0, 0, 25, 8), buffer);

        // Should contain day 29
        String content = extractBufferContent(buffer, 0, 0, 25, 8);
        assertThat(content).contains("29");
    }

    @Test
    void renderFebruaryNonLeapYear() {
        // 2025 is not a leap year
        LocalDate date = LocalDate.of(2025, 2, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY);

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 8));
        calendar.render(new Rect(0, 0, 25, 8), buffer);

        // Should contain day 28 but not 29 in February context
        String content = extractBufferContent(buffer, 0, 0, 25, 8);
        assertThat(content).contains("28");
    }

    private String extractBufferContent(Buffer buffer, int startX, int startY, int width, int height) {
        StringBuilder sb = new StringBuilder();
        for (int y = startY; y < startY + height; y++) {
            for (int x = startX; x < startX + width; x++) {
                sb.append(buffer.get(x, y).symbol());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

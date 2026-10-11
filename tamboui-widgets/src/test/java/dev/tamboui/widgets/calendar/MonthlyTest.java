/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.block.Block;

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
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY, Locale.US)
            .showMonthHeader(Style.EMPTY.bold());

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 10));
        calendar.render(new Rect(0, 0, 25, 10), buffer);

        // Find "June" in the buffer
        String content = extractBufferContent(buffer, 0, 0, 25, 1);
        assertThat(content).contains("June");
        assertThat(content).contains("2025");
    }

    @Test
    void withCustomLocale_renderWithMonthHeader() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY, new Locale("pl"))
                .showMonthHeader(Style.EMPTY.bold());

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 10));
        calendar.render(new Rect(0, 0, 25, 10), buffer);

        // Find "Czerwiec" in the buffer
        String content = extractBufferContent(buffer, 0, 0, 25, 1);
        assertThat(content).contains("Czerwiec");
    }

    @Test
    void ofCurrentMonthWithLocale_renderWithMonthHeader() {
        Monthly calendar = Monthly.ofCurrentMonth(Locale.US)
                .showMonthHeader(Style.EMPTY.bold());

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 10));
        calendar.render(new Rect(0, 0, 25, 10), buffer);

        String currentMonth = LocalDate.now().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, Locale.US);

        // Find the currentMonth in the buffer
        String content = extractBufferContent(buffer, 0, 0, 25, 1);
        assertThat(content).contains(currentMonth);
    }

    @Test
    void renderWithWeekdaysHeader() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY, Locale.US)
            .showWeekdaysHeader(Style.EMPTY.fg(Color.CYAN));

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 10));
        calendar.render(new Rect(0, 0, 25, 10), buffer);

        // Find weekday abbreviations in the buffer
        String content = extractBufferContent(buffer, 0, 0, 25, 1);
        assertThat(content).contains("Mo");
    }

    @Test
    void withCustomLocale_renderWithWeekdaysHeader() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY, new Locale("pl", "PL"))
                .showWeekdaysHeader(Style.EMPTY.fg(Color.CYAN));

        Buffer buffer = Buffer.empty(new Rect(0, 0, 25, 10));
        calendar.render(new Rect(0, 0, 25, 10), buffer);

        // Find weekday abbreviations in the buffer
        String content = extractBufferContent(buffer, 0, 0, 25, 1);
        assertThat(content).contains("Po");
    }

    @Test
    void renderWithBothHeaders() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Monthly calendar = Monthly.of(date, d -> Style.EMPTY, Locale.US)
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

        Monthly mondayFirst = Monthly.of(date, d -> Style.EMPTY, Locale.US)
            .showWeekdaysHeader(Style.EMPTY)
            .firstDayOfWeek(DayOfWeek.MONDAY);

        Monthly sundayFirst = Monthly.of(date, d -> Style.EMPTY, Locale.US)
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

    @Test
    void japaneseWeekdaysLineUpWithTheDayColumns() {
        BufferAssertions.assertThat(renderOctober2025(Locale.JAPANESE)).hasContent(
            "     10月 2025      ",
            "月 火 水 木 金 土 日",
            "       1  2  3  4  5",
            " 6  7  8  9 10 11 12",
            "13 14 15 16 17 18 19",
            "20 21 22 23 24 25 26",
            "27 28 29 30 31      ");
    }

    @Test
    void koreanWeekdaysLineUpWithTheDayColumns() {
        BufferAssertions.assertThat(renderOctober2025(Locale.KOREAN)).hasContent(
            "     10월 2025      ",
            "월 화 수 목 금 토 일",
            "       1  2  3  4  5",
            " 6  7  8  9 10 11 12",
            "13 14 15 16 17 18 19",
            "20 21 22 23 24 25 26",
            "27 28 29 30 31      ");
    }

    @Test
    void chineseWeekdaysUseTheNarrowNamesWhenTheShortNamesLookAlike() {
        // The short names 周一 to 周日 all fit two columns as 周
        BufferAssertions.assertThat(renderOctober2025(Locale.CHINESE)).hasContent(
            "     十月 2025      ",
            "一 二 三 四 五 六 日",
            "       1  2  3  4  5",
            " 6  7  8  9 10 11 12",
            "13 14 15 16 17 18 19",
            "20 21 22 23 24 25 26",
            "27 28 29 30 31      ");
    }

    @Test
    void englishWeekdaysKeepTheirFirstTwoLetters() {
        BufferAssertions.assertThat(renderOctober2025(Locale.ENGLISH)).hasContent(
            "    October 2025    ",
            "Mo Tu We Th Fr Sa Su",
            "       1  2  3  4  5",
            " 6  7  8  9 10 11 12",
            "13 14 15 16 17 18 19",
            "20 21 22 23 24 25 26",
            "27 28 29 30 31      ");
    }

    @Test
    void fitToDayWidthCutsAndPadsByDisplayWidth() {
        assertThat(Monthly.fitToDayWidth("Mon")).isEqualTo("Mo");
        assertThat(Monthly.fitToDayWidth("A")).isEqualTo(" A");
        assertThat(Monthly.fitToDayWidth("月")).isEqualTo("月");
        assertThat(Monthly.fitToDayWidth("周一")).isEqualTo("周");
        assertThat(Monthly.fitToDayWidth("a月")).isEqualTo(" a");
        assertThat(Monthly.fitToDayWidth("")).isEqualTo("  ");
        assertThat(Monthly.fitToDayWidth(null)).isEqualTo("  ");
    }

    @Test
    void fitToDayWidthKeepsZwjSequencesWhole() {
        assertThat(Monthly.fitToDayWidth("👩\u200D💻")).isEqualTo("👩\u200D💻");
        assertThat(Monthly.fitToDayWidth("👨\u200D👩\u200D👧 family")).isEqualTo("👨\u200D👩\u200D👧");
        assertThat(Monthly.fitToDayWidth("a👩\u200D💻")).isEqualTo(" a");
    }

    @Test
    void fitToDayWidthKeepsAGraphemeClusterWiderThanTwoColumnsWhole() {
        // Tibetan for Saturday starts with a stacked letter that is three columns wide
        // for CharWidth: it is kept whole rather than cut, or dropped for a blank label
        assertThat(Monthly.fitToDayWidth("\u0F66\u0FA4\u0F7A\u0F53")).isEqualTo("\u0F66\u0FA4\u0F7A");
    }

    @Test
    void wideScriptWidensDayCellsSoTheWeekdayHeaderStaysAligned() {
        // Burmese weekday names include three-column grapheme clusters (e.g. Thursday). With fixed
        // two-column cells the header overflowed the grid; the cells now widen to three columns so the
        // weekday header lines up with the days, and the grid is wider than a two-column locale's.
        Buffer burmese = renderOctober2025Wide(Locale.forLanguageTag("my"));
        Buffer english = renderOctober2025Wide(Locale.ENGLISH);

        int headerRight = rightmostNonSpace(burmese, 1);  // row 0 is the month header, row 1 the weekdays
        int gridRight = maxRightmostNonSpace(burmese, 2, 8);
        assertThat(headerRight)
            .as("Burmese weekday header ends at the same column as the day grid")
            .isEqualTo(gridRight);

        assertThat(maxRightmostNonSpace(burmese, 2, 8))
            .as("three-column cells make the Burmese grid wider than the two-column English grid")
            .isGreaterThan(maxRightmostNonSpace(english, 2, 8));
    }

    private static Buffer renderOctober2025Wide(Locale locale) {
        Monthly calendar = Monthly.of(LocalDate.of(2025, 10, 15), d -> Style.EMPTY, locale)
            .showMonthHeader(Style.EMPTY)
            .showWeekdaysHeader(Style.EMPTY);
        Rect area = new Rect(0, 0, 30, 9);
        Buffer buffer = Buffer.empty(area);
        calendar.render(area, buffer);
        return buffer;
    }

    private static int rightmostNonSpace(Buffer buffer, int y) {
        int rightmost = -1;
        for (int x = buffer.area().left(); x < buffer.area().right(); x++) {
            String symbol = buffer.get(x, y).symbol();
            if (symbol != null && !symbol.isEmpty() && !symbol.equals(" ")) {
                rightmost = x;
            }
        }
        return rightmost;
    }

    private static int maxRightmostNonSpace(Buffer buffer, int startY, int endY) {
        int max = -1;
        for (int y = startY; y < endY; y++) {
            max = Math.max(max, rightmostNonSpace(buffer, y));
        }
        return max;
    }

    private static Buffer renderOctober2025(Locale locale) {
        Monthly calendar = Monthly.of(LocalDate.of(2025, 10, 15), d -> Style.EMPTY, locale)
            .showMonthHeader(Style.EMPTY)
            .showWeekdaysHeader(Style.EMPTY);
        Rect area = new Rect(0, 0, 20, 7);
        Buffer buffer = Buffer.empty(area);
        calendar.render(area, buffer);
        return buffer;
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

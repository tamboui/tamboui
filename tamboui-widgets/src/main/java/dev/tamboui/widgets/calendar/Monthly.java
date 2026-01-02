/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.calendar;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.Widget;
import dev.tamboui.widgets.block.Block;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * A calendar widget displaying a single month.
 * <p>
 * Renders a calendar grid for the month containing the display date,
 * with customizable styling for headers, weekdays, and individual dates.
 *
 * <pre>{@code
 * CalendarEventStore events = CalendarEventStore.today(Style.EMPTY.fg(Color.RED).bold());
 *
 * Monthly calendar = Monthly.of(LocalDate.now(), events)
 *     .showMonthHeader(Style.EMPTY.bold())
 *     .showWeekdaysHeader(Style.EMPTY.fg(Color.CYAN))
 *     .showSurrounding(Style.EMPTY.dim())
 *     .block(Block.bordered());
 * }</pre>
 *
 * @see DateStyler
 * @see CalendarEventStore
 */
public final class Monthly implements Widget {

    private static final String[] WEEKDAY_ABBREV = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
    private static final int CELL_WIDTH = 3;  // Width per day cell (2 digits + space)
    private static final int CALENDAR_WIDTH = CELL_WIDTH * 7 - 1;  // 7 days, minus trailing space

    private final LocalDate displayDate;
    private final DateStyler dateStyler;
    private final Style monthHeaderStyle;
    private final Style weekdaysHeaderStyle;
    private final Style surroundingStyle;
    private final Style defaultStyle;
    private final Block block;
    private final DayOfWeek firstDayOfWeek;

    private Monthly(Builder builder) {
        this.displayDate = builder.displayDate;
        this.dateStyler = builder.dateStyler;
        this.monthHeaderStyle = builder.monthHeaderStyle;
        this.weekdaysHeaderStyle = builder.weekdaysHeaderStyle;
        this.surroundingStyle = builder.surroundingStyle;
        this.defaultStyle = builder.defaultStyle;
        this.block = builder.block;
        this.firstDayOfWeek = builder.firstDayOfWeek;
    }

    /**
     * Creates a calendar for the month containing the given date.
     *
     * @param displayDate the date determining which month to display
     * @param dateStyler  the styler for individual dates
     * @return a new Monthly calendar
     */
    public static Monthly of(LocalDate displayDate, DateStyler dateStyler) {
        return new Builder(displayDate, dateStyler).build();
    }

    /**
     * Creates a calendar for the current month with no special styling.
     */
    public static Monthly ofCurrentMonth() {
        return of(LocalDate.now(), date -> Style.EMPTY);
    }

    /**
     * Creates a builder for a calendar displaying the given month.
     */
    public static Builder builder(LocalDate displayDate, DateStyler dateStyler) {
        return new Builder(displayDate, dateStyler);
    }

    /**
     * Returns a new calendar with the month header shown.
     */
    public Monthly showMonthHeader(Style style) {
        return toBuilder().monthHeaderStyle(style).build();
    }

    /**
     * Returns a new calendar with the weekdays header shown.
     */
    public Monthly showWeekdaysHeader(Style style) {
        return toBuilder().weekdaysHeaderStyle(style).build();
    }

    /**
     * Returns a new calendar with surrounding days shown.
     * <p>
     * Surrounding days are days from the previous/next month that
     * appear in the calendar grid.
     */
    public Monthly showSurrounding(Style style) {
        return toBuilder().surroundingStyle(style).build();
    }

    /**
     * Returns a new calendar with the default date style.
     */
    public Monthly defaultStyle(Style style) {
        return toBuilder().defaultStyle(style).build();
    }

    /**
     * Returns a new calendar wrapped in a block.
     */
    public Monthly block(Block block) {
        return toBuilder().block(block).build();
    }

    /**
     * Returns a new calendar with the given first day of week.
     */
    public Monthly firstDayOfWeek(DayOfWeek dayOfWeek) {
        return toBuilder().firstDayOfWeek(dayOfWeek).build();
    }

    private Builder toBuilder() {
        return new Builder(displayDate, dateStyler)
            .monthHeaderStyle(monthHeaderStyle)
            .weekdaysHeaderStyle(weekdaysHeaderStyle)
            .surroundingStyle(surroundingStyle)
            .defaultStyle(defaultStyle)
            .block(block)
            .firstDayOfWeek(firstDayOfWeek);
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        // Render block if present
        Rect calendarArea = area;
        if (block != null) {
            block.render(area, buffer);
            calendarArea = block.inner(area);
        }

        if (calendarArea.isEmpty()) {
            return;
        }

        int y = calendarArea.y();

        // Render month header
        if (monthHeaderStyle != null) {
            y = renderMonthHeader(buffer, calendarArea, y);
        }

        // Render weekdays header
        if (weekdaysHeaderStyle != null) {
            y = renderWeekdaysHeader(buffer, calendarArea, y);
        }

        // Render calendar grid
        renderCalendarGrid(buffer, calendarArea, y);
    }

    private int renderMonthHeader(Buffer buffer, Rect area, int y) {
        if (y >= area.bottom()) {
            return y;
        }

        YearMonth yearMonth = YearMonth.from(displayDate);
        String monthName = yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        String header = monthName + " " + yearMonth.getYear();

        // Center the header
        int x = area.x() + Math.max(0, (area.width() - header.length()) / 2);
        buffer.setString(x, y, header, monthHeaderStyle);

        return y + 1;
    }

    private int renderWeekdaysHeader(Buffer buffer, Rect area, int y) {
        if (y >= area.bottom()) {
            return y;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            int dayIndex = (firstDayOfWeek.getValue() - 1 + i) % 7;
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(WEEKDAY_ABBREV[dayIndex]);
        }

        int x = area.x() + Math.max(0, (area.width() - sb.length()) / 2);
        buffer.setString(x, y, sb.toString(), weekdaysHeaderStyle);

        return y + 1;
    }

    private void renderCalendarGrid(Buffer buffer, Rect area, int startY) {
        YearMonth yearMonth = YearMonth.from(displayDate);
        LocalDate firstOfMonth = yearMonth.atDay(1);
        LocalDate lastOfMonth = yearMonth.atEndOfMonth();

        // Find the first day to display (may be from previous month)
        int dayOffset = (firstOfMonth.getDayOfWeek().getValue() - firstDayOfWeek.getValue() + 7) % 7;
        LocalDate gridStart = firstOfMonth.minusDays(dayOffset);

        // Calculate number of weeks needed
        int totalDays = dayOffset + yearMonth.lengthOfMonth();
        int weeks = (totalDays + 6) / 7;

        int y = startY;
        LocalDate currentDate = gridStart;

        for (int week = 0; week < weeks && y < area.bottom(); week++) {
            StringBuilder line = new StringBuilder();
            Style[] styles = new Style[7];

            for (int day = 0; day < 7; day++) {
                if (day > 0) {
                    line.append(" ");
                }

                String dayStr = String.format("%2d", currentDate.getDayOfMonth());
                line.append(dayStr);

                // Determine style for this day
                Style style;
                if (currentDate.isBefore(firstOfMonth) || currentDate.isAfter(lastOfMonth)) {
                    // Surrounding day
                    if (surroundingStyle != null) {
                        style = surroundingStyle;
                    } else {
                        // Don't show surrounding days if not configured
                        line.setLength(line.length() - 2);
                        line.append("  ");
                        styles[day] = null;
                        currentDate = currentDate.plusDays(1);
                        continue;
                    }
                } else {
                    // Day in current month
                    Style eventStyle = dateStyler.getStyle(currentDate);
                    if (eventStyle != null && !eventStyle.equals(Style.EMPTY)) {
                        style = eventStyle;
                    } else {
                        style = defaultStyle != null ? defaultStyle : Style.EMPTY;
                    }
                }

                styles[day] = style;
                currentDate = currentDate.plusDays(1);
            }

            // Render the line with individual styles
            int x = area.x() + Math.max(0, (area.width() - line.length()) / 2);
            int charPos = 0;

            for (int day = 0; day < 7; day++) {
                if (day > 0) {
                    // Space between days
                    buffer.setString(x + charPos, y, " ", Style.EMPTY);
                    charPos++;
                }

                if (styles[day] != null) {
                    String dayStr = line.substring(charPos, charPos + 2);
                    buffer.setString(x + charPos, y, dayStr, styles[day]);
                }
                charPos += 2;
            }

            y++;
        }
    }

    /**
     * Builder for {@link Monthly}.
     */
    public static final class Builder {
        private final LocalDate displayDate;
        private final DateStyler dateStyler;
        private Style monthHeaderStyle;
        private Style weekdaysHeaderStyle;
        private Style surroundingStyle;
        private Style defaultStyle;
        private Block block;
        private DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY;

        private Builder(LocalDate displayDate, DateStyler dateStyler) {
            this.displayDate = displayDate != null ? displayDate : LocalDate.now();
            this.dateStyler = dateStyler != null ? dateStyler : date -> Style.EMPTY;
        }

        /**
         * Shows the month header with the given style.
         */
        public Builder monthHeaderStyle(Style style) {
            this.monthHeaderStyle = style;
            return this;
        }

        /**
         * Shows the weekdays header with the given style.
         */
        public Builder weekdaysHeaderStyle(Style style) {
            this.weekdaysHeaderStyle = style;
            return this;
        }

        /**
         * Shows surrounding days with the given style.
         */
        public Builder surroundingStyle(Style style) {
            this.surroundingStyle = style;
            return this;
        }

        /**
         * Sets the default style for dates.
         */
        public Builder defaultStyle(Style style) {
            this.defaultStyle = style;
            return this;
        }

        /**
         * Wraps the calendar in a block.
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the first day of the week.
         */
        public Builder firstDayOfWeek(DayOfWeek dayOfWeek) {
            this.firstDayOfWeek = dayOfWeek != null ? dayOfWeek : DayOfWeek.MONDAY;
            return this;
        }

        /**
         * Builds the calendar.
         */
        public Monthly build() {
            return new Monthly(this);
        }
    }
}

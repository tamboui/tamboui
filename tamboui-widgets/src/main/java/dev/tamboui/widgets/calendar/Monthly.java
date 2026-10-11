/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.GraphemeClusters;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

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

    private static final int MIN_DAY_WIDTH = 2;  // A day number is at most two digits, so a cell is at least this wide

    // Weekday labels depend only on the locale, so they are computed once per locale and shared across all
    // calendars and renders (a calendar is often rebuilt every frame, so a per-instance cache would not help)
    private static final Map<Locale, String[]> WEEKDAY_LABEL_CACHE = new ConcurrentHashMap<>();

    private final LocalDate displayDate;
    private final DateStyler dateStyler;
    private final Style monthHeaderStyle;
    private final Style weekdaysHeaderStyle;
    private final Style surroundingStyle;
    private final Style defaultStyle;
    private final Block block;
    private final DayOfWeek firstDayOfWeek;
    private final Locale locale;

    // Weekday labels and the day cell width derived from them depend only on the final locale, so they are
    // computed once and reused across renders
    private Monthly(Builder builder) {
        this.displayDate = builder.displayDate;
        this.dateStyler = builder.dateStyler;
        this.monthHeaderStyle = builder.monthHeaderStyle;
        this.weekdaysHeaderStyle = builder.weekdaysHeaderStyle;
        this.surroundingStyle = builder.surroundingStyle;
        this.defaultStyle = builder.defaultStyle;
        this.block = builder.block;
        this.firstDayOfWeek = builder.firstDayOfWeek;
        this.locale = builder.locale;
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
     * Creates a calendar for the month containing the given date using custom locale.
     *
     * @param displayDate the date determining which month to display
     * @param dateStyler  the styler for individual dates
     * @param locale the locale to be used
     * @return a new Monthly calendar
     */
    public static Monthly of(LocalDate displayDate, DateStyler dateStyler, Locale locale) {
        return new Builder(displayDate, dateStyler)
                .locale(locale)
                .build();
    }

    /**
     * Creates a calendar for the current month with no special styling.
     *
     * @return a new Monthly calendar for the current month
     */
    public static Monthly ofCurrentMonth() {
        return of(LocalDate.now(), date -> Style.EMPTY);
    }

    /**
     * Creates a calendar for the current month with custom locale and no special styling.
     *
     * @param locale the locale to be used
     * @return a new Monthly calendar for the current month
     */
    public static Monthly ofCurrentMonth(Locale locale) {
        return of(LocalDate.now(), date -> Style.EMPTY, locale);
    }

    /**
     * Creates a builder for a calendar displaying the given month.
     *
     * @param displayDate the date determining which month to display
     * @param dateStyler  the styler for individual dates
     * @return a new Builder
     */
    public static Builder builder(LocalDate displayDate, DateStyler dateStyler) {
        return new Builder(displayDate, dateStyler);
    }

    /**
     * Returns a new calendar with the month header shown.
     *
     * @param style the style for the month header
     * @return a new Monthly with the month header enabled
     */
    public Monthly showMonthHeader(Style style) {
        return toBuilder().monthHeaderStyle(style).build();
    }

    /**
     * Returns a new calendar with the weekdays header shown.
     *
     * @param style the style for the weekdays header
     * @return a new Monthly with the weekdays header enabled
     */
    public Monthly showWeekdaysHeader(Style style) {
        return toBuilder().weekdaysHeaderStyle(style).build();
    }

    /**
     * Returns a new calendar with surrounding days shown.
     * <p>
     * Surrounding days are days from the previous/next month that
     * appear in the calendar grid.
     *
     * @param style the style for surrounding days
     * @return a new Monthly with surrounding days enabled
     */
    public Monthly showSurrounding(Style style) {
        return toBuilder().surroundingStyle(style).build();
    }

    /**
     * Returns a new calendar with the default date style.
     *
     * @param style the default style for dates
     * @return a new Monthly with the given default style
     */
    public Monthly defaultStyle(Style style) {
        return toBuilder().defaultStyle(style).build();
    }

    /**
     * Returns a new calendar wrapped in a block.
     *
     * @param block the block to wrap the calendar in
     * @return a new Monthly wrapped in the given block
     */
    public Monthly block(Block block) {
        return toBuilder().block(block).build();
    }

    /**
     * Returns a new calendar with the given first day of week.
     *
     * @param dayOfWeek the first day of the week
     * @return a new Monthly with the given first day of week
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
            .firstDayOfWeek(firstDayOfWeek)
            .locale(locale);
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

        // The day cell is as wide as the widest weekday label (three columns for some scripts, such as
        // Burmese or Tibetan), but at least two columns for the day numbers. Every column uses this width,
        // so the weekday header lines up with the days below it.
        String[] labels = weekdaysHeaderStyle != null ? weekdayLabels(locale) : null;
        int dayWidth = labels != null ? dayWidth(labels) : MIN_DAY_WIDTH;

        // Render weekdays header
        if (labels != null) {
            y = renderWeekdaysHeader(buffer, calendarArea, y, labels, dayWidth);
        }

        // Render calendar grid
        renderCalendarGrid(buffer, calendarArea, y, dayWidth);
    }

    private int renderMonthHeader(Buffer buffer, Rect area, int y) {
        if (y >= area.bottom()) {
            return y;
        }

        YearMonth yearMonth = YearMonth.from(displayDate);
        String monthName = capitalize(yearMonth.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, locale), locale);
        String header = monthName + " " + yearMonth.getYear();

        // Center the header
        int x = area.x() + Math.max(0, (area.width() - CharWidth.of(header)) / 2);
        buffer.setString(x, y, header, monthHeaderStyle);

        return y + 1;
    }

    private int renderWeekdaysHeader(Buffer buffer, Rect area, int y, String[] weekdayLabels, int dayWidth) {
        if (y >= area.bottom()) {
            return y;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            int dayIndex = (firstDayOfWeek.getValue() - 1 + i) % 7;
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(leftPad(weekdayLabels[dayIndex], dayWidth));
        }

        String header = sb.toString();
        int x = area.x() + Math.max(0, (area.width() - CharWidth.of(header)) / 2);
        buffer.setString(x, y, header, weekdaysHeaderStyle);

        return y + 1;
    }

    private void renderCalendarGrid(Buffer buffer, Rect area, int startY, int dayWidth) {
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

                String dayStr = String.format("%" + dayWidth + "d", currentDate.getDayOfMonth());
                line.append(dayStr);

                // Determine style for this day
                Style style;
                if (currentDate.isBefore(firstOfMonth) || currentDate.isAfter(lastOfMonth)) {
                    // Surrounding day
                    if (surroundingStyle != null) {
                        style = surroundingStyle;
                    } else {
                        // Don't show surrounding days if not configured
                        line.setLength(line.length() - dayWidth);
                        line.append(leftPad("", dayWidth));
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
                    String dayStr = line.substring(charPos, charPos + dayWidth);
                    buffer.setString(x + charPos, y, dayStr, styles[day]);
                }
                charPos += dayWidth;
            }

            y++;
        }
    }

    /**
     * Capitalizes the first letter of the given string.
     *
     * @param s the string to capitalize
     * @param locale the locale to be used
     * @return the string with the first letter converted to uppercase,
     *         or the original string if it is {@code null} or empty
     */
    private static String capitalize(String s, Locale locale) {
        if (s == null || s.isEmpty()) return s;
        int firstCp = s.codePointAt(0);
        int firstLen = Character.charCount(firstCp);
        String first = new String(Character.toChars(firstCp)).toUpperCase(locale);
        return first + s.substring(firstLen);
    }

    /**
     * Returns the weekday labels for the given locale, one per day column.
     * <p>
     * Each label is the day's short standalone name, capitalized according to the
     * locale rules and fitted to the width of a day number: "Mo" in English, "月" in
     * Japanese. Where that makes two days look the same, as "周一" and "周二" in
     * Chinese both fit as "周", the narrow names ("一", "二") are used instead.
     * <p>
     * The returned array contains seven elements, one for each day of the week,
     * ordered from Monday to Sunday as defined by {@link DayOfWeek#values()}.
     *
     * @param locale the locale to use for day-of-week display names
     * @return a seven-element array of weekday labels, each two columns wide
     * @see DayOfWeek#getDisplayName(TextStyle, Locale)
     */
    private static String[] weekdayLabels(Locale locale) {
        return WEEKDAY_LABEL_CACHE.computeIfAbsent(locale, Monthly::computeWeekdayLabels);
    }

    private static String[] computeWeekdayLabels(Locale locale) {
        String[] labels = weekdayLabels(locale, TextStyle.SHORT_STANDALONE);
        if (!allDistinct(labels)) {
            String[] narrowLabels = weekdayLabels(locale, TextStyle.NARROW_STANDALONE);
            if (allDistinct(narrowLabels)) {
                return narrowLabels;
            }
        }
        return labels;
    }

    private static String[] weekdayLabels(Locale locale, TextStyle textStyle) {
        return Arrays.stream(DayOfWeek.values())
            .map(dayOfWeek -> dayOfWeek.getDisplayName(textStyle, locale))
            .map(name -> capitalize(name, locale))
            .map(Monthly::fitToDayWidth)
            .toArray(String[]::new);
    }

    private static boolean allDistinct(String[] labels) {
        return new HashSet<>(Arrays.asList(labels)).size() == labels.length;
    }

    /**
     * Fits the given text to the two columns of a day number.
     * <p>
     * The text is cut to its first two columns, keeping whole grapheme clusters, and
     * left-padded with spaces to exactly two columns. A first grapheme cluster that is
     * wider than two columns on its own is kept whole rather than dropped. A {@code null}
     * input is treated as an empty string.
     *
     * <pre>{@code
     * fitToDayWidth("Hello")  → "He"
     * fitToDayWidth("A")      → " A"
     * fitToDayWidth("月")     → "月"
     * fitToDayWidth("周一")   → "周"
     * fitToDayWidth("a月")    → " a"
     * fitToDayWidth(null)     → "  "
     * }</pre>
     *
     * @param text the text to fit, may be {@code null}
     * @return the fitted text, two columns wide unless its first grapheme cluster is wider
     */
    static String fitToDayWidth(String text) {
        String fitted = CharWidth.substringByWidth(text, MIN_DAY_WIDTH);
        if (fitted.isEmpty() && text != null && !text.isEmpty()) {
            fitted = text.substring(0, GraphemeClusters.clusterEnd(text, 0));
        }
        return leftPad(fitted, MIN_DAY_WIDTH);
    }

    /**
     * Returns the width of a day cell: the width of the widest weekday label, but at least
     * {@link #MIN_DAY_WIDTH} for the day numbers. A script whose shortest weekday name is three
     * columns wide (such as Burmese or Tibetan) gives three-column cells, so the header stays aligned.
     *
     * @param labels the fitted weekday labels
     * @return the day cell width in columns
     */
    private static int dayWidth(String[] labels) {
        int width = MIN_DAY_WIDTH;
        for (String label : labels) {
            width = Math.max(width, CharWidth.of(label));
        }
        return width;
    }


    /**
     * Left-pads the text with spaces to the given display width. Text already at least that wide is
     * returned unchanged.
     *
     * @param text  the text to pad
     * @param width the target display width in columns
     * @return the left-padded text
     */
    private static String leftPad(String text, int width) {
        StringBuilder sb = new StringBuilder();
        for (int w = CharWidth.of(text); w < width; w++) {
            sb.append(' ');
        }
        return sb.append(text).toString();
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
        private Locale locale = Locale.getDefault();

        private Builder(LocalDate displayDate, DateStyler dateStyler) {
            this.displayDate = displayDate != null ? displayDate : LocalDate.now();
            this.dateStyler = dateStyler != null ? dateStyler : date -> Style.EMPTY;
        }

        /**
         * Shows the month header with the given style.
         *
         * @param style the month header style
         * @return this builder
         */
        public Builder monthHeaderStyle(Style style) {
            this.monthHeaderStyle = style;
            return this;
        }

        /**
         * Shows the weekdays header with the given style.
         *
         * @param style the weekdays header style
         * @return this builder
         */
        public Builder weekdaysHeaderStyle(Style style) {
            this.weekdaysHeaderStyle = style;
            return this;
        }

        /**
         * Shows surrounding days with the given style.
         *
         * @param style the surrounding days style
         * @return this builder
         */
        public Builder surroundingStyle(Style style) {
            this.surroundingStyle = style;
            return this;
        }

        /**
         * Sets the default style for dates.
         *
         * @param style the default date style
         * @return this builder
         */
        public Builder defaultStyle(Style style) {
            this.defaultStyle = style;
            return this;
        }

        /**
         * Wraps the calendar in a block.
         *
         * @param block the block to wrap in
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the first day of the week.
         *
         * @param dayOfWeek the first day of the week
         * @return this builder
         */
        public Builder firstDayOfWeek(DayOfWeek dayOfWeek) {
            this.firstDayOfWeek = dayOfWeek != null ? dayOfWeek : DayOfWeek.MONDAY;
            return this;
        }

        /**
         * Sets the locale.
         *
         * @param locale the locale to be used
         * @return this builder
         */
        public Builder locale(Locale locale) {
            Objects.requireNonNull(locale);
            this.locale = locale;
            return this;
        }

        /**
         * Builds the calendar.
         *
         * @return a new Monthly calendar
         */
        public Monthly build() {
            return new Monthly(this);
        }
    }
}

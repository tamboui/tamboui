/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.calendar.CalendarEventStore;
import dev.tamboui.widgets.calendar.DateStyler;
import dev.tamboui.widgets.calendar.Monthly;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A DSL wrapper for the Monthly calendar widget.
 * <p>
 * Displays a calendar grid for a month.
 * <pre>{@code
 * calendar(LocalDate.now())
 *     .showMonthHeader()
 *     .showWeekdaysHeader()
 *     .highlightToday(Color.RED)
 *     .title("Calendar")
 *     .rounded()
 * }</pre>
 */
public final class CalendarElement extends StyledElement<CalendarElement> {

    private LocalDate displayDate = LocalDate.now();
    private DateStyler dateStyler = date -> Style.EMPTY;
    private Style monthHeaderStyle;
    private Style weekdaysHeaderStyle;
    private Style surroundingStyle;
    private Style defaultStyle;
    private DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY;
    private String title;
    private BorderType borderType;
    private Color borderColor;

    public CalendarElement() {
    }

    public CalendarElement(LocalDate date) {
        this.displayDate = date != null ? date : LocalDate.now();
    }

    /**
     * Sets the display date (determines which month to show).
     */
    public CalendarElement date(LocalDate date) {
        this.displayDate = date != null ? date : LocalDate.now();
        return this;
    }

    /**
     * Sets the date styler for customizing individual dates.
     */
    public CalendarElement dateStyler(DateStyler styler) {
        this.dateStyler = styler != null ? styler : date -> Style.EMPTY;
        return this;
    }

    /**
     * Highlights today with the given color.
     */
    public CalendarElement highlightToday(Color color) {
        this.dateStyler = CalendarEventStore.today(Style.EMPTY.fg(color).bold());
        return this;
    }

    /**
     * Highlights today with the given style.
     */
    public CalendarElement highlightToday(Style style) {
        this.dateStyler = CalendarEventStore.today(style);
        return this;
    }

    /**
     * Shows the month header with the given style.
     */
    public CalendarElement showMonthHeader(Style style) {
        this.monthHeaderStyle = style;
        return this;
    }

    /**
     * Shows the month header with bold style.
     */
    public CalendarElement showMonthHeader() {
        this.monthHeaderStyle = Style.EMPTY.bold();
        return this;
    }

    /**
     * Shows the weekdays header with the given style.
     */
    public CalendarElement showWeekdaysHeader(Style style) {
        this.weekdaysHeaderStyle = style;
        return this;
    }

    /**
     * Shows the weekdays header with cyan color.
     */
    public CalendarElement showWeekdaysHeader() {
        this.weekdaysHeaderStyle = Style.EMPTY.fg(Color.CYAN);
        return this;
    }

    /**
     * Shows surrounding days (from previous/next month) with the given style.
     */
    public CalendarElement showSurrounding(Style style) {
        this.surroundingStyle = style;
        return this;
    }

    /**
     * Shows surrounding days with dim style.
     */
    public CalendarElement showSurrounding() {
        this.surroundingStyle = Style.EMPTY.dim();
        return this;
    }

    /**
     * Sets the default date style.
     */
    public CalendarElement defaultStyle(Style style) {
        this.defaultStyle = style;
        return this;
    }

    /**
     * Sets the first day of the week.
     */
    public CalendarElement firstDayOfWeek(DayOfWeek day) {
        this.firstDayOfWeek = day != null ? day : DayOfWeek.MONDAY;
        return this;
    }

    /**
     * Sets the first day of week to Sunday.
     */
    public CalendarElement sundayFirst() {
        this.firstDayOfWeek = DayOfWeek.SUNDAY;
        return this;
    }

    /**
     * Sets the title.
     */
    public CalendarElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     */
    public CalendarElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     */
    public CalendarElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    @Override
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (title != null) {
            attrs.put("title", title);
        }
        return Collections.unmodifiableMap(attrs);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        Monthly.Builder builder = Monthly.builder(displayDate, dateStyler)
            .firstDayOfWeek(firstDayOfWeek);

        if (monthHeaderStyle != null) {
            builder.monthHeaderStyle(monthHeaderStyle);
        }

        if (weekdaysHeaderStyle != null) {
            builder.weekdaysHeaderStyle(weekdaysHeaderStyle);
        }

        if (surroundingStyle != null) {
            builder.surroundingStyle(surroundingStyle);
        }

        if (defaultStyle != null) {
            builder.defaultStyle(defaultStyle);
        }

        if (title != null || borderType != null) {
            Block.Builder blockBuilder = Block.builder()
                    .borders(Borders.ALL)
                    .styleResolver(styleResolver(context));
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (borderColor != null) {
                blockBuilder.borderColor(borderColor);
            }
            builder.block(blockBuilder.build());
        }

        frame.renderWidget(builder.build(), area);
    }
}

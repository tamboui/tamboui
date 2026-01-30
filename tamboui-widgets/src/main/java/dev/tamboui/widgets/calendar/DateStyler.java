/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.calendar;

import java.time.LocalDate;

import dev.tamboui.style.Style;

/**
 * Provides styling for specific dates in a {@link Monthly} calendar.
 * <p>
 * Implement this interface to customize how individual dates are rendered in
 * the calendar widget.
 *
 * <pre>{@code
 * DateStyler styler = date -> {
 *     if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
 *         return Style.EMPTY.fg(Color.RED);
 *     }
 *     return Style.EMPTY;
 * };
 * }</pre>
 *
 * @see Monthly
 * @see CalendarEventStore
 */
@FunctionalInterface
public interface DateStyler {

    /**
     * Returns the style for the given date.
     * <p>
     * Return {@link Style#EMPTY} if no special styling is needed.
     *
     * @param date
     *            the date to style
     * @return the style to apply to the date
     */
    Style getStyle(LocalDate date);
}

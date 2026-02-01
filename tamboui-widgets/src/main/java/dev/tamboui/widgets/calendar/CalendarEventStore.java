/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.calendar;

import dev.tamboui.style.Style;
import static dev.tamboui.util.CollectionUtil.mapCopyOf;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link DateStyler} implementation backed by a {@link HashMap}.
 * <p>
 * Allows storing styles for specific dates and retrieving them during
 * calendar rendering.
 *
 * <pre>{@code
 * CalendarEventStore events = CalendarEventStore.today(Style.EMPTY.fg(Color.RED).bold())
 *     .add(LocalDate.of(2025, 12, 25), Style.EMPTY.fg(Color.GREEN))
 *     .add(LocalDate.of(2025, 1, 1), Style.EMPTY.fg(Color.YELLOW));
 *
 * Monthly calendar = Monthly.of(LocalDate.now(), events);
 * }</pre>
 *
 * @see DateStyler
 * @see Monthly
 */
public final class CalendarEventStore implements DateStyler {

    private final Map<LocalDate, Style> events;

    private CalendarEventStore(Map<LocalDate, Style> events) {
        this.events = new HashMap<>(events);
    }

    /**
     * Creates an empty event store.
     *
     * @return an empty event store
     */
    public static CalendarEventStore empty() {
        return new CalendarEventStore(mapCopyOf());
    }

    /**
     * Creates an event store with today's date styled.
     *
     * @param style the style to apply to today's date
     * @return a new event store with today styled
     */
    public static CalendarEventStore today(Style style) {
        return empty().add(LocalDate.now(), style);
    }

    /**
     * Adds a date with the specified style.
     *
     * @param date  the date to style
     * @param style the style to apply
     * @return this store for chaining
     */
    public CalendarEventStore add(LocalDate date, Style style) {
        Map<LocalDate, Style> newEvents = new HashMap<>(this.events);
        newEvents.put(date, style);
        return new CalendarEventStore(newEvents);
    }

    /**
     * Adds multiple dates with the same style.
     *
     * @param style the style to apply
     * @param dates the dates to style
     * @return this store for chaining
     */
    public CalendarEventStore addAll(Style style, LocalDate... dates) {
        Map<LocalDate, Style> newEvents = new HashMap<>(this.events);
        for (LocalDate date : dates) {
            newEvents.put(date, style);
        }
        return new CalendarEventStore(newEvents);
    }

    /**
     * Adds a range of dates with the specified style.
     *
     * @param start the start date (inclusive)
     * @param end   the end date (inclusive)
     * @param style the style to apply
     * @return this store for chaining
     */
    public CalendarEventStore addRange(LocalDate start, LocalDate end, Style style) {
        Map<LocalDate, Style> newEvents = new HashMap<>(this.events);
        LocalDate current = start;
        while (!current.isAfter(end)) {
            newEvents.put(current, style);
            current = current.plusDays(1);
        }
        return new CalendarEventStore(newEvents);
    }

    @Override
    public Style getStyle(LocalDate date) {
        return events.getOrDefault(date, Style.EMPTY);
    }

    /**
     * Returns the number of styled dates.
     *
     * @return the number of styled dates
     */
    public int size() {
        return events.size();
    }

    /**
     * Returns true if this store has no styled dates.
     *
     * @return {@code true} if this store is empty
     */
    public boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * Returns true if the given date has a style.
     *
     * @param date the date to check
     * @return {@code true} if the date has a style
     */
    public boolean contains(LocalDate date) {
        return events.containsKey(date);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.calendar;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarEventStoreTest {

    @Test
    void emptyStoreReturnsEmptyStyle() {
        CalendarEventStore store = CalendarEventStore.empty();

        assertThat(store.isEmpty()).isTrue();
        assertThat(store.size()).isEqualTo(0);
        assertThat(store.getStyle(LocalDate.now())).isEqualTo(Style.EMPTY);
    }

    @Test
    void todayCreatesStoreWithTodayStyled() {
        Style redBold = Style.EMPTY.fg(Color.RED).bold();
        CalendarEventStore store = CalendarEventStore.today(redBold);

        assertThat(store.isEmpty()).isFalse();
        assertThat(store.size()).isEqualTo(1);
        assertThat(store.contains(LocalDate.now())).isTrue();
        assertThat(store.getStyle(LocalDate.now())).isEqualTo(redBold);
    }

    @Test
    void addCreatesNewStoreWithDate() {
        CalendarEventStore store = CalendarEventStore.empty();
        LocalDate christmas = LocalDate.of(2025, 12, 25);
        Style green = Style.EMPTY.fg(Color.GREEN);

        CalendarEventStore newStore = store.add(christmas, green);

        // Original store unchanged (immutability)
        assertThat(store.isEmpty()).isTrue();

        // New store has the date
        assertThat(newStore.size()).isEqualTo(1);
        assertThat(newStore.contains(christmas)).isTrue();
        assertThat(newStore.getStyle(christmas)).isEqualTo(green);
    }

    @Test
    void addAllCreatesStoreWithMultipleDates() {
        Style yellow = Style.EMPTY.fg(Color.YELLOW);
        LocalDate date1 = LocalDate.of(2025, 1, 1);
        LocalDate date2 = LocalDate.of(2025, 7, 4);
        LocalDate date3 = LocalDate.of(2025, 12, 31);

        CalendarEventStore store = CalendarEventStore.empty().addAll(yellow, date1, date2, date3);

        assertThat(store.size()).isEqualTo(3);
        assertThat(store.getStyle(date1)).isEqualTo(yellow);
        assertThat(store.getStyle(date2)).isEqualTo(yellow);
        assertThat(store.getStyle(date3)).isEqualTo(yellow);
    }

    @Test
    void addRangeStylesConsecutiveDates() {
        Style cyan = Style.EMPTY.fg(Color.CYAN);
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 7);

        CalendarEventStore store = CalendarEventStore.empty().addRange(start, end, cyan);

        assertThat(store.size()).isEqualTo(7);
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            assertThat(store.contains(date)).isTrue();
            assertThat(store.getStyle(date)).isEqualTo(cyan);
        }
    }

    @Test
    void chainingMultipleOperations() {
        Style red = Style.EMPTY.fg(Color.RED);
        Style green = Style.EMPTY.fg(Color.GREEN);
        Style blue = Style.EMPTY.fg(Color.BLUE);

        LocalDate christmas = LocalDate.of(2025, 12, 25);
        LocalDate newYear = LocalDate.of(2025, 1, 1);
        LocalDate july4 = LocalDate.of(2025, 7, 4);

        CalendarEventStore store = CalendarEventStore.today(red).add(christmas, green)
                .add(newYear, blue).add(july4, red);

        if (LocalDate.now().equals(christmas)) {
            assertThat(store.size()).isEqualTo(3);
        } else {
            assertThat(store.size()).isEqualTo(4);
            assertThat(store.getStyle(LocalDate.now())).isEqualTo(red);
        }
        assertThat(store.getStyle(christmas)).isEqualTo(green);
        assertThat(store.getStyle(newYear)).isEqualTo(blue);
        assertThat(store.getStyle(july4)).isEqualTo(red);
    }

    @Test
    void unstyledDateReturnsEmptyStyle() {
        Style red = Style.EMPTY.fg(Color.RED);
        CalendarEventStore store = CalendarEventStore.today(red);

        LocalDate unstyledDate = LocalDate.of(2000, 1, 1);
        assertThat(store.contains(unstyledDate)).isFalse();
        assertThat(store.getStyle(unstyledDate)).isEqualTo(Style.EMPTY);
    }

    @Test
    void addOverwritesExistingStyle() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        Style red = Style.EMPTY.fg(Color.RED);
        Style blue = Style.EMPTY.fg(Color.BLUE);

        CalendarEventStore store = CalendarEventStore.empty().add(date, red).add(date, blue);

        // Size should still be 1 (same date overwritten)
        assertThat(store.size()).isEqualTo(1);
        // Should have the latest style
        assertThat(store.getStyle(date)).isEqualTo(blue);
    }

    @Test
    void rangeWithSameDateAddsOneEntry() {
        LocalDate date = LocalDate.of(2025, 3, 15);
        Style magenta = Style.EMPTY.fg(Color.MAGENTA);

        CalendarEventStore store = CalendarEventStore.empty().addRange(date, date, magenta);

        assertThat(store.size()).isEqualTo(1);
        assertThat(store.getStyle(date)).isEqualTo(magenta);
    }
}

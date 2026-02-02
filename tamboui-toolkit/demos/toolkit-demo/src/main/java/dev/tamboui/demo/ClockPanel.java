/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * A panel displaying the current time and date.
 */
final class ClockPanel extends PanelContent {

    ClockPanel() {
        super("[Clock]", 22, 4, Color.CYAN);
    }

    @Override
    Element render(boolean focused) {
        var time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        var date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d"));
        return column(
            text(time).bold().cyan(),
            text(date).dim()
        );
    }
}

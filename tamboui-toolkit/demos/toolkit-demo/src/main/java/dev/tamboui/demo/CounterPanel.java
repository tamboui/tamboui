/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.style.Color;
import dev.tamboui.tui.Keys;
import dev.tamboui.tui.event.KeyEvent;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * A panel with an incrementable/decrementable counter.
 */
final class CounterPanel extends PanelContent {
    private int counter = 0;

    CounterPanel() {
        super("[Counter]", 24, 5, Color.GREEN);
    }

    @Override
    Element render(boolean focused) {
        var display = String.format("%+d", counter);
        var valueColor = counter > 0 ? Color.GREEN : (counter < 0 ? Color.RED : Color.WHITE);
        return column(
            row(text("Value: ").dim(), text(display).bold().fg(valueColor)),
            text("[k] Inc  [j] Dec").dim()
        );
    }

    @Override
    EventResult handleKey(KeyEvent event) {
        if (Keys.isChar(event, 'k') || Keys.isChar(event, 'K')) {
            counter++;
            return EventResult.HANDLED;
        }
        if (Keys.isChar(event, 'j') || Keys.isChar(event, 'J')) {
            counter--;
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}

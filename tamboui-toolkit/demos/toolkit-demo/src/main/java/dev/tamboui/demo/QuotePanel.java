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
 * A panel displaying inspirational quotes.
 */
final class QuotePanel extends PanelContent {
    private static final String[] QUOTES = {
        "The only way to do great work is to love what you do. - Steve Jobs",
        "Code is like humor. When you have to explain it, it's bad. - Cory House",
        "First, solve the problem. Then, write the code. - John Johnson",
        "Experience is the name everyone gives to their mistakes. - Oscar Wilde",
        "Java is to JavaScript what car is to carpet. - Chris Heilmann"
    };

    private int quoteIndex = 0;

    QuotePanel() {
        super("[Quote]", 50, 4, Color.YELLOW);
    }

    @Override
    Element render(boolean focused) {
        var quote = QUOTES[quoteIndex];
        return column(
            text(quote).italic().yellow(),
            text("[←/h] Prev  [→/l] Next").dim()
        );
    }

    @Override
    EventResult handleKey(KeyEvent event) {
        if (Keys.isRight(event) || Keys.isChar(event, 'l') || Keys.isChar(event, 'L')) {
            quoteIndex = (quoteIndex + 1) % QUOTES.length;
            return EventResult.HANDLED;
        }
        if (Keys.isLeft(event) || Keys.isChar(event, 'h') || Keys.isChar(event, 'H')) {
            quoteIndex = (quoteIndex - 1 + QUOTES.length) % QUOTES.length;
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}

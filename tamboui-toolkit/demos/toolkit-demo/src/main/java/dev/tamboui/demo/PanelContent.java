/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;

/**
 * Base class for panel content implementations.
 */
abstract class PanelContent {
    private final String title;
    private final int width;
    private final int height;
    private final Color color;

    protected PanelContent(String title, int width, int height, Color color) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    String title() { return title; }
    int width() { return width; }
    int height() { return height; }
    Color color() { return color; }

    abstract Element render(boolean focused);

    void onTick(long tick) {}

    EventResult handleKey(KeyEvent event) { return EventResult.UNHANDLED; }
}

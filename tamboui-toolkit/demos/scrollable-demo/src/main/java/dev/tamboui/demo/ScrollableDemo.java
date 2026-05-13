///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.time.Duration;
import java.util.stream.IntStream;

import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.ScrollableElement;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.widgets.form.BooleanFieldState;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Scrollable demo with a large list of items.
 */
public final class ScrollableDemo extends ToolkitApp {

    private final ScrollableElement scrollable;

    private ScrollableDemo() {
        this.scrollable = scrollable(
            IntStream.range(0, 100)
                .mapToObj(i -> formField("Row " + i, new BooleanFieldState()))
                .toArray(Element[]::new)
        )
          .scrollUpIndicator(text("[scroll up to see more...]").dim())
          .scrollDownIndicator(text("[scroll down to see more...]").dim())
          .fill();
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new ScrollableDemo().run();
    }

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
            .tickRate(Duration.ofMillis(100))
            .mouseCapture(false)
            .bindings(BindingSets.vim())
            .build();
    }

    @Override
    protected Element render() {
        return panel(this.scrollable)
            .id("root")
            .focusable()
            .rounded()
            .bottomTitle("[jk or scroll] Scroll");
    }
}

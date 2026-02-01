//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//SOURCES CounterComponent.java

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.actionhandler;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.KeyTrigger;

import java.util.ArrayList;
import java.util.function.Consumer;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing the ActionHandler API for handling input actions.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Global actions via programmatic handler: quit (q), clear (c)</li>
 *   <li>Component actions via @OnAction on Component subclass: arrow keys</li>
 *   <li>Automatic registration - no manual wiring needed for components</li>
 * </ul>
 * <p>
 * Use Tab/Shift+Tab to switch focus between counters. Arrow keys modify the focused counter.
 */
public class ActionHandlerDemo {

    private static final int MAX_LOG_ENTRIES = 6;

    private ActionHandlerDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new ActionHandlerDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if terminal initialization fails
     */
    public void run() throws Exception {
        var eventLog = new ArrayList<String>();
        Consumer<String> logger = msg -> {
            eventLog.add(msg);
            while (eventLog.size() > MAX_LOG_ENTRIES) {
                eventLog.removeFirst();
            }
        };

        // Extend vim bindings with custom 'clear' action bound to 'c'
        var bindings = BindingSets.vim()
                .toBuilder()
                .bind(KeyTrigger.ch('c'), "clear")
                .build();

        // Create counter components - they use @OnAction internally
        var counterA = new CounterComponent("Counter A", Color.CYAN, logger).id("counter-a");
        var counterB = new CounterComponent("Counter B", Color.MAGENTA, logger).id("counter-b");

        // Use builder - global actions are registered programmatically to capture local state
        try (var runner = ToolkitRunner.builder()
                .bindings(bindings)
                .build()) {

            // Register global actions programmatically
            var globalHandler = new ActionHandler(bindings)
                    .on(Actions.QUIT, e -> {
                        logger.accept("Quit (global)");
                        runner.quit();
                    })
                    .on("clear", e -> {
                        eventLog.clear();
                        counterA.reset();
                        counterB.reset();
                        logger.accept("Cleared (global)");
                    });
            runner.eventRouter().addGlobalHandler(globalHandler);

            runner.run(() -> column(
                    // Title bar
                    panel(() -> row(
                            text(" ActionHandler Demo ").bold().cyan(),
                            spacer(),
                            text(" Tab: switch focus ").dim(),
                            text(" arrows: change value ").dim(),
                            text(" c: clear ").dim(),
                            text(" q: quit ").dim()
                    )).rounded().borderColor(Color.DARK_GRAY).length(3),

                    // Two counters side by side (using Component with @OnAction)
                    row(counterA, counterB).length(7),

                    // Event log
                    panel(() -> {
                        if (eventLog.isEmpty()) {
                            return text("(use Tab to focus a counter, then arrow keys)").dim();
                        }
                        var lines = eventLog.stream()
                                .map(entry -> text(entry)
                                        .fg(entry.contains("Counter A") ? Color.CYAN :
                                            entry.contains("Counter B") ? Color.MAGENTA : Color.GREEN))
                                .toArray(Element[]::new);
                        return column(lines);
                    }).title("Event Log").rounded().borderColor(Color.YELLOW).fill()
            ));
        }
    }
}

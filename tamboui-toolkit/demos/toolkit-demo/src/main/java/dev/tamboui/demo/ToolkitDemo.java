///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
// These are listed explicitly to allow running the demo without cloning the repo locally
//SOURCES FloatingPanelsArea.java ClockPanel.java CounterPanel.java ProgressPanel.java ProgressPanel.java QuotePanel.java SystemInfoPanel.java TodoPanel.java FloatingPanel.java PanelContent.java
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.style.Color;
import dev.tamboui.tui.TuiConfig;

import java.time.Duration;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Widget Playground Demo showcasing the TamboUI DSL.
 * <p>
 * Features demonstrated:
 * <ul>
 *   <li>Lambda-based DSL for dynamic content</li>
 *   <li>Stateful components (TODO list with input field)</li>
 *   <li>Event handlers on elements</li>
 *   <li>Draggable floating panels</li>
 *   <li>Focus navigation (Tab/Shift+Tab)</li>
 *   <li>Dynamic component creation</li>
 * </ul>
 */
public class ToolkitDemo {

    private ToolkitDemo() {
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new ToolkitDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        var config = TuiConfig.builder()
            .mouseCapture(true)
            .tickRate(Duration.ofMillis(100))
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            var panels = new FloatingPanelsArea();
            runner.run(() -> dock()
                    .top(panel(() -> row(
                            text(" TamboUI Widget Playground ").bold().cyan(),
                            spacer(),
                            text(" [1-6] Add Panel ").dim(),
                            text(" [Tab] Focus ").dim(),
                            text(" [Drag] Move ").dim(),
                            text(" [x] Delete ").dim(),
                            text(" [q] Quit ").dim()
                    )).rounded().borderColor(Color.DARK_GRAY))
                    .center(panels)
            );
        }
    }

}

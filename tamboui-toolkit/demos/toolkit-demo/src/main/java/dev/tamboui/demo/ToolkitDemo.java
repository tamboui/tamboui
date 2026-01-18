///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
// These are listed explicitly to allow running the demo without cloning the repo locally
//SOURCES FloatingPanelsArea.java ClockPanel.java CounterPanel.java ProgressPanel.java ProgressPanel.java QuotePanel.java SystemInfoPanel.java TodoPanel.java FloatingPanel.java PanelContent.java
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.style.Color;
import dev.tamboui.tui.TuiConfig;

import java.time.Duration;

import static dev.tamboui.toolkit.Toolkit.*;

/// Widget Playground Demo showcasing the TamboUI DSL.
///
///
///
/// Features demonstrated:
///
/// - Lambda-based DSL for dynamic content
/// - Stateful components (TODO list with input field)
/// - Event handlers on elements
/// - Draggable floating panels
/// - Focus navigation (Tab/Shift+Tab)
/// - Dynamic component creation
public class ToolkitDemo {

    public ToolkitDemo() {
    }

    public static void main(String[] args) throws Exception {
        new ToolkitDemo().run();
    }

    public void run() throws Exception {
        var config = TuiConfig.builder()
            .mouseCapture(true)
            .tickRate(Duration.ofMillis(100))
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            var panels = new FloatingPanelsArea();
            runner.run(() -> column(
                    panel(() -> row(
                            text(" TamboUI Widget Playground ").bold().cyan(),
                            spacer(),
                            text(" [1-6] Add Panel ").dim(),
                            text(" [Tab] Focus ").dim(),
                            text(" [Drag] Move ").dim(),
                            text(" [x] Delete ").dim(),
                            text(" [q] Quit ").dim()
                    )).rounded().borderColor(Color.DARK_GRAY).length(3),
                    panels
            ));
        }
    }

}


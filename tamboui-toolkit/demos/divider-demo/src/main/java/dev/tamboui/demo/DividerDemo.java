///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.time.Duration;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.tui.TuiConfig;

import static dev.tamboui.toolkit.Toolkit.*;
import static dev.tamboui.toolkit.elements.DividerStyle.*;

/**
 * Demo showcasing the Divider component.
 * <p>
 * Features demonstrated:
 * <ul>
 *   <li>All line styles (single, double, bold, dotted, dashed, heavy, rounded)</li>
 *   <li>Left, center, and right text positioning</li>
 *   <li>CSS styling for dividers and text</li>
 *   <li>Custom line and text colors</li>
 * </ul>
 */
public class DividerDemo {

    private DividerDemo() {
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new DividerDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
    public void run() throws Exception {
        var config = TuiConfig.builder()
            .mouseCapture(false)
            .tickRate(Duration.ofMillis(100))
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> column(
                // Title
                text("Divider Component Demo").bold().cyan(),

                spacer(1),

                // Plain dividers with different styles
                text("Line Styles:").bold().yellow(),
                divider().style(SINGLE).fg(Color.CYAN),
                divider().style(DOUBLE).fg(Color.BLUE),
                divider().style(BOLD).fg(Color.MAGENTA),
                divider().style(DOTTED).fg(Color.GREEN),
                divider().style(DASHED).fg(Color.YELLOW),
                divider().style(HEAVY).fg(Color.RED),
                divider().style(ROUNDED).fg(Color.WHITE),

                spacer(1),

                // Center text
                text("Center Text:").bold().yellow(),
                divider().center("Section Title").fg(Color.CYAN),
                divider().center("--- Middle ---").style(BOLD).fg(Color.GREEN),
                divider().center("[ Important ]").style(DOUBLE).fg(Color.YELLOW),

                spacer(1),

                // Left text
                text("Left Text:").bold().yellow(),
                divider().left("Chapter 1").fg(Color.CYAN),
                divider().left(">>> ").style(BOLD).fg(Color.GREEN),

                spacer(1),

                // Right text
                text("Right Text:").bold().yellow(),
                divider().right("Page 1").fg(Color.CYAN),
                divider().right(" (End)").style(BOLD).fg(Color.GREEN),

                spacer(1),

                // All three positions
                text("Left + Center + Right:").bold().yellow(),
                divider()
                    .left("Left")
                    .center("Middle")
                    .right("Right")
                    .fg(Color.CYAN),

                divider()
                    .left("Start")
                    .center("---")
                    .right("End")
                    .style(DOUBLE)
                    .fg(Color.GREEN),

                spacer(1),

                // Custom colors for line and text separately
                text("Separate Colors:").bold().yellow(),
                divider()
                    .center("Red Line, Cyan Text")
                    .lineColor(Color.RED)
                    .centerColor(Color.CYAN),

                divider()
                    .left("L").center("M").right("R")
                    .lineColor(Color.GRAY)
                    .leftColor(Color.RED)
                    .centerColor(Color.GREEN)
                    .rightColor(Color.BLUE),

                spacer(1),

                // Inline with row
                text("Inline in Row:").bold().yellow(),
                row(
                    text("Before"),
                    divider().center(" Mid ").style(BOLD).fg(Color.YELLOW),
                    text("After")
                ),

                spacer(1),

                text("Press [q] to quit").dim()
            ));
        }
    }
}

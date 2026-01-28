//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.modular;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.style.Overflow;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * A demo application that runs on the Java module path using JPMS.
 * <p>
 * This demo showcases that TamboUI works correctly when running
 * as a modular application with explicit module dependencies.
 */
public class ModularDemo {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ModularDemo() {

    }

    /**
     * Entry point for the modular demo application.
     *
     * @param args command line arguments (not used)
     * @throws Exception if an error occurs during execution
     */
    public static void main(String[] args) throws Exception {
        var config = TuiConfig.builder()
                .mouseCapture(true)
                .tickRate(Duration.ofMillis(100))
                .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> column(
                    panel(() -> row(
                            text(" TamboUI Modular Demo ").bold().cyan(),
                            spacer(),
                            text(" Running on Java Module Path ").dim(),
                            text(" [q] Quit ").dim()
                    )).rounded().borderColor(Color.DARK_GRAY).length(3),

                    row(
                            panel(() -> column(
                                    text("Module Information").bold().yellow(),
                                    text(""),
                                    text("This application runs using JPMS (Java Platform Module System).").overflow(Overflow.WRAP_WORD),
                                    text(""),
                                    text("Required modules:").cyan(),
                                    text("  • dev.tamboui.demo.modular"),
                                    text("  • dev.tamboui.toolkit"),
                                    text("  • dev.tamboui.tui"),
                                    text("  • dev.tamboui.widgets"),
                                    text("  • dev.tamboui.css"),
                                    text("  • dev.tamboui.core")
                            )).title("Modules").rounded().borderColor(Color.BLUE).fill(),

                            panel(() -> column(
                                    text("Current Time").bold().green(),
                                    text(""),
                                    text(LocalTime.now().format(TIME_FORMAT)).bold(),
                                    text(""),
                                    text("The time updates on each render,"),
                                    text("demonstrating reactive rendering.")
                            )).title("Clock").rounded().borderColor(Color.GREEN)
                    ).fill()
            ));
        }
    }
}

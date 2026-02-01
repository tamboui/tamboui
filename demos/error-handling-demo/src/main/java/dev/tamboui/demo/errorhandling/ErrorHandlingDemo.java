//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.errorhandling;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;

import java.time.Duration;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo application showcasing TamboUI's error handling features.
 * <p>
 * This demo demonstrates fault-tolerant rendering where individual
 * widgets that fail to render are replaced with error placeholders
 * while the rest of the UI continues to work normally.
 */
public class ErrorHandlingDemo {

    private static boolean faultyPanelEnabled = false;

    private ErrorHandlingDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        TuiConfig config = TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .build();

        try (ToolkitRunner runner = ToolkitRunner.builder()
                .config(config)
                .faultTolerant(true)
                .build()) {

            runner.run(() -> column(
                    // Header panel
                    panel(() -> row(
                            text(" Error Handling Demo ").bold().cyan(),
                            spacer(),
                            text(" [Fault-Tolerant Mode] ").green(),
                            text(" [q] Quit ").dim()
                    )).rounded().borderColor(Color.DARK_GRAY).length(3),

                    // Main content - two panels side by side
                    row(
                            // Left panel - Info (always works)
                            panel(() -> column(
                                    text("About This Demo").bold().yellow(),
                                    text(""),
                                    text("In fault-tolerant mode, when a"),
                                    text("widget fails to render, it is"),
                                    text("replaced with an error placeholder."),
                                    text(""),
                                    text("The rest of the UI continues to"),
                                    text("work normally."),
                                    text(""),
                                    text("Press [e] to toggle the faulty"),
                                    text("panel on the right.")
                            )).title("Info").rounded().borderColor(Color.BLUE).fill(),

                            // Right panel - may be faulty
                            faultyPanelEnabled
                                    ? faultyPanel()
                                    : panel(() -> column(
                                            text("Normal Panel").bold().green(),
                                            text(""),
                                            text("This panel renders correctly."),
                                            text(""),
                                            text("Press [e] to make this panel"),
                                            text("throw an exception during render.")
                                    )).title("Controls").rounded().borderColor(Color.GREEN).fill()
                    ).fill(),

                    // Footer
                    panel(() -> row(
                            text(" Status: ").dim(),
                            faultyPanelEnabled
                                    ? text("Faulty panel ENABLED - see error placeholder above").red()
                                    : text("Normal operation").green(),
                            spacer(),
                            text(" [e] Toggle faulty panel ").yellow()
                    )).rounded().borderColor(Color.DARK_GRAY).length(3)
            ).id("root").focusable().onKeyEvent(ErrorHandlingDemo::handleKeyEvent));
        }
    }

    private static EventResult handleKeyEvent(KeyEvent event) {
        char c = event.character();

        if (c == 'e' || c == 'E') {
            faultyPanelEnabled = !faultyPanelEnabled;
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    /**
     * Creates a panel that throws an exception during rendering.
     */
    private static Element faultyPanel() {
        return new Element() {
            @Override
            public void render(Frame frame, Rect area, RenderContext context) {
                throw new NullPointerException(
                        "Cannot render: data is null"
                );
            }

            @Override
            public String id() {
                return "faulty-panel";
            }
        };
    }
}

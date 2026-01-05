/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.parser.CssParseException;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextAreaState;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Demo showcasing live CSS editing with custom components.
 * <p>
 * Features demonstrated:
 * <ul>
 *   <li>Live CSS editing in a text area</li>
 *   <li>Real-time CSS application to ProgressCard components</li>
 *   <li>CSS parse error display</li>
 *   <li>Custom ProgressCard component with @OnAction for keyboard control</li>
 * </ul>
 */
public class CustomComponentDemo implements Element {

    private static final String DEFAULT_CSS = """
        /* Edit this CSS to style the cards! */

        ProgressCard {
            background: #1a1a1a;
            border-color: #666666;
            border-type: rounded;
        }

        ProgressCard:focus {
            border-color: cyan;
            border-type: double;
        }

        ProgressCard.complete {
            border-color: green;
        }

        ProgressCard.in-progress {
            border-color: yellow;
        }

        /* ID selector: style a specific card */
        #card-1 {
            background: #1a2a1a;
        }

        .card-title {
            color: cyan;
            text-style: bold;
        }

        .card-description {
            color: #888888;
        }

        .progress-complete {
            color: green;
        }

        .progress-in-progress {
            color: yellow;
        }
        """;

    private final StyleEngine styleEngine;
    private final List<ProgressCard> cards = new ArrayList<>();
    private final TextAreaState cssEditorState;
    private String parseError = null;
    private String lastAppliedCss = "";

    public CustomComponentDemo() {
        styleEngine = StyleEngine.create();

        // Initialize CSS editor state with default CSS
        cssEditorState = new TextAreaState(DEFAULT_CSS);

        // Apply initial CSS
        applyUserCss(DEFAULT_CSS);

        // Create sample progress cards
        cards.add(new ProgressCard()
            .id("card-1")
            .title("Setup Environment")
            .description("Install dependencies and configure tools")
            .progress(1.0));

        cards.add(new ProgressCard()
            .id("card-2")
            .title("Build Application")
            .description("Compile source code and run tests")
            .progress(0.65));

        cards.add(new ProgressCard()
            .id("card-3")
            .title("Deploy to Production")
            .description("Push to production servers")
            .progress(0.0));
    }

    public static void main(String[] args) throws Exception {
        var demo = new CustomComponentDemo();
        demo.run();
    }

    public void run() throws Exception {
        // Create bindings with increment/decrement actions for ProgressCard
        var bindings = BindingSets.standard()
                .toBuilder()
                .bind("increment", KeyTrigger.ch('+'))
                .bind("decrement", KeyTrigger.ch('-'))
                .build();

        var config = TuiConfig.builder()
            .mouseCapture(true)
            .noTick()
            .build();

        try (var runner = ToolkitRunner.builder()
                .config(config)
                .bindings(bindings)
                .build()) {

            // Register global quit handler
            var globalHandler = new ActionHandler(bindings)
                    .on(Actions.QUIT, e -> runner.quit());
            runner.eventRouter().addGlobalHandler(globalHandler);

            runner.styleEngine(styleEngine);
            runner.run(() -> this);
        }
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        // Try to apply CSS if it changed
        String currentCss = cssEditorState.text();
        if (!currentCss.equals(lastAppliedCss)) {
            applyUserCss(currentCss);
            lastAppliedCss = currentCss;
        }

        column(
            // Header
            panel(() -> row(
                text(" Live CSS Editor Demo ").bold().cyan(),
                spacer(),
                text(" [Tab] Focus ").dim(),
                text(" [+/-] Progress ").dim(),
                text(" [q] Quit ").dim()
            )).rounded().length(3),

            // Main content
            row(
                // Left side - CSS Editor
                column(
                    textArea(cssEditorState)
                        .title("CSS Editor - Edit to see live changes")
                        .showLineNumbers()
                        .rounded()
                        .id("css-editor")
                        .fill(),

                    // Error/status display
                    panel(() -> {
                        if (parseError != null) {
                            return text(parseError).red();
                        } else {
                            return text("CSS Valid").green();
                        }
                    }).title("Status").rounded().length(3)
                ).percent(50),

                // Right side - Preview
                column(
                    panel(() -> column(
                        text("Preview").bold().cyan(),
                        spacer(1),
                        text("Edit the CSS on the left to"),
                        text("see changes applied live!"),
                        spacer(1),
                        text("Properties:").yellow(),
                        text("  border-type: rounded|double|thick").dim(),
                        text("  border-color: <color>").dim(),
                        text("  background: <color>").dim()
                    )).id("info-panel").rounded().length(12),

                    // The cards
                    renderCard(0),
                    renderCard(1),
                    renderCard(2)
                ).fill()
            ).fill(),

            // Footer
            panel(() -> row(
                text("Edit CSS ").bold(),
                text("to style ").dim(),
                text("ProgressCard ").cyan(),
                text("components in real-time").dim()
            )).rounded().length(3)
        ).render(frame, area, context);
    }

    private Element renderCard(int index) {
        return cards.get(index).length(5);
    }

    private void applyUserCss(String css) {
        try {
            // Add/update the user stylesheet
            styleEngine.addStylesheet("user", css);
            styleEngine.setActiveStylesheet("user");
            parseError = null;
        } catch (CssParseException e) {
            parseError = "Line " + e.getPosition().line() + ": " + e.getMessage();
        } catch (Exception e) {
            parseError = "Error: " + e.getMessage();
        }
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Tab and +/- are handled by the framework and individual components
        return EventResult.UNHANDLED;
    }
}

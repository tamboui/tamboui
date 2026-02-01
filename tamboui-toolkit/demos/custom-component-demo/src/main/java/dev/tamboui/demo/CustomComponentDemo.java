//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//SOURCES ProgressCard.java
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.css.parser.CssParseException;
import dev.tamboui.export.BufferSvgExporter;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    private boolean showHelp = false;
    private boolean exportSvgRequested = false;
    private String lastSvgExportMessage = null;

    private static final String DEFAULT_CSS = """
        /* Edit this CSS to style the cards! */

        /* ══════════════════════════════════════════
           ProgressCard Styles
           ══════════════════════════════════════════ */

        ProgressCard {
            background: #1a1a1a;
            border-color: #666666;
            border-type: rounded;
            height: 5;
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

        .progress-complete GaugeElement-filled {
            color: green;
        }

        .progress-in-progress GaugeElement-filled {
            color: yellow;
        }

        /* ══════════════════════════════════════════
           Panel Styles
           ══════════════════════════════════════════ */

        .header-panel {
            border-type: rounded;
            height: 3;
        }

        .status-panel {
            border-type: rounded;
            height: 3;
        }

        #info-panel {
            border-type: rounded;
            spacing: 0;
            height: 3;
        }

        .footer-panel {
            border-type: rounded;
            height: 3;
        }

        #css-editor {
            border-type: rounded;
            height: fill;
        }

        .editor-column {
            width: 50%;
        }

        .preview-column {
            width: fill;
        }

        .main-row {
            height: fill;
        }

        /* ══════════════════════════════════════════
           Text Styles
           ══════════════════════════════════════════ */

        .title {
            color: cyan;
            text-style: bold;
        }

        .dim {
            text-style: dim;
        }

        .highlight {
            color: yellow;
        }

        .success {
            color: green;
        }

        .error {
            color: red;
        }

        /* ══════════════════════════════════════════
           Flex Layout Properties
           ══════════════════════════════════════════ */

        .cards-container {
            spacing: 1;
            flex: start;
            height: fill;
        }

        .header-row {
            flex: space-between;
        }

        .footer-row {
            flex: center;
        }

        /* Footer text uses 'fit' to size to content, enabling flex */
        .footer-row .title {
            width: fit;
        }

        .footer-row .dim {
            width: fit;
        }
        """;

    private final StyleEngine styleEngine;
    private final List<ProgressCard> cards = new ArrayList<>();
    private final TextAreaState cssEditorState;
    private String parseError = null;
    private String lastAppliedCss = "";

    private CustomComponentDemo() {
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

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        var demo = new CustomComponentDemo();
        demo.run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        // Create bindings with increment/decrement actions for ProgressCard
        var bindings = BindingSets.standard()
                .toBuilder()
                .bind(KeyTrigger.ch('+'), "increment")
                .bind(KeyTrigger.ch('-'), "decrement")
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
            // Header - styling via CSS (.header-panel, .header-row, .title, .dim)
            panel(() -> row(
                text(" Live CSS Editor Demo ").addClass("title"),
                spacer(),
                text(" [Tab] Focus ").addClass("dim"),
                text(" [+/-] Progress ").addClass("dim"),
                text(" [h] Help ").addClass("dim"),
                text(" [s] Save SVG ").addClass("dim"),
                text(" [q] Quit ").addClass("dim")
            ).addClass("header-row")).addClass("header-panel"),

            // Main content
            row(
                // Left side - CSS Editor
                column(
                    textArea(cssEditorState)
                        .title("CSS Editor - Edit to see live changes")
                        .showLineNumbers()
                        .id("css-editor"),

                    // Error/status display - styling via CSS (.status-panel, .error, .success)
                    panel(() -> {
                        if (parseError != null) {
                            return text(parseError).addClass("error");
                        } else {
                            return text("CSS Valid").addClass("success");
                        }
                    }).title("Status").addClass("status-panel")
                ).addClass("editor-column"),

                // Right side - Preview with cards container using CSS flex
                column(
                    // Compact info panel with hint
                    panel(() -> row(
                        text("Edit CSS to style the cards").addClass("dim"),
                        spacer(),
                        text("[h] Help").addClass("highlight")
                    )).title("Preview").id("info-panel"),

                    // The cards in a container - styling via CSS (.cards-container)
                    column(
                        renderCard(0),
                        renderCard(1),
                        renderCard(2)
                    ).addClass("cards-container")
                ).addClass("preview-column")
            ).addClass("main-row"),

            // Footer - styling via CSS (.footer-panel, .footer-row)
            // Text widths set via CSS width: fit, flex controls positioning
            panel(() -> row(
                text("Edit CSS ").addClass("title"),
                text("to style ").addClass("dim"),
                text("ProgressCard ").addClass("title"),
                text("in real-time").addClass("dim"),
                spacer(),
                lastSvgExportMessage != null ? text(lastSvgExportMessage).addClass("dim") : spacer(0)
            ).addClass("footer-row")).addClass("footer-panel")
        ).render(frame, area, context);

        // Export SVG if requested (F2 was pressed)
        if (exportSvgRequested) {
            exportSvgRequested = false;
            exportSvgSnapshot(frame.buffer());
        }

        // Show help dialog if requested
        if (showHelp) {
            dialog("CSS Properties Help",
                text("Style Properties:").addClass("highlight"),
                text("  border-type: plain|rounded|double|thick"),
                text("  border-color: <color>"),
                text("  background: <color>"),
                text("  color: <color>"),
                text("  text-style: bold|dim|italic|underline"),
                spacer(1),
                text("Layout Properties:").addClass("highlight"),
                text("  height: fill | <number> | <percent>%"),
                text("  width: fill | <number> | <percent>%"),
                text("  flex: start|center|end|space-between"),
                text("  spacing: <number>"),
                spacer(1),
                text("[Esc] Close").addClass("dim")
            ).width(50).height(17).onCancel(() -> showHelp = false)
             .render(frame, area, context);
        }
    }

    private Element renderCard(int index) {
        return cards.get(index);
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
        // Handle dialog events when shown
        if (showHelp) {
            if (event.isCancel()) {
                showHelp = false;
                return EventResult.HANDLED;
            }
            // Dialog is modal - consume all events
            return EventResult.HANDLED;
        }

        if (event.isChar('s')) {
            // Request export - will happen in next render cycle where we have access to frame
            exportSvgRequested = true;
            return EventResult.HANDLED;
        }

        // Handle 'h' to show help dialog
        if (event.isChar('h')) {
            showHelp = true;
            return EventResult.HANDLED;
        }
        // Tab and +/- are handled by the framework and individual components
        return EventResult.UNHANDLED;
    }

    private void exportSvgSnapshot(Buffer buffer) {
        try {
            // Copy the buffer to avoid mutation
            Buffer snapshot = buffer.copy();

            Path outDir = Paths.get("build", "svg");
            Files.createDirectories(outDir);

            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
            Path outFile = outDir.resolve("snapshot-" + timestamp + ".svg");

            String svg = BufferSvgExporter.exportSvg(
                snapshot,
                new BufferSvgExporter.Options()
                    .title("TamboUI")
                    .uniqueId("snapshot-" + timestamp)
            );

            Files.write(outFile, svg.getBytes(StandardCharsets.UTF_8));
            lastSvgExportMessage = "Saved " + outFile.toString();
        } catch (Exception e) {
            lastSvgExportMessage = "SVG export failed: " + e.getMessage();
        }
    }
}

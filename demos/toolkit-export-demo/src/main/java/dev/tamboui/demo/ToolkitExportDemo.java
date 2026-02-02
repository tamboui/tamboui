//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.buffer.Buffer;
import static dev.tamboui.export.ExportRequest.export;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.toolkit.app.ToolkitPostRenderProcessor;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.terminal.Frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Toolkit export demo: uses Elements and {@link Element#renderedArea()} for export regions.
 * Press <strong>E</strong> to export the current screen (full, cropped by panel, and combined rect).
 * <p>
 * Output directory: current working directory, or path given as first argument.
 */
public final class ToolkitExportDemo {

    private final Path outDir;
    private final AtomicReference<Frame> lastFrame = new AtomicReference<>();
    private ToolkitRunner runner;

    private final List<Path> lastExported = new ArrayList<>();
    private boolean showExportsPopup;
    private Element titlePanel;
    private Element sidebarPanel;
    private Element contentPanel;
    private Element footerPanel;

    private ToolkitExportDemo(Path outDir) {
        this.outDir = outDir;
    }

    /**
     * Entry point. Optional first argument: output directory for exported files.
     *
     * @param args optional output path; default is a temp directory
     * @throws Exception if export or I/O fails
     */
    public static void main(String[] args) throws Exception {
        Path outDir = args.length > 0 ? Paths.get(args[0]) : Files.createTempDirectory("tamboui-export-");
        ToolkitExportDemo app = new ToolkitExportDemo(outDir);

        var config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(100))
            .build();

        try (var r = ToolkitRunner.builder()
            .config(config)
            .app(app)
            .postRenderProcessor(app::captureFrame)
            .build()) {
            app.runner = r;
            r.run(app::buildRoot);
        }
    }

    private void captureFrame(Frame frame,
                               ElementRegistry elementRegistry,
                               StyledAreaRegistry styledAreaRegistry,
                               FocusManager focusManager,
                               Duration elapsed) {
        lastFrame.set(frame);
    }

    private Element buildExportsDialog() {
        // Markup: header + one line per file (link tag must close with [/link]), then close hint
        StringBuilder markup = new StringBuilder("");
        for (Path p : lastExported) {
            String name = p.getFileName().toString();
            String url = p.toUri().toString();
            markup.append("  [cyan][link=").append(url).append("]").append(name).append("[/link][/cyan]\n");
        }
        markup.append("[dim]Press Enter or Escape to close[/dim]");

        Element content = markupText(markup.toString());
        return dialog("Exported Files (click to open)", content)
            .rounded()
            .borderColor(Color.GREEN)
            .width(50)
            .padding(2)
            .onConfirm(() -> showExportsPopup = false)
            .onCancel(() -> showExportsPopup = false);
    }

    private Element buildRoot() {
        titlePanel = panel(row(
            text(" TamboUI ").bold().fg(Color.CYAN),
            text(" Export Demo (Toolkit) "),
            text(" [E] Export · [q] Quit ").dim()
        )).rounded().borderColor(Color.CYAN).length(3);

        sidebarPanel = panel(column(
            text(" Export ").bold().fg(Color.BLUE),
            text(""),
            text("Press [E] to export"),
            text(""),
            text("Regions come from"),
            text("element.renderedArea()"),
            text("after each render."),
            text(""),
            text("Combined rect:"),
            text("r1.union(r2)")
        )).rounded().borderColor(Color.BLUE).length(22);

        contentPanel = panel(column(
            text(" Star Wars Movies ").bold().fg(Color.GREEN),
            text(""),
            text(" Dec 20, 2019  The Rise of Skywalker   $952M "),
            text(" May 25, 2018  Solo                     $393M "),
            text(" Dec 15, 2017  The Last Jedi           $1.3B "),
            text(" Dec 16, 2016  Rogue One               $1.3B ")
        )).rounded().borderColor(Color.GREEN).fill();

        footerPanel = panel(row(
            text(" Uses Element.renderedArea() for crop regions ").dim(),
            text(" ToolkitExportDemo ").fg(Color.CYAN)
        )).rounded().borderColor(Color.DARK_GRAY).length(2);

        Element mainColumn = column(
            titlePanel,
            row(sidebarPanel, contentPanel),
            footerPanel
        ).focusable().onKeyEvent(event -> {
            if (event.isQuit() || event.isChar('q') || event.isChar('Q')) {
                runner.quit();
                return EventResult.HANDLED;
            }
            if (event.isChar('e') || event.isChar('E')) {
                runner.runOnRenderThread(this::doExport);
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        });

        if (showExportsPopup) {
            return stack(mainColumn, buildExportsDialog());
        }
        return mainColumn;
    }

    private void doExport() {
        Frame frame = lastFrame.get();
        if (frame == null) {
            return;
        }
        Buffer buffer = frame.buffer();
        try {
            if (!Files.isDirectory(outDir)) {
                Files.createDirectories(outDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output dir: " + outDir, e);
        }

        try {
            List<Path> exported = new ArrayList<>();

            Path svg = outDir.resolve("export_demo.svg");
            Path html = outDir.resolve("export_demo.html");
            Path txt = outDir.resolve("export_demo.txt");
            export(buffer).svg().options(o -> o.title("TamboUI Export (Toolkit)")).toFile(svg);
            export(buffer).html().toFile(html);
            export(buffer).text().toFile(txt);
            exported.add(svg);
            exported.add(html);
            exported.add(txt);

            exportPanel(buffer, titlePanel, "title", exported);
            exportPanel(buffer, sidebarPanel, "sidebar", exported);
            exportPanel(buffer, contentPanel, "content", exported);
            exportPanel(buffer, footerPanel, "footer", exported);

            Rect contentRect = contentPanel.renderedArea();
            Rect footerRect = footerPanel.renderedArea();
            if (contentRect != null && footerRect != null && !contentRect.isEmpty() && !footerRect.isEmpty()) {
                Rect combined = contentRect.union(footerRect);
                Path combinedSvg = outDir.resolve("export_demo_content_and_footer.svg");
                export(buffer).crop(combined).svg()
                    .options(o -> o.title("Content + Footer"))
                    .toFile(combinedSvg);
                exported.add(combinedSvg);
            }

            lastExported.clear();
            lastExported.addAll(exported);
            showExportsPopup = true;
        } catch (IOException e) {
            throw new RuntimeException("Export failed", e);
        }
    }

    private void exportPanel(Buffer buffer, Element panel, String name, List<Path> exported) throws IOException {
        Rect area = panel.renderedArea();
        if (area != null && !area.isEmpty()) {
            Path path = outDir.resolve("export_demo_" + name + ".svg");
            export(buffer).crop(area).svg()
                .options(o -> o.title(name.substring(0, 1).toUpperCase() + name.substring(1)))
                .toFile(path);
            exported.add(path);
        }
    }
}

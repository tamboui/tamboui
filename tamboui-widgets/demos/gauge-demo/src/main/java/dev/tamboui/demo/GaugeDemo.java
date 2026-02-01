///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.gauge.Gauge;
import dev.tamboui.widgets.gauge.LineGauge;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;
import java.util.List;

/**
 * Demo TUI application showcasing Gauge and LineGauge widgets.
 */
public class GaugeDemo {

    private boolean running = true;
    private int progress = 0;
    private boolean autoProgress = true;

    private GaugeDemo() {

    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new GaugeDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.onResize(() -> {
                terminal.draw(this::ui);
            });

            // Initial draw
            terminal.draw(this::ui);

            // Event loop
            while (running) {
                int c = backend.read(50);
                if (c == -2) {
                    // Timeout - update progress if auto mode
                    if (autoProgress) {
                        progress = (progress + 1) % 101;
                    }
                    terminal.draw(this::ui);
                    continue;
                }
                if (c == -1) {
                    continue;
                }

                boolean needsRedraw = handleInput(c);
                if (needsRedraw) {
                    terminal.draw(this::ui);
                }
            }
        }
    }

    private boolean handleInput(int c) {
        return switch (c) {
            case 'q', 'Q', 3 -> { // q/Q/Ctrl+C to quit
                running = false;
                yield true;
            }
            case ' ' -> { // Space to toggle auto progress
                autoProgress = !autoProgress;
                yield true;
            }
            case '+', '=' -> { // + to increase progress
                if (progress < 100) {
                    progress++;
                }
                yield true;
            }
            case '-', '_' -> { // - to decrease progress
                if (progress > 0) {
                    progress--;
                }
                yield true;
            }
            case 'r', 'R' -> { // r to reset
                progress = 0;
                yield true;
            }
            default -> false;
        };
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        // Split into header, main content, and footer
        List<Rect> mainLayout = Layout.vertical()
            .constraints(
                Constraint.length(3),    // Header
                Constraint.fill(),       // Main content
                Constraint.length(3)     // Footer
            )
            .split(area);

        renderHeader(frame, mainLayout.get(0));
        renderMain(frame, mainLayout.get(1));
        renderFooter(frame, mainLayout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Gauge Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMain(Frame frame, Rect area) {
        // Split main area into sections for different gauge styles
        List<Rect> sections = Layout.vertical()
            .constraints(
                Constraint.length(5),  // Basic gauge
                Constraint.length(5),  // Styled gauge
                Constraint.length(5),  // Gauge with block
                Constraint.length(3),  // Line gauges
                Constraint.fill()      // Info section
            )
            .margin(1)
            .split(area);

        renderBasicGauge(frame, sections.get(0));
        renderStyledGauge(frame, sections.get(1));
        renderGaugeWithBlock(frame, sections.get(2));
        renderLineGauges(frame, sections.get(3));
        renderInfo(frame, sections.get(4));
    }

    private void renderBasicGauge(Frame frame, Rect area) {
        Gauge gauge = Gauge.builder()
            .percent(progress)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .title("Basic Gauge")
                .build())
            .build();

        frame.renderWidget(gauge, area);
    }

    private void renderStyledGauge(Frame frame, Rect area) {
        // Color based on progress
        Color gaugeColor;
        if (progress < 30) {
            gaugeColor = Color.RED;
        } else if (progress < 70) {
            gaugeColor = Color.YELLOW;
        } else {
            gaugeColor = Color.GREEN;
        }

        Gauge gauge = Gauge.builder()
            .percent(progress)
            .label(String.format("Progress: %d%%", progress))
            .gaugeStyle(Style.EMPTY.fg(gaugeColor).bg(Color.DARK_GRAY))
            .style(Style.EMPTY.bg(Color.BLACK))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(gaugeColor))
                .title(Title.from(
                    Line.from(Span.raw("Styled Gauge ").fg(gaugeColor))
                ))
                .build())
            .build();

        frame.renderWidget(gauge, area);
    }

    private void renderGaugeWithBlock(Frame frame, Rect area) {
        // Multiple gauges in horizontal layout
        List<Rect> gaugeAreas = Layout.horizontal()
            .constraints(
                Constraint.percentage(33),
                Constraint.percentage(33),
                Constraint.percentage(34)
            )
            .split(area);

        // Download progress
        int downloadProgress = (progress * 2) % 101;
        Gauge download = Gauge.builder()
            .percent(downloadProgress)
            .label("Download")
            .gaugeStyle(Style.EMPTY.fg(Color.BLUE))
            .block(Block.builder()
                .borders(Borders.ALL)
                .title("Downloads")
                .build())
            .build();
        frame.renderWidget(download, gaugeAreas.get(0));

        // Upload progress
        int uploadProgress = (progress * 3) % 101;
        Gauge upload = Gauge.builder()
            .percent(uploadProgress)
            .label("Upload")
            .gaugeStyle(Style.EMPTY.fg(Color.MAGENTA))
            .block(Block.builder()
                .borders(Borders.ALL)
                .title("Uploads")
                .build())
            .build();
        frame.renderWidget(upload, gaugeAreas.get(1));

        // CPU usage (inverse)
        int cpuProgress = 100 - progress;
        Gauge cpu = Gauge.builder()
            .percent(cpuProgress)
            .label("CPU: " + cpuProgress + "%")
            .gaugeStyle(Style.EMPTY.fg(cpuProgress > 80 ? Color.RED : Color.GREEN))
            .block(Block.builder()
                .borders(Borders.ALL)
                .title("System")
                .build())
            .build();
        frame.renderWidget(cpu, gaugeAreas.get(2));
    }

    private void renderLineGauges(Frame frame, Rect area) {
        List<Rect> lineAreas = Layout.vertical()
            .constraints(
                Constraint.length(1),
                Constraint.length(1),
                Constraint.length(1)
            )
            .split(area);

        // Normal line gauge
        LineGauge normal = LineGauge.builder()
            .percent(progress)
            .label("Normal:  ")
            .filledStyle(Style.EMPTY.fg(Color.GREEN))
            .unfilledStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .lineSet(LineGauge.NORMAL)
            .build();
        frame.renderWidget(normal, lineAreas.get(0));

        // Thick line gauge
        LineGauge thick = LineGauge.builder()
            .percent(progress)
            .label("Thick:   ")
            .filledStyle(Style.EMPTY.fg(Color.CYAN))
            .unfilledStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .lineSet(LineGauge.THICK)
            .build();
        frame.renderWidget(thick, lineAreas.get(1));

        // Double line gauge
        LineGauge doubleGauge = LineGauge.builder()
            .percent(progress)
            .label("Double:  ")
            .filledStyle(Style.EMPTY.fg(Color.YELLOW))
            .unfilledStyle(Style.EMPTY.fg(Color.DARK_GRAY))
            .lineSet(LineGauge.DOUBLE)
            .build();
        frame.renderWidget(doubleGauge, lineAreas.get(2));
    }

    private void renderInfo(Frame frame, Rect area) {
        String modeText = autoProgress ? "Auto (running)" : "Manual (paused)";
        Color modeColor = autoProgress ? Color.GREEN : Color.YELLOW;

        Paragraph info = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw("Progress: ").bold(),
                Span.raw(progress + "%").cyan(),
                Span.raw("  |  Mode: "),
                Span.raw(modeText).fg(modeColor)
            )))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title("Status")
                .build())
            .build();

        frame.renderWidget(info, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" Space").bold().yellow(),
            Span.raw(" Toggle auto  ").dim(),
            Span.raw("+/-").bold().yellow(),
            Span.raw(" Adjust  ").dim(),
            Span.raw("r").bold().yellow(),
            Span.raw(" Reset  ").dim(),
            Span.raw("q").bold().yellow(),
            Span.raw(" Quit").dim()
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(helpLine))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }
}
